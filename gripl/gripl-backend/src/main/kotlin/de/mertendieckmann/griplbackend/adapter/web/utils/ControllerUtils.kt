package de.mertendieckmann.griplbackend.adapter.web.utils

import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import org.springframework.core.env.Environment
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.FilePart
import de.mertendieckmann.griplbackend.repository.CustomAnalysisEndpointRepository
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

object ControllerUtils {

    const val CUSTOM_ENDPOINT_PREFIX = "/gdpr/analysis/custom/"

    /**
     * If [endpoint] points at a custom analysis endpoint (`/gdpr/analysis/custom/{id}`), require
     * that it exists *and belongs to [ownerUserId]* — someone else's endpoint is reported exactly
     * like a missing one. Built-in endpoints (and null) pass through. Call this at the request
     * boundary wherever a client-supplied endpoint string will later be run by code that has no
     * user context (the process-model job runner, the evaluator).
     */
    fun requireOwnedCustomEndpoint(
        endpoint: String?,
        repository: CustomAnalysisEndpointRepository,
        ownerUserId: String
    ) {
        if (endpoint == null || !endpoint.startsWith(CUSTOM_ENDPOINT_PREFIX)) return
        val id = endpoint.removePrefix(CUSTOM_ENDPOINT_PREFIX).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed custom analysis endpoint '$endpoint'")
        if (repository.getByIdAndOwner(id, ownerUserId) == null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No custom analysis endpoint found for id $id")
        }
    }

    fun getBpmnXmlMono(file: FilePart): Mono<String> {
        return DataBufferUtils
            .join(file.content())
            .map { dataBuffer ->
                dataBuffer.asInputStream().bufferedReader().use { it.readText() }
            }
    }

    inline fun <reified T> resolveEnvironmentVariables(objectToResolve: T?, env: Environment): T? {
        return objectToResolve.let {
            jacksonObjectMapper().readValue<T>(
                env.resolvePlaceholders(jacksonObjectMapper().writeValueAsString(it))
            )
        }
    }
}