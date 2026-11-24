package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.adapter.auth.AuthServiceClient
import de.mertendieckmann.griplbackend.adapter.web.utils.ControllerUtils
import de.mertendieckmann.griplbackend.application.ActiveEvaluationRunExistsException
import de.mertendieckmann.griplbackend.application.EvaluationJobRunner
import de.mertendieckmann.griplbackend.config.LlmConfig
import de.mertendieckmann.griplbackend.evaluation.MultiEvaluationRunner
import de.mertendieckmann.griplbackend.model.dto.EvaluationRunDetail
import de.mertendieckmann.griplbackend.model.dto.EvaluationRunEvent
import de.mertendieckmann.griplbackend.model.dto.EvaluationRunListItem
import de.mertendieckmann.griplbackend.model.dto.MultiEvaluationRequest
import de.mertendieckmann.griplbackend.model.dto.StartEvaluationRunResponse
import de.mertendieckmann.griplbackend.repository.CustomAnalysisEndpointRepository
import de.mertendieckmann.griplbackend.repository.EvaluationRunRepository
import de.mertendieckmann.griplbackend.repository.EvaluationRunRow
import de.mertendieckmann.griplbackend.security.authenticatedUserId
import de.mertendieckmann.griplbackend.security.bearerToken
import de.mertendieckmann.griplbackend.security.requirePrivilegedGriplRole
import io.swagger.v3.oas.annotations.Operation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import tools.jackson.databind.ObjectMapper

private const val DEFAULT_EVENT_PAGE_SIZE = 500
private const val MAX_EVENT_PAGE_SIZE = 2000

/**
 * Evaluations as background jobs (see [EvaluationJobRunner]): start a run, poll its status
 * and stored events, cancel, resume and delete it. Like the streamed evaluation this is
 * admin/researcher only, and every run is private to the user who started it; other users'
 * runs are answered with 404.
 */
@RestController
@RequestMapping("/gdpr/evaluation/runs")
class EvaluationRunController(
    private val jobRunner: EvaluationJobRunner,
    private val repository: EvaluationRunRepository,
    private val env: Environment,
    private val authServiceClient: AuthServiceClient,
    private val customAnalysisEndpointRepository: CustomAnalysisEndpointRepository,
    private val objectMapper: ObjectMapper
) {

    @Operation(summary = "Start an evaluation run in the background")
    @PostMapping
    suspend fun startRun(
        @RequestBody request: MultiEvaluationRequest,
        exchange: ServerWebExchange
    ): ResponseEntity<StartEvaluationRunResponse> {
        val userId = exchange.authenticatedUserId()
        exchange.requirePrivilegedGriplRole()
        if (request.models.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one model")
        }
        val apiKey = requireOpenRouterApiKey(exchange)
        // The seed is fixed up front so a resumed run uses the same seeds per repetition.
        val storedRequest = request.copy(seed = request.seed ?: MultiEvaluationRunner.randomSeed()).withoutApiKeys()
        val runRequest = prepareRunRequest(storedRequest, apiKey, userId)
        val id = withContext(Dispatchers.IO) {
            try {
                jobRunner.start(userId, storedRequest, runRequest)
            } catch (e: ActiveEvaluationRunExistsException) {
                throw activeRunConflict()
            }
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(StartEvaluationRunResponse(id))
    }

    @Operation(summary = "List the caller's evaluation runs, newest first")
    @GetMapping
    suspend fun listRuns(exchange: ServerWebExchange): List<EvaluationRunListItem> {
        val userId = exchange.authenticatedUserId()
        exchange.requirePrivilegedGriplRole()
        return withContext(Dispatchers.IO) { repository.listByOwner(userId).map { it.toListItem() } }
    }

    @Operation(summary = "Status, metadata and current progress of one evaluation run")
    @GetMapping("/{id}")
    suspend fun getRun(@PathVariable id: Long, exchange: ServerWebExchange): EvaluationRunDetail {
        val userId = exchange.authenticatedUserId()
        exchange.requirePrivilegedGriplRole()
        val row = withContext(Dispatchers.IO) { requireOwnedRun(id, userId) }
        return EvaluationRunDetail(
            run = row.toListItem(),
            metadata = row.metadataJson?.let { objectMapper.readTree(it) },
            currentSteps = if (row.status.isActive) jobRunner.currentSteps(id) else emptyList()
        )
    }

    @Operation(
        summary = "Stored events of an evaluation run",
        description = "testCase, error and summary events with an id greater than 'after', oldest first, " +
            "in the same JSON form as the NDJSON stream. Page through with the id of the last event."
    )
    @GetMapping("/{id}/events")
    suspend fun getEvents(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") after: Long,
        @RequestParam(defaultValue = DEFAULT_EVENT_PAGE_SIZE.toString()) limit: Int,
        exchange: ServerWebExchange
    ): List<EvaluationRunEvent> {
        val userId = exchange.authenticatedUserId()
        exchange.requirePrivilegedGriplRole()
        return withContext(Dispatchers.IO) {
            requireOwnedRun(id, userId)
            repository.eventsAfter(id, userId, after, limit.coerceIn(1, MAX_EVENT_PAGE_SIZE))
        }
    }

    @Operation(summary = "Cancel a running evaluation; finished test cases stay stored")
    @PostMapping("/{id}/cancel")
    suspend fun cancelRun(@PathVariable id: Long, exchange: ServerWebExchange): ResponseEntity<Void> {
        val userId = exchange.authenticatedUserId()
        exchange.requirePrivilegedGriplRole()
        val row = withContext(Dispatchers.IO) { requireOwnedRun(id, userId) }
        if (!row.status.isActive) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "The evaluation is not running")
        }
        withContext(Dispatchers.IO) { jobRunner.cancel(id) }
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Resume a cancelled, interrupted or failed evaluation run")
    @PostMapping("/{id}/resume")
    suspend fun resumeRun(@PathVariable id: Long, exchange: ServerWebExchange): ResponseEntity<StartEvaluationRunResponse> {
        val userId = exchange.authenticatedUserId()
        exchange.requirePrivilegedGriplRole()
        val row = withContext(Dispatchers.IO) { requireOwnedRun(id, userId) }
        if (!row.status.isResumable) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Only cancelled, interrupted or failed evaluations can be resumed")
        }
        val apiKey = requireOpenRouterApiKey(exchange)
        val storedRequest = objectMapper.readValue(row.requestJson, MultiEvaluationRequest::class.java)
        val runRequest = prepareRunRequest(storedRequest, apiKey, userId)
        val resumed = withContext(Dispatchers.IO) {
            try {
                jobRunner.resume(id, userId, runRequest)
            } catch (e: ActiveEvaluationRunExistsException) {
                throw activeRunConflict()
            }
        }
        if (!resumed) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Only cancelled, interrupted or failed evaluations can be resumed")
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(StartEvaluationRunResponse(id))
    }

    @Operation(summary = "Delete an evaluation run and its results")
    @DeleteMapping("/{id}")
    suspend fun deleteRun(@PathVariable id: Long, exchange: ServerWebExchange): ResponseEntity<Void> {
        val userId = exchange.authenticatedUserId()
        exchange.requirePrivilegedGriplRole()
        withContext(Dispatchers.IO) {
            requireOwnedRun(id, userId)
            if (!repository.deleteIfNotActive(id, userId)) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Cancel the evaluation before deleting it")
            }
        }
        return ResponseEntity.noContent().build()
    }

    private fun requireOwnedRun(id: Long, userId: String): EvaluationRunRow =
        repository.getByIdAndOwner(id, userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation run not found")

    private fun activeRunConflict() =
        ResponseStatusException(HttpStatus.CONFLICT, "An evaluation is already running. Wait for it or cancel it first")

    private suspend fun requireOpenRouterApiKey(exchange: ServerWebExchange): String =
        authServiceClient.getOpenRouterApiKey(exchange.bearerToken())
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Set your OpenRouter API key in account settings before running an evaluation"
            )

    /** Resolves environment placeholders, adds the key and checks every endpoint is built-in or the caller's own. */
    private fun prepareRunRequest(request: MultiEvaluationRequest, apiKey: String, userId: String): MultiEvaluationRequest {
        val resolved = ControllerUtils.resolveEnvironmentVariables(request, env)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request after resolving environment variables")
        val endpoints = listOf(resolved.defaultEvaluationEndpoint) + resolved.models.map { it.evaluationEndpoint }
        endpoints.forEach { ControllerUtils.requireOwnedCustomEndpoint(it, customAnalysisEndpointRepository, userId) }
        return resolved.copy(models = resolved.models.map {
            it.copy(llmProps = (it.llmProps ?: LlmConfig.Companion.LlmPropsOverride()).copy(apiKey = apiKey))
        })
    }

    private fun MultiEvaluationRequest.withoutApiKeys(): MultiEvaluationRequest =
        copy(models = models.map { it.copy(llmProps = it.llmProps?.copy(apiKey = null)) })

    private fun EvaluationRunRow.toListItem(): EvaluationRunListItem {
        val request = objectMapper.readTree(requestJson)
        val metadata = metadataJson?.let { objectMapper.readTree(it) }
        return EvaluationRunListItem(
            id = id,
            status = status,
            modelLabels = request.path("models").values().map { it.path("label").asString() },
            datasetNames = metadata?.path("datasets")?.values()?.map { it.path("name").asString() }.orEmpty(),
            completedTestCases = completedTestCases,
            totalTestCases = totalTestCases,
            errorMessage = errorMessage,
            createdAt = createdAt,
            startedAt = startedAt,
            finishedAt = finishedAt
        )
    }
}
