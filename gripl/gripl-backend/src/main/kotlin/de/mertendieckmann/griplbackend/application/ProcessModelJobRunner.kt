package de.mertendieckmann.griplbackend.application

import de.mertendieckmann.griplbackend.application.analyzer.AnalysisService
import de.mertendieckmann.griplbackend.model.dto.EnqueueAnalysisRequest
import de.mertendieckmann.griplbackend.model.dto.ProcessModelAnalysisOptions
import de.mertendieckmann.griplbackend.model.dto.ProcessModelStatus
import de.mertendieckmann.griplbackend.model.dto.RagMode
import de.mertendieckmann.griplbackend.repository.ProcessModelRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

private const val PROMPT_ENGINEERING_ENDPOINT = "/gdpr/analysis/prompt-engineering"
private const val BASELINE_ENDPOINT = "/gdpr/analysis/baseline"
private const val MULTICLASS_ENDPOINT = "/gdpr/analysis/multiclass"

/**
 * Runs process-model analyses one at a time on a single background thread.
 * Postgres (process_model.status) is the durable source of truth; the in-memory
 * queue only ever carries ids, never parameters, so a still-QUEUED row can be
 * faithfully resumed after a restart from analysis_options alone.
 */
@Service
class ProcessModelJobRunner(
    private val repository: ProcessModelRepository,
    private val analysisService: AnalysisService
) {
    private val log = KotlinLogging.logger { }
    private val objectMapper = jacksonObjectMapper()
    private val queue = LinkedBlockingQueue<Long>()
    private val inFlight = mutableSetOf<Long>()
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "process-model-job-runner").apply { isDaemon = true }
    }

    init {
        executor.submit { workerLoop() }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun recoverOnStartup() {
        val resetCount = repository.resetStaleRunningToError("Interrupted by application restart")
        if (resetCount > 0) {
            log.warn { "Reset $resetCount process model(s) stuck in RUNNING to ERROR after restart" }
        }

        val queuedIds = repository.getIdsByStatus(ProcessModelStatus.QUEUED)
        synchronized(inFlight) {
            queuedIds.forEach { id ->
                inFlight.add(id)
                queue.offer(id)
            }
        }
        if (queuedIds.isNotEmpty()) {
            log.info { "Resumed ${queuedIds.size} queued process model job(s) after restart" }
        }
    }

    fun enqueue(request: EnqueueAnalysisRequest): List<Long> {
        val options = ProcessModelAnalysisOptions(
            llmProps = request.llmProps,
            useRag = request.useRag,
            ragMode = request.ragMode,
            activitiesOnly = request.activitiesOnly
        )
        val optionsJson = objectMapper.writeValueAsString(options)

        val enqueued = mutableListOf<Long>()
        synchronized(inFlight) {
            request.ids.forEach { id ->
                if (inFlight.add(id)) {
                    repository.markQueued(id, request.endpoint, optionsJson)
                    queue.offer(id)
                    enqueued.add(id)
                }
            }
        }
        return enqueued
    }

    private fun workerLoop() {
        while (true) {
            val id = queue.take()
            try {
                processModel(id)
            } catch (e: Exception) {
                log.error(e) { "Unexpected error processing process model $id" }
                repository.markError(id, e.message ?: "Unknown error")
            } finally {
                synchronized(inFlight) { inFlight.remove(id) }
            }
        }
    }

    private fun processModel(id: Long) {
        val model = repository.getById(id)
        if (model == null) {
            log.warn { "Process model $id no longer exists, skipping" }
            return
        }

        repository.markRunning(id)

        val options: ProcessModelAnalysisOptions = model.analysisOptionsJson
            ?.let { objectMapper.readValue(it) }
            ?: ProcessModelAnalysisOptions()
        val endpoint = model.analysisEndpoint ?: PROMPT_ENGINEERING_ENDPOINT

        try {
            val (resultJson, criticalCount, amountOfRetries) = when (endpoint) {
                PROMPT_ENGINEERING_ENDPOINT -> {
                    val result = analysisService.analyzePromptEngineering(
                        bpmnXml = model.bpmnXml,
                        llmPropsOverride = options.llmProps,
                        useRag = options.useRag ?: false,
                        ragMode = options.ragMode ?: RagMode.HYBRID,
                        activitiesOnly = options.activitiesOnly ?: false
                    )
                    Triple(objectMapper.writeValueAsString(result), result.criticalElements.size, result.amountOfRetries)
                }
                BASELINE_ENDPOINT -> {
                    val result = analysisService.analyzeBaseline(
                        bpmnXml = model.bpmnXml,
                        llmPropsOverride = options.llmProps,
                        useRag = options.useRag ?: false,
                        ragMode = options.ragMode ?: RagMode.HYBRID,
                        activitiesOnly = options.activitiesOnly ?: false
                    )
                    Triple(objectMapper.writeValueAsString(result), result.criticalElements.size, result.amountOfRetries)
                }
                MULTICLASS_ENDPOINT -> {
                    val result = analysisService.analyzeMulticlass(
                        bpmnXml = model.bpmnXml,
                        llmPropsOverride = options.llmProps
                    )
                    Triple(objectMapper.writeValueAsString(result), result.classifiedElements.size, result.amountOfRetries)
                }
                else -> throw IllegalArgumentException("Unknown analysis endpoint '$endpoint'")
            }

            val totalElements = BpmnExtractor().extractBpmnElements(model.bpmnXml).size
            repository.markDone(id, resultJson, criticalCount, totalElements, amountOfRetries)
        } catch (e: Exception) {
            log.error(e) { "Analysis failed for process model $id" }
            repository.markError(id, e.message ?: "Analysis failed")
        }
    }
}
