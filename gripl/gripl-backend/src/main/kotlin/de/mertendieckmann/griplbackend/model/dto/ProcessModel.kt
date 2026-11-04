package de.mertendieckmann.griplbackend.model.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class ProcessModelStatus(@get:JsonValue val value: String) {
    PENDING("PENDING"),
    QUEUED("QUEUED"),
    RUNNING("RUNNING"),
    DONE("DONE"),
    ERROR("ERROR");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(raw: String): ProcessModelStatus =
            entries.firstOrNull { it.value.equals(raw.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid process model status '$raw'")
    }
}

data class ProcessModel(
    val id: Long,
    val name: String,
    val bpmnXml: String,
    val status: ProcessModelStatus,
    val analysisEndpoint: String?,
    val analysisOptionsJson: String?,
    val analysisResultJson: String?,
    val amountOfRetries: Int?,
    val totalElements: Int?,
    val criticalElementCount: Int?,
    val errorMessage: String?,
    val createdAt: String,
    val updatedAt: String,
    /** The diagram was saved after [analysisResultJson] was produced, so the result no longer matches it. */
    val analysisOutdated: Boolean = false,
    /** Bumped on every diagram save; a save has to name the version it was based on. */
    val bpmnVersion: Int
) {
    companion object {
        fun fromRow(
            id: Long,
            name: String,
            bpmnXml: String,
            status: String,
            analysisEndpoint: String?,
            analysisOptionsJson: String?,
            analysisResultJson: String?,
            amountOfRetries: Int?,
            totalElements: Int?,
            criticalElementCount: Int?,
            errorMessage: String?,
            createdAt: String,
            updatedAt: String,
            analysisOutdated: Boolean,
            bpmnVersion: Int
        ): ProcessModel = ProcessModel(
            id = id,
            name = name,
            bpmnXml = bpmnXml,
            status = ProcessModelStatus.fromString(status),
            analysisEndpoint = analysisEndpoint,
            analysisOptionsJson = analysisOptionsJson,
            analysisResultJson = analysisResultJson,
            amountOfRetries = amountOfRetries,
            totalElements = totalElements,
            criticalElementCount = criticalElementCount,
            errorMessage = errorMessage,
            createdAt = createdAt,
            updatedAt = updatedAt,
            analysisOutdated = analysisOutdated,
            bpmnVersion = bpmnVersion
        )
    }
}

data class ProcessModelListItemDto(
    val id: Long,
    val name: String,
    val status: ProcessModelStatus,
    val analysisEndpoint: String?,
    val totalElements: Int?,
    val criticalElementCount: Int?,
    val errorMessage: String?,
    val createdAt: String,
    val updatedAt: String,
    val analysisOutdated: Boolean = false
)

data class ProcessModelDetailDto(
    val id: Long,
    val name: String,
    val bpmnXml: String,
    val status: ProcessModelStatus,
    val analysisEndpoint: String?,
    val analysisResult: Any?,
    val amountOfRetries: Int?,
    val totalElements: Int?,
    val criticalElementCount: Int?,
    val errorMessage: String?,
    val createdAt: String,
    val updatedAt: String,
    val analysisOutdated: Boolean = false,
    val bpmnVersion: Int
)
