package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.adapter.web.utils.ControllerUtils
import de.mertendieckmann.griplbackend.application.ProcessModelJobRunner
import de.mertendieckmann.griplbackend.model.dto.EnqueueAnalysisRequest
import de.mertendieckmann.griplbackend.model.dto.EnqueueAnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.ProcessModel
import de.mertendieckmann.griplbackend.model.dto.ProcessModelDetailDto
import de.mertendieckmann.griplbackend.model.dto.ProcessModelListItemDto
import de.mertendieckmann.griplbackend.model.dto.ProcessModelStatus
import de.mertendieckmann.griplbackend.repository.ProcessModelRepository
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@RestController
@RequestMapping("/process-models")
class ProcessModelController(
    private val repository: ProcessModelRepository,
    private val jobRunner: ProcessModelJobRunner,
    private val objectMapper: ObjectMapper
) {

    @Operation(
        summary = "Upload a process model",
        description = "Upload a BPMN XML document (file part **bpmnFile**) to be added to the persisted process model list."
    )
    @PostMapping("", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun uploadProcessModel(
        @RequestPart("bpmnFile") file: FilePart,
        @RequestPart("name", required = false) namePart: FormFieldPart?
    ): Mono<ResponseEntity<ProcessModelListItemDto>> {
        val name = namePart?.value()?.takeIf { it.isNotBlank() } ?: file.filename()

        return ControllerUtils.getBpmnXmlMono(file).flatMap { bpmnXml ->
            Mono.fromCallable {
                val id = repository.create(name, bpmnXml)
                repository.getById(id)!!.toListItemDto()
            }.subscribeOn(Schedulers.boundedElastic())
        }.map { ResponseEntity.status(HttpStatus.CREATED).body(it) }
    }

    @Operation(
        summary = "List all process models",
        description = "Returns every uploaded process model (without its BPMN XML or full result, for cheap polling)."
    )
    @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun listProcessModels(): Mono<List<ProcessModelListItemDto>> {
        return Mono.fromCallable { repository.listAll() }.subscribeOn(Schedulers.boundedElastic())
    }

    @Operation(
        summary = "Get a process model",
        description = "Returns the full process model, including its BPMN XML and analysis result if available."
    )
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getProcessModel(@PathVariable id: Long): Mono<ResponseEntity<ProcessModelDetailDto>> {
        return Mono.fromCallable { repository.getById(id) }
            .subscribeOn(Schedulers.boundedElastic())
            .map { model ->
                if (model == null) {
                    ResponseEntity.notFound().build()
                } else {
                    ResponseEntity.ok(model.toDetailDto())
                }
            }
    }

    @Operation(
        summary = "Delete a process model",
        description = "Deletes a process model, unless it is currently being analyzed."
    )
    @DeleteMapping("/{id}")
    fun deleteProcessModel(@PathVariable id: Long): Mono<ResponseEntity<Void>> {
        return Mono.fromCallable {
            val model = repository.getById(id)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No process model found for id $id")
            if (model.status == ProcessModelStatus.RUNNING) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Process model $id is currently being analyzed")
            }
            repository.deleteIfNotRunning(id)
        }.subscribeOn(Schedulers.boundedElastic()).map { ResponseEntity.noContent().build<Void>() }
    }

    @Operation(
        summary = "Enqueue process models for analysis",
        description = "Queues the given process model ids for sequential analysis. Ids already queued or running are skipped."
    )
    @PostMapping("/analyze", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun analyzeProcessModels(@RequestBody request: EnqueueAnalysisRequest): Mono<ResponseEntity<EnqueueAnalysisResponse>> {
        return Mono.fromCallable {
            val enqueued = jobRunner.enqueue(request)
            val skipped = request.ids.filter { it !in enqueued }
            EnqueueAnalysisResponse(enqueuedIds = enqueued, skippedIds = skipped)
        }.subscribeOn(Schedulers.boundedElastic())
            .map { ResponseEntity.status(HttpStatus.ACCEPTED).body(it) }
    }

    private fun ProcessModel.toListItemDto(): ProcessModelListItemDto {
        return ProcessModelListItemDto(
            id = id,
            name = name,
            status = status,
            analysisEndpoint = analysisEndpoint,
            totalElements = totalElements,
            criticalElementCount = criticalElementCount,
            errorMessage = errorMessage,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun ProcessModel.toDetailDto(): ProcessModelDetailDto {
        return ProcessModelDetailDto(
            id = id,
            name = name,
            bpmnXml = bpmnXml,
            status = status,
            analysisEndpoint = analysisEndpoint,
            analysisResult = analysisResultJson?.let { objectMapper.readValue<Any>(it) },
            amountOfRetries = amountOfRetries,
            totalElements = totalElements,
            criticalElementCount = criticalElementCount,
            errorMessage = errorMessage,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
