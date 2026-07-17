package de.mertendieckmann.griplbackend.model.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Mirror of the Python /api/evaluate/ragas endpoint contract.
 * The Python service emits snake_case; @JsonProperty maps to it.
 */

data class RagasSampleRequest(
    val id: String,
    val query: String,
    val contexts: List<String>,
    val response: String
)

data class RagasEvaluationRequest(
    val samples: List<RagasSampleRequest>,
    val metrics: List<String>? = null
)

data class RagasSampleResult(
    val id: String,
    @JsonProperty("faithfulness") val faithfulness: Double? = null,
    @JsonProperty("context_utilization") val contextUtilization: Double? = null,
    val error: String? = null
)

data class RagasAggregate(
    @JsonProperty("faithfulness_mean") val faithfulnessMean: Double? = null,
    @JsonProperty("context_utilization_mean") val contextUtilizationMean: Double? = null,
    @JsonProperty("sample_count") val sampleCount: Int = 0,
    @JsonProperty("failed_count") val failedCount: Int = 0
)

data class RagasEvaluationResponse(
    val samples: List<RagasSampleResult>,
    val aggregate: RagasAggregate,
    val status: String = "success"
)
