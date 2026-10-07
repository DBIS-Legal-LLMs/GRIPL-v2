package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.model.dto.CreateCustomAnalysisEndpointRequest
import de.mertendieckmann.griplbackend.model.dto.CustomAnalysisEndpoint
import de.mertendieckmann.griplbackend.repository.CustomAnalysisEndpointRepository
import de.mertendieckmann.griplbackend.security.authenticatedUserId
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange

@RestController
@RequestMapping("/custom-analysis-endpoints")
class CustomAnalysisEndpointController(
    private val repository: CustomAnalysisEndpointRepository
) {

    @Operation(
        summary = "Create a custom analysis endpoint",
        description = "Uploads a new analysis endpoint: a name, a prompt, and whether it produces binary or multiclass output."
    )
    @PostMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun create(
        @RequestBody request: CreateCustomAnalysisEndpointRequest,
        exchange: ServerWebExchange
    ): ResponseEntity<CustomAnalysisEndpoint> {
        val userId = exchange.authenticatedUserId()
        val id = repository.create(
            name = request.name,
            promptText = request.promptText,
            responseType = request.responseType,
            ragEnabled = request.ragEnabled,
            ragMode = request.ragMode,
            ownerUserId = userId
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.getByIdAndOwner(id, userId))
    }

    @Operation(
        summary = "List all custom analysis endpoints",
        description = "Returns the custom analysis endpoints uploaded by the authenticated user."
    )
    @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun listAll(exchange: ServerWebExchange): List<CustomAnalysisEndpoint> {
        return repository.listByOwner(exchange.authenticatedUserId())
    }

    @Operation(
        summary = "Delete a custom analysis endpoint",
        description = "Deletes a custom analysis endpoint. Process models already analyzed with it keep their results."
    )
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long, exchange: ServerWebExchange): ResponseEntity<Void> {
        // Someone else's endpoint is reported exactly like a missing one.
        if (!repository.deleteByOwner(id, exchange.authenticatedUserId())) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.noContent().build()
    }
}
