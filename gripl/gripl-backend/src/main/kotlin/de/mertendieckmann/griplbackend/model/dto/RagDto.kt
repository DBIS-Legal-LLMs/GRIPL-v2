package de.mertendieckmann.griplbackend.model.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

enum class RagMode(@get:JsonValue val value: String) {
    NAIVE("naive"),
    LOCAL("local"),
    GLOBAL("global"),
    HYBRID("hybrid");

    override fun toString(): String = value

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(raw: String): RagMode =
            entries.firstOrNull { it.value.equals(raw.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException(
                    "Invalid RAG mode '$raw'. Valid modes: ${entries.joinToString(", ") { it.value }}"
                )
    }
}

data class RagRequest(
    val query: String,
    val mode: RagMode
)

data class RagResponseWrapper(
    val query: String,
    val mode: String,
    val status: String,
    val response: Map<String, Any>
)

/**
 * Mirror of the Python /api/status endpoint contract (whether the knowledge
 * graph currently holds ingested data).
 */
data class RagStatusResponse(
    val ingested: Boolean,
    @JsonProperty("node_count") val nodeCount: Int
)
