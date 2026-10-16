package de.mertendieckmann.griplbackend.model.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class CustomAnalysisResponseType(@get:JsonValue val value: String) {
    BINARY("BINARY"),
    MULTICLASS("MULTICLASS");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(raw: String): CustomAnalysisResponseType =
            entries.firstOrNull { it.value.equals(raw.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException(
                    "Invalid response type '$raw'. Valid types: ${entries.joinToString(", ") { it.value }}"
                )
    }
}

/**
 * [ragEnabled]/[ragMode] are fixed on the endpoint itself — like [responseType], RAG usage is a
 * property of the endpoint you choose, not something re-picked on every analysis run.
 */
data class CustomAnalysisEndpoint(
    val id: Long,
    val name: String,
    val promptText: String,
    val responseType: CustomAnalysisResponseType,
    val ragEnabled: Boolean,
    val ragMode: RagMode?,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromRow(
            id: Long,
            name: String,
            promptText: String,
            responseType: String,
            ragEnabled: Boolean,
            ragMode: String?,
            createdAt: String,
            updatedAt: String
        ): CustomAnalysisEndpoint = CustomAnalysisEndpoint(
            id = id,
            name = name,
            promptText = promptText,
            responseType = CustomAnalysisResponseType.fromString(responseType),
            ragEnabled = ragEnabled,
            ragMode = ragMode?.let { RagMode.fromString(it) },
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

data class CreateCustomAnalysisEndpointRequest(
    val name: String,
    val promptText: String,
    val responseType: CustomAnalysisResponseType,
    val ragEnabled: Boolean = false,
    val ragMode: RagMode? = null
)
