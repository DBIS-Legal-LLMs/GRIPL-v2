package de.mertendieckmann.griplbackend.evaluation.metrics

import de.mertendieckmann.griplbackend.evaluation.ElementCategory
import de.mertendieckmann.griplbackend.model.dto.ElementTypeSummary
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportSummary
import de.mertendieckmann.griplbackend.model.dto.RagSummaryMetrics
import de.mertendieckmann.griplbackend.model.evaluation.ElementTypeCounts
import de.mertendieckmann.griplbackend.model.evaluation.EvaluationMetrics
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import de.mertendieckmann.griplbackend.model.analysis.GdprProcessingClass
import de.mertendieckmann.griplbackend.model.evaluation.ClassMetrics
import de.mertendieckmann.griplbackend.model.dto.PerClassEvaluationMetrics

/**
 * Thread-safe metrics accumulator for all test cases.
 */
class MetricsAccumulator {
    private val lock = Mutex()
    private var total: Int = 0
    private var passed: Int = 0
    private var errors: Int = 0
    private var amountOfRetries: Int = 0
    private var totalTruePositives: Int = 0
    private var totalFalsePositives: Int = 0
    private var totalFalseNegatives: Int = 0
    private var totalTrueNegatives: Int = 0

    private val perElementTypeTotals =
    mutableMapOf<String, ElementTypeCounts>()

    // RAG metric sums + counts (per test case mean values)
    private var faithfulnessSum: Double = 0.0
    private var faithfulnessCount: Int = 0
    private var contextUtilizationSum: Double = 0.0
    private var contextUtilizationCount: Int = 0
    private var ragSampleTotal: Int = 0
    private var ragFailedTotal: Int = 0
    private var ragEvaluatedTestCases: Int = 0

    private val perClassTotals =
        GdprProcessingClass.entries
            .associateWith { ClassMetrics() }
            .toMutableMap()
            
    suspend fun add(metrics: EvaluationMetrics) = lock.withLock {
        total++
        if (metrics.isSuccessful) passed++
        totalTruePositives += metrics.truePositives
        totalFalsePositives += metrics.falsePositives
        totalFalseNegatives += metrics.falseNegatives
        totalTrueNegatives += metrics.trueNegatives
        amountOfRetries += metrics.amountOfRetries ?: 0
        metrics.perElementType.forEach { (category, counts) ->
            perElementTypeTotals[category] = (perElementTypeTotals[category] ?: ElementTypeCounts()) + counts
        }

        metrics.ragMetrics?.let { rag ->
            ragEvaluatedTestCases++
            ragSampleTotal += rag.sampleCount
            ragFailedTotal += rag.failedCount
            rag.faithfulness?.let { faithfulnessSum += it; faithfulnessCount++ }
            rag.contextUtilization?.let { contextUtilizationSum += it; contextUtilizationCount++ }
        }
        metrics.perClassMetrics.forEach { (gdprClass, classMetrics) ->
            val current = perClassTotals.getValue(gdprClass)

            perClassTotals[gdprClass] = ClassMetrics(
                truePositives = current.truePositives + classMetrics.truePositives,
                falsePositives = current.falsePositives + classMetrics.falsePositives,
                falseNegatives = current.falseNegatives + classMetrics.falseNegatives,
                trueNegatives = current.trueNegatives + classMetrics.trueNegatives
            )
        }
    }

    suspend fun addError() = lock.withLock {
        total++
        errors++
    }

    private data class DerivedMetrics(
        val precision: Double,
        val recall: Double,
        val f1Score: Double,
        val accuracy: Double
    )

    private fun deriveMetrics(tp: Int, fp: Int, fn: Int, tn: Int): DerivedMetrics {
        val precision = if (tp + fp > 0) tp.toDouble() / (tp + fp) else 0.0
        val recall = if (tp + fn > 0) tp.toDouble() / (tp + fn) else 0.0
        val f1Score = if (precision + recall > 0) 2 * (precision * recall) / (precision + recall) else 0.0
        val totalCount = tp + fp + fn + tn
        val accuracy = if (totalCount > 0) (tp + tn).toDouble() / totalCount else 0.0
        return DerivedMetrics(precision, recall, f1Score, accuracy)
    }

    fun toSummary(): EvaluationReportSummary {
        val overall = deriveMetrics(
            totalTruePositives, totalFalsePositives, totalFalseNegatives, totalTrueNegatives
        )

        // Per-category metrics in stable display order (activities, events, gateways, data, other).
        // The "other" bucket is dropped when empty — it only carries unresolvable ids.
        val perElementTypeSummary = perElementTypeTotals.entries
            .sortedBy { (key, _) -> ElementCategory.entries.indexOfFirst { it.key == key } }
            .filter { (key, c) ->
                key != ElementCategory.OTHER.key ||
                    (c.truePositives + c.falsePositives + c.falseNegatives) > 0
            }
            .associate { (key, c) ->
                val derived = deriveMetrics(c.truePositives, c.falsePositives, c.falseNegatives, c.trueNegatives)
                key to ElementTypeSummary(
                    displayName = ElementCategory.entries.firstOrNull { it.key == key }?.displayName ?: key,
                    truePositives = c.truePositives,
                    falsePositives = c.falsePositives,
                    falseNegatives = c.falseNegatives,
                    trueNegatives = c.trueNegatives,
                    precision = derived.precision,
                    recall = derived.recall,
                    f1Score = derived.f1Score,
                    accuracy = derived.accuracy
                )
            }

        val ragSummary = if (ragEvaluatedTestCases > 0) {
            RagSummaryMetrics(
                faithfulnessMean = if (faithfulnessCount > 0) faithfulnessSum / faithfulnessCount else null,
                contextUtilizationMean = if (contextUtilizationCount > 0) contextUtilizationSum / contextUtilizationCount else null,
                evaluatedTestCases = ragEvaluatedTestCases,
                totalSamples = ragSampleTotal,
                failedSamples = ragFailedTotal
            )
        } else null

        val exactMatchAccuracy = if (total > 0)
            passed.toDouble() / total
        else 0.0

        val perClassMetrics = perClassTotals.mapValues { (_, metrics) ->

            val classPrecision =
                if (metrics.truePositives + metrics.falsePositives > 0)
                    metrics.truePositives.toDouble() /
                        (metrics.truePositives + metrics.falsePositives)
                else 0.0

            val classRecall =
                if (metrics.truePositives + metrics.falseNegatives > 0)
                    metrics.truePositives.toDouble() /
                        (metrics.truePositives + metrics.falseNegatives)
                else 0.0

            val classF1Score =
                if (classPrecision + classRecall > 0)
                    2 * (classPrecision * classRecall) /
                        (classPrecision + classRecall)
                else 0.0

            val totalClassDecisions =
                metrics.truePositives +
                metrics.falsePositives +
                metrics.falseNegatives +
                metrics.trueNegatives

            val classAccuracy =
                if (totalClassDecisions > 0)
                    (metrics.truePositives + metrics.trueNegatives).toDouble() /
                        totalClassDecisions
                else 0.0

            PerClassEvaluationMetrics(
                truePositives = metrics.truePositives,
                falsePositives = metrics.falsePositives,
                falseNegatives = metrics.falseNegatives,
                trueNegatives = metrics.trueNegatives,
                precision = classPrecision,
                recall = classRecall,
                f1Score = classF1Score,
                accuracy = classAccuracy
            )
        }
        return EvaluationReportSummary(
            total = total,
            passed = passed,
            failed = (total - passed - errors).coerceAtLeast(0),
            error = errors,
            amountOfRetries = amountOfRetries,
            precision = overall.precision,
            recall = overall.recall,
            f1Score = overall.f1Score,
            accuracy = overall.accuracy,
            exactMatchAccuracy = exactMatchAccuracy,
            totalTruePositives = totalTruePositives,
            totalFalsePositives = totalFalsePositives,
            totalFalseNegatives = totalFalseNegatives,
            totalTrueNegatives = totalTrueNegatives,
            ragMetrics = ragSummary,
            perElementType = perElementTypeSummary,
            perClassMetrics = perClassMetrics
        )
    }
}
