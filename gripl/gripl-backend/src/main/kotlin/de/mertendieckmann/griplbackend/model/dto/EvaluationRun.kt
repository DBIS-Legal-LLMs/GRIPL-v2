package de.mertendieckmann.griplbackend.model.dto

import de.mertendieckmann.griplbackend.model.evaluation.EvaluationMetrics
import tools.jackson.databind.JsonNode

enum class EvaluationRunStatus {
    QUEUED, RUNNING, DONE, CANCELLED, INTERRUPTED, ERROR;

    val isActive: Boolean get() = this == QUEUED || this == RUNNING
    val isResumable: Boolean get() = this == CANCELLED || this == INTERRUPTED || this == ERROR

    companion object {
        val ACTIVE = listOf(QUEUED, RUNNING)
        val RESUMABLE = listOf(CANCELLED, INTERRUPTED, ERROR)
    }
}

/** Light row for the list of a user's evaluation runs. */
data class EvaluationRunListItem(
    val id: Long,
    val status: EvaluationRunStatus,
    val modelLabels: List<String>,
    val datasetNames: List<String>,
    /** testCase and error events stored so far (all models and repetitions). */
    val completedTestCases: Int,
    /** Test cases × models × repetitions; null until the run has started. */
    val totalTestCases: Int?,
    val errorMessage: String?,
    val createdAt: String,
    val startedAt: String?,
    val finishedAt: String?
)

data class EvaluationRunDetail(
    val run: EvaluationRunListItem,
    /** The EvaluationMetadataReport event as JSON, exactly as the NDJSON stream sends it. */
    val metadata: JsonNode?,
    /** Test cases that are being evaluated right now (stepInfo envelopes). Empty when the run is not active. */
    val currentSteps: List<ModelReportEnvelope>
)

/** A stored testCase, error or summary event; [report] has the same JSON form as in the NDJSON stream. */
data class EvaluationRunEvent(
    val id: Long,
    val modelLabel: String,
    val runNumber: Int,
    val report: JsonNode
)

data class StartEvaluationRunResponse(val id: Long)

/** Key of one model in one repetition of a run. */
data class RunModelKey(val runNumber: Int, val modelLabel: String)

/**
 * What a run already finished for one model in one repetition. Resuming skips the finished
 * test cases and seeds the summary with their metrics, so the summary covers all test cases.
 */
data class ResumeState(
    val completedTestCaseIds: Set<Long> = emptySet(),
    val completedMetrics: List<EvaluationMetrics> = emptyList(),
    /** The summary of this model and repetition is stored, nothing is left to do. */
    val finished: Boolean = false
)
