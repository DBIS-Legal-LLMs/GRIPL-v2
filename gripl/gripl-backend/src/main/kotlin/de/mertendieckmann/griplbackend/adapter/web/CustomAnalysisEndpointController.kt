package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.model.dto.CreateCustomAnalysisEndpointRequest
import de.mertendieckmann.griplbackend.model.dto.CustomAnalysisEndpoint
import de.mertendieckmann.griplbackend.repository.CustomAnalysisEndpointRepository
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
    fun create(@RequestBody request: CreateCustomAnalysisEndpointRequest): ResponseEntity<CustomAnalysisEndpoint> {
        val id = repository.create(
            name = request.name,
            promptText = request.promptText,
            responseType = request.responseType,
            ragEnabled = request.ragEnabled,
            ragMode = request.ragMode
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.getById(id))
    }

    @Operation(
        summary = "List all custom analysis endpoints",
        description = "Returns every uploaded custom analysis endpoint."
    )
    @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun listAll(): List<CustomAnalysisEndpoint> {
        return repository.listAll()
    }

    @Operation(
        summary = "Delete a custom analysis endpoint",
        description = "Deletes a custom analysis endpoint. Process models already analyzed with it keep their results."
    )
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        if (repository.getById(id) == null) {
            return ResponseEntity.notFound().build()
        }
        repository.delete(id)
        return ResponseEntity.noContent().build()
    }
}
