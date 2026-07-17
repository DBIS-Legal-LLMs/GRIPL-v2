package de.mertendieckmann.griplbackend.model.dto

import de.mertendieckmann.griplbackend.config.LlmConfig

data class MultiEvaluationRequest(
    val defaultEvaluationEndpoint: String,
    val maxConcurrent: Int = 4,
    val models: List<ModelRunConfig>,
    val seed: Int?,
    val datasets: List<Int>,
    val evaluationDataIds: List<Int> = emptyList(),
    val repetitions: Int = 1,
    val useRag: Boolean = false,
    val ragMode: RagMode = RagMode.HYBRID,
    val evaluateRag: Boolean = true,
    val activitiesOnly: Boolean = false,
)

data class ModelRunConfig(
    val label: String,
    val evaluationEndpoint: String? = null,
    val llmProps: LlmConfig.Companion.LlmPropsOverride? = null,
) {
    override fun toString(): String =
        "EvaluationRequest(label=$label, llmProps=${llmProps?.copy(apiKey = llmProps.apiKey?.let { "\"****\"" })})"
}
