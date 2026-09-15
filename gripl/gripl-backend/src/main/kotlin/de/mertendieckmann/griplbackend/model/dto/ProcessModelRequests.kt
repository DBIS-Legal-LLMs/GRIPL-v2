package de.mertendieckmann.griplbackend.model.dto

import de.mertendieckmann.griplbackend.config.LlmConfig

data class EnqueueAnalysisRequest(
    val ids: List<Long>,
    val endpoint: String,
    val llmProps: LlmConfig.Companion.LlmPropsOverride? = null,
    val useRag: Boolean? = null,
    val ragMode: RagMode? = null,
    val activitiesOnly: Boolean? = null
)

data class EnqueueAnalysisResponse(
    val enqueuedIds: List<Long>,
    val skippedIds: List<Long>
)

/**
 * Persisted alongside a queued process_model row so the job runner can resume
 * a still-QUEUED job with its original parameters after an app restart.
 */
data class ProcessModelAnalysisOptions(
    val llmProps: LlmConfig.Companion.LlmPropsOverride? = null,
    val useRag: Boolean? = null,
    val ragMode: RagMode? = null,
    val activitiesOnly: Boolean? = null
)
