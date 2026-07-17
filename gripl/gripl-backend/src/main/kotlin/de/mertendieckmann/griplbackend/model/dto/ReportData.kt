package de.mertendieckmann.griplbackend.model.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.mertendieckmann.griplbackend.model.evaluation.ElementTypeCounts
import de.mertendieckmann.griplbackend.model.evaluation.RagMetrics
import java.sql.Timestamp

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(EvaluationMetadataReport::class, name = "metadata"),
    JsonSubTypes.Type(TestCaseReport::class, name = "testCase"),
    JsonSubTypes.Type(EvaluationReportSummary::class, name = "summary"),
    JsonSubTypes.Type(EvaluationReportStepInfo::class, name = "stepInfo"),
    JsonSubTypes.Type(EvaluationReportError::class, name = "error")
)
sealed class EvaluationReport{
    val markdown: String by lazy { toMarkdown() }
    abstract fun toMarkdown(): String
}

data class EvaluationMetadataReport(
    val modelLabels: List<String>,
    val modelTemperatures: List<Double?>,
    val modelTopPs: List<Double?>,
    val datasets: List<DatasetInfo>,
    val timestamp: Timestamp = Timestamp(System.currentTimeMillis()),
    val totalTestCases: Int,
    val defaultEvaluationEndpoint: String,
    val seed: Int? = null,
    val totalRepetitions: Int = 1,
    val activitiesOnly: Boolean = false
): EvaluationReport() {
    override fun toMarkdown(): String {
        return """
            |## Evaluation Metadata
            |- **Models:** ${modelLabels.joinToString(", ")}
            |- **Temperatures:** ${modelTemperatures.joinToString(", ") { it?.toString() ?: "default" }}
            |- **Top Ps:** ${modelTopPs.joinToString(", ") { it?.toString() ?: "default" }}
            |- **Datasets:** ${datasets.joinToString(", ")}
            |- **Total Test Cases:** $totalTestCases
            |- **Evaluation Scope:** ${if (activitiesOnly) "Activities only" else "All BPMN elements"}
            |${if (totalRepetitions > 1) "- **Repetitions:** $totalRepetitions" else ""}
            |${if (seed != null) "- **Seed:** $seed" else ""}
            |- **Timestamp:** $timestamp
            |- **Default Evaluation Endpoint:** $defaultEvaluationEndpoint
        """.trimMargin()
    }

    data class DatasetInfo(
        val id: Long,
        val name: String
    )
}

data class TestCaseReport(
    val testCaseId: Long,
    val testCaseName: String? = null,
    val datasetId: Long? = null,
    val imageSrc: String,
    val correctActivityIds: List<String>,
    val falsePositiveIds: List<String>,
    val falseNegativeIds: List<String>,
    val expectedNamesWithIds: List<String>,
    val actualNamesWithIds: List<String>,
    val isSuccessful: Boolean,
    val result: List<ExpectedValue>,
    val amountOfRetries: Int? = null,
    val ragMetrics: RagMetrics? = null,
    val ragPromptContext: List<String>? = null,
    /** Confusion counts of this test case broken down by element category. */
    val perElementType: Map<String, ElementTypeCounts> = emptyMap()
): EvaluationReport() {

    override fun toMarkdown(): String {
        val ragBlock = ragMetrics?.let {
            """
            |
            |### RAG Metrics (Ragas)
            |- **Faithfulness:** ${it.faithfulness?.let { v -> "%.3f".format(v) } ?: "n/a"}
            |- **Context Utilization:** ${it.contextUtilization?.let { v -> "%.3f".format(v) } ?: "n/a"}
            |- **Samples:** ${it.sampleCount} (failed: ${it.failedCount})
            """.trimMargin()
        } ?: ""

        return """
            |## Test Case $testCaseId${if (testCaseName != null) " - $testCaseName" else ""}
            |<img src="$imageSrc" alt="Test Case BPMN XML" />
            |
            |- Amount of Correct Activities: ${correctActivityIds.size}
            |- Amount of False Positives: ${falsePositiveIds.size}
            |- Amount of False Negatives: ${falseNegativeIds.size}
            |
            |- **Expected:** ${expectedNamesWithIds.joinToString(", ")}
            |- **Actual:** ${actualNamesWithIds.joinToString(", ")}
            |- **Result:** ${if (isSuccessful) "✅ Passed" else "❌ Failed"}
            |
            |- **False Positives:** ${if (falsePositiveIds.isEmpty()) "None" else falsePositiveIds.joinToString(", ") { id -> actualNamesWithIds.find { it.contains(id) } ?: id }}
            |- **False Negatives:** ${if (falseNegativeIds.isEmpty()) "None" else falseNegativeIds.joinToString(", ") { id -> expectedNamesWithIds.find { it.contains(id) } ?: id }}
            |
            |- **Amount of Retries:** ${amountOfRetries ?: "N/A"}
            |$ragBlock
            |
            |<details>
            |<summary><h3>Reasoning of the LLM</h3></summary>
            |
            |${result.joinToString("\n") { res -> "- **${actualNamesWithIds.find { it.contains(res.value) } ?: res.value}**: ${res.reason ?: "No reason provided"}" }}
            |
            |</details>
        """.trimMargin()
    }
}

data class RagSummaryMetrics(
    val faithfulnessMean: Double? = null,
    val contextUtilizationMean: Double? = null,
    val evaluatedTestCases: Int = 0,
    val totalSamples: Int = 0,
    val failedSamples: Int = 0
)

/** Aggregated confusion counts and derived metrics for one element category. */
data class ElementTypeSummary(
    val displayName: String,
    val truePositives: Int,
    val falsePositives: Int,
    val falseNegatives: Int,
    val trueNegatives: Int,
    val precision: Double,
    val recall: Double,
    val f1Score: Double,
    val accuracy: Double
)

data class EvaluationReportSummary(
    val total: Int,
    val passed: Int,
    val failed: Int,
    val error: Int,
    val amountOfRetries: Int? = null,
    val precision: Double,
    val recall: Double,
    val f1Score: Double,
    val accuracy: Double,
    val totalTruePositives: Int,
    val totalFalsePositives: Int,
    val totalFalseNegatives: Int,
    val totalTrueNegatives: Int,
    val ragMetrics: RagSummaryMetrics? = null,
    /** Metrics broken down by element category, keyed by category key (activity, event, ...). */
    val perElementType: Map<String, ElementTypeSummary> = emptyMap()
): EvaluationReport() {

    override fun toMarkdown(): String {
        val perTypeBlock = if (perElementType.isEmpty()) "" else buildString {
            appendLine()
            appendLine("### Metrics by Element Type")
            appendLine("| Type | TP | FP | FN | TN | Precision | Recall | F1 |")
            appendLine("|------|----|----|----|----|-----------|--------|----|")
            perElementType.values.forEach { s ->
                appendLine("| ${s.displayName} | ${s.truePositives} | ${s.falsePositives} | ${s.falseNegatives} | ${s.trueNegatives} | ${"%.3f".format(s.precision)} | ${"%.3f".format(s.recall)} | ${"%.3f".format(s.f1Score)} |")
            }
        }

        val ragBlock = ragMetrics?.let {
            """
            |
            |### RAG Metrics (Ragas) — averaged across $evaluatedTestCasesText
            |Faithfulness: ${it.faithfulnessMean?.let { v -> "%.3f".format(v) } ?: "n/a"}
            |Context Utilization: ${it.contextUtilizationMean?.let { v -> "%.3f".format(v) } ?: "n/a"}
            |Total Samples Scored: ${it.totalSamples} (failed: ${it.failedSamples})
            """.trimMargin()
        } ?: ""

        return """
            |## Summary
            |Total: $total
            |Passed: $passed
            |Failed: $failed
            |${if (error > 0) "Error: $error" else ""}
            |${if (amountOfRetries != null) "Total Retries: $amountOfRetries" else ""}
            |
            |### Metrics
            |Accuracy: ${"%.3f".format(accuracy)}
            |Precision: ${"%.3f".format(precision)}
            |Recall: ${"%.3f".format(recall)}
            |F1-Score: ${"%.3f".format(f1Score)}
            |
            |### Confusion Matrix
            |True Positives: $totalTruePositives
            |False Positives: $totalFalsePositives
            |False Negatives: $totalFalseNegatives
            |True Negatives: $totalTrueNegatives
            |$perTypeBlock
            |$ragBlock
        """.trimMargin()
    }

    private val evaluatedTestCasesText: String
        get() = ragMetrics?.evaluatedTestCases?.let { "$it test case(s)" } ?: "0 test cases"
}

data class EvaluationReportStepInfo(
    val currentTestCaseName: String,
    val currentTestCaseId: Long,
    val currentTestCaseNumber: Int,
    val totalTestCases: Int,
): EvaluationReport() {
    override fun toMarkdown(): String {
        return """
            |## Step Info
            |Current Test Case: $currentTestCaseName
            |Test Case $currentTestCaseNumber of $totalTestCases
        """.trimMargin()
    }
}

data class EvaluationReportError(
    val testCaseId: Long,
    val datasetId: Long?,
    val testCaseName: String? = null,
    val errorMessage: String
): EvaluationReport() {

    override fun toMarkdown(): String {
        return """
            |## Error in Test Case ${if (testCaseName != null) "$testCaseName" else ""} ($testCaseId)
            |**Error Message:** $errorMessage
        """.trimMargin()
    }
}