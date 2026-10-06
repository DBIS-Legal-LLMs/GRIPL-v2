package de.mertendieckmann.griplbackend.model.dto

import de.mertendieckmann.griplbackend.config.LlmConfig

data class EvaluationRequest(
    val evaluationEndpoint: String = "/gdpr/analysis/prompt-engineering",
    var llmProps: LlmConfig.Companion.LlmPropsOverride? = null,
    val maxConcurrent: Int = 4,
    val datasets: List<Int>,
    val evaluationDataIds: List<Int> = emptyList(),
    val useRag: Boolean = false,
    val ragMode: RagMode = RagMode.HYBRID,
    val evaluateRag: Boolean = true,
    val activitiesOnly: Boolean = false
) {
    override fun toString(): String =
        "EvaluationRequest(evaluationEndpoint=$evaluationEndpoint, useRag=$useRag, ragMode=$ragMode, evaluateRag=$evaluateRag, activitiesOnly=$activitiesOnly, llmProps=${llmProps?.copy(apiKey = llmProps?.apiKey?.let { "\"****\"" })})"
}
