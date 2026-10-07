package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.adapter.auth.AuthServiceClient
import de.mertendieckmann.griplbackend.adapter.web.utils.ControllerUtils
import de.mertendieckmann.griplbackend.application.ProcessModelJobRunner
import de.mertendieckmann.griplbackend.config.LlmConfig
import de.mertendieckmann.griplbackend.model.dto.EnqueueAnalysisRequest
import de.mertendieckmann.griplbackend.model.dto.EnqueueAnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.ProcessModel
import de.mertendieckmann.griplbackend.model.dto.ProcessModelDetailDto
import de.mertendieckmann.griplbackend.model.dto.ProcessModelListItemDto
import de.mertendieckmann.griplbackend.model.dto.ProcessModelStatus
import de.mertendieckmann.griplbackend.repository.ProcessModelRepository
import de.mertendieckmann.griplbackend.security.authenticatedUserId
import de.mertendieckmann.griplbackend.security.bearerToken
import io.swagger.v3.oas.annotations.Operation
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@RestController
@RequestMapping("/process-models")
class ProcessModelController(
    private val repository: ProcessModelRepository,
    private val jobRunner: ProcessModelJobRunner,
    private val objectMapper: ObjectMapper,
    private val authServiceClient: AuthServiceClient
) {

    @Operation(
        summary = "Upload a process model",
        description = "Upload a BPMN XML document (file part **bpmnFile**) to be added to the persisted process model list."
    )
    @PostMapping("", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun uploadProcessModel(
        @RequestPart("bpmnFile") file: FilePart,
        @RequestPart("name", required = false) namePart: FormFieldPart?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ProcessModelListItemDto>> {
        val userId = exchange.authenticatedUserId()
        val name = namePart?.value()?.takeIf { it.isNotBlank() } ?: file.filename()

        return ControllerUtils.getBpmnXmlMono(file).flatMap { bpmnXml ->
            Mono.fromCallable {
                val id = repository.create(name, bpmnXml, userId)
                repository.getByIdAndOwner(id, userId)!!.toListItemDto()
            }.subscribeOn(Schedulers.boundedElastic())
        }.map { ResponseEntity.status(HttpStatus.CREATED).body(it) }
    }

    @Operation(
        summary = "List all process models",
        description = "Returns every uploaded process model (without its BPMN XML or full result, for cheap polling)."
    )
    @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun listProcessModels(exchange: ServerWebExchange): Mono<List<ProcessModelListItemDto>> {
        val userId = exchange.authenticatedUserId()
        return Mono.fromCallable { repository.listByOwner(userId) }.subscribeOn(Schedulers.boundedElastic())
    }

    @Operation(
        summary = "Get a process model",
        description = "Returns the full process model, including its BPMN XML and analysis result if available."
    )
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getProcessModel(@PathVariable id: Long, exchange: ServerWebExchange): Mono<ResponseEntity<ProcessModelDetailDto>> {
        val userId = exchange.authenticatedUserId()
        // fromCallable returning null completes *empty*, not with a null value —
        // so "not found" has to be the defaultIfEmpty fallback, not a null check.
        return Mono.fromCallable { repository.getByIdAndOwner(id, userId) }
            .subscribeOn(Schedulers.boundedElastic())
            .map { model -> ResponseEntity.ok(model.toDetailDto()) }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }

    @Operation(
        summary = "Delete a process model",
        description = "Deletes a process model, unless it is currently being analyzed."
    )
    @DeleteMapping("/{id}")
    fun deleteProcessModel(@PathVariable id: Long, exchange: ServerWebExchange): Mono<ResponseEntity<Void>> {
        val userId = exchange.authenticatedUserId()
        return Mono.fromCallable {
            val model = repository.getByIdAndOwner(id, userId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No process model found for id $id")
            if (model.status == ProcessModelStatus.RUNNING) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Process model $id is currently being analyzed")
            }
            repository.deleteIfNotRunning(id, userId)
        }.subscribeOn(Schedulers.boundedElastic()).map { ResponseEntity.noContent().build<Void>() }
    }

    @Operation(
        summary = "Enqueue process models for analysis",
        description = "Queues the given process model ids for sequential analysis. Ids already queued or running are skipped."
    )
    @PostMapping("/analyze", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun analyzeProcessModels(
        @RequestBody request: EnqueueAnalysisRequest,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<EnqueueAnalysisResponse>> {
        val userId = exchange.authenticatedUserId()
        val bearerToken = exchange.bearerToken()

        // Note: a suspend block returning null makes `mono { }` complete
        // *empty* (Reactor forbids onNext(null)) rather than emitting null,
        // so a missing key has to be turned into a thrown exception here —
        // checking for null downstream in flatMap would just never run.
        return mono {
            authServiceClient.getOpenRouterApiKey(bearerToken)
                ?: throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Set your OpenRouter API key in account settings before running an analysis"
                )
        }
            .flatMap { apiKey ->
                val requestWithKey = request.copy(
                    llmProps = (request.llmProps ?: LlmConfig.Companion.LlmPropsOverride()).copy(apiKey = apiKey)
                )

                Mono.fromCallable {
                    // Ids that aren't the caller's own are skipped exactly like
                    // ones that don't exist — never enqueued, never revealed.
                    val owned = repository.filterOwnedIds(requestWithKey.ids, userId)
                    val enqueued = jobRunner.enqueue(requestWithKey.copy(ids = owned))
                    val skipped = requestWithKey.ids.filter { it !in enqueued }
                    EnqueueAnalysisResponse(enqueuedIds = enqueued, skippedIds = skipped)
                }.subscribeOn(Schedulers.boundedElastic())
                    .map { ResponseEntity.status(HttpStatus.ACCEPTED).body(it) }
            }
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
