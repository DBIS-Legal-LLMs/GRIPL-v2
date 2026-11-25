package de.mertendieckmann.griplbackend.application

import de.mertendieckmann.griplbackend.application.analyzer.AnalysisService
import de.mertendieckmann.griplbackend.application.failure.Failure
import de.mertendieckmann.griplbackend.application.failure.FailureCode
import de.mertendieckmann.griplbackend.application.failure.ServiceFailureException
import de.mertendieckmann.griplbackend.application.failure.logFailure
import de.mertendieckmann.griplbackend.config.LlmConfig
import de.mertendieckmann.griplbackend.model.dto.CustomAnalysisResponseType
import de.mertendieckmann.griplbackend.model.dto.EnqueueAnalysisRequest
import de.mertendieckmann.griplbackend.model.dto.ProcessModelAnalysisOptions
import de.mertendieckmann.griplbackend.model.dto.ProcessModelStatus
import de.mertendieckmann.griplbackend.model.dto.RagMode
import de.mertendieckmann.griplbackend.repository.CustomAnalysisEndpointRepository
import de.mertendieckmann.griplbackend.repository.ProcessModelRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

internal const val INTERRUPTED_MESSAGE = "Interrupted by application restart, queue the analysis again"
private const val PROMPT_ENGINEERING_ENDPOINT = "/gdpr/analysis/prompt-engineering"
private const val MULTICLASS_ENDPOINT = "/gdpr/analysis/multiclass"
private const val CUSTOM_ENDPOINT_PREFIX = "/gdpr/analysis/custom/"

/**
 * Runs process-model analyses one at a time on a single background thread.
 * Postgres (process_model.status) is the durable source of truth; the in-memory
 * queue only ever carries ids, never parameters. The caller's OpenRouter API key
 * is the exception to "persist the parameters": it lives only in memory
 * ([apiKeys]) and is never written to analysis_options, so a job that is still
 * QUEUED or RUNNING when the application restarts is set to ERROR and has to be
 * queued again by the user.
 */
@Service
class ProcessModelJobRunner(
    private val repository: ProcessModelRepository,
    private val analysisService: AnalysisService,
    private val customAnalysisEndpointRepository: CustomAnalysisEndpointRepository
) {
    private val log = KotlinLogging.logger { }
    private val objectMapper = jacksonObjectMapper()
    private val queue = LinkedBlockingQueue<Long>()
    private val inFlight = mutableSetOf<Long>()
    /** The caller's OpenRouter key per queued process model id; removed when the job ends. */
    private val apiKeys = ConcurrentHashMap<Long, String>()
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "process-model-job-runner").apply { isDaemon = true }
    }

    init {
        executor.submit { workerLoop() }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun recoverOnStartup() {
        // The queue and the API keys were in memory only, so nothing that was queued or running can continue.
        val resetCount = repository.resetInterruptedToError(INTERRUPTED_MESSAGE)
        if (resetCount > 0) {
            log.warn { "Reset $resetCount queued or running process model(s) to ERROR after restart" }
        }
    }

    fun enqueue(request: EnqueueAnalysisRequest): List<Long> {
        val options = ProcessModelAnalysisOptions(
            llmProps = request.llmProps?.copy(apiKey = null),
            useRag = request.useRag,
            ragMode = request.ragMode,
            activitiesOnly = request.activitiesOnly
        )
        val optionsJson = objectMapper.writeValueAsString(options)
        val apiKey = request.llmProps?.apiKey?.takeIf { it.isNotBlank() }

        val enqueued = mutableListOf<Long>()
        synchronized(inFlight) {
            request.ids.forEach { id ->
                if (inFlight.add(id)) {
                    apiKey?.let { apiKeys[id] = it }
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
                val failure = log.logFailure("Processing process model", e, mapOf("processModelId" to id))
                repository.markError(id, failure.message)
            } finally {
                apiKeys.remove(id)
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
        val llmProps = (options.llmProps ?: LlmConfig.Companion.LlmPropsOverride()).copy(apiKey = apiKeys[id])
        val endpoint = model.analysisEndpoint ?: PROMPT_ENGINEERING_ENDPOINT

        try {
            val (resultJson, criticalCount, amountOfRetries) = when (endpoint) {
                PROMPT_ENGINEERING_ENDPOINT -> {
                    val result = analysisService.analyzePromptEngineering(
                        bpmnXml = model.bpmnXml,
                        llmPropsOverride = llmProps,
                        useRag = options.useRag ?: false,
                        ragMode = options.ragMode ?: RagMode.HYBRID,
                        activitiesOnly = options.activitiesOnly ?: false
                    )
                    Triple(objectMapper.writeValueAsString(result), result.criticalElements.size, result.amountOfRetries)
                }
                MULTICLASS_ENDPOINT -> {
                    val result = analysisService.analyzeMulticlass(
                        bpmnXml = model.bpmnXml,
                        llmPropsOverride = llmProps,
                        useRag = options.useRag ?: false,
                        ragMode = options.ragMode ?: RagMode.HYBRID,
                        activitiesOnly = options.activitiesOnly ?: false
                    )
                    Triple(objectMapper.writeValueAsString(result), result.classifiedElements.size, result.amountOfRetries)
                }
                else -> if (endpoint.startsWith(CUSTOM_ENDPOINT_PREFIX)) {
                    val customEndpointId = endpoint.removePrefix(CUSTOM_ENDPOINT_PREFIX).toLongOrNull()
                        ?: throw invalidEndpoint("Malformed custom analysis endpoint '$endpoint'")
                    val customEndpoint = customAnalysisEndpointRepository.getById(customEndpointId)
                        ?: throw invalidEndpoint("Custom analysis endpoint $customEndpointId no longer exists")

                    // RAG usage is a fixed property of the custom endpoint itself (like responseType),
                    // not something re-picked per analysis run.
                    when (customEndpoint.responseType) {
                        CustomAnalysisResponseType.BINARY -> {
                            val result = analysisService.analyzeCustomBinary(
                                bpmnXml = model.bpmnXml,
                                promptText = customEndpoint.promptText,
                                llmPropsOverride = llmProps,
                                useRag = customEndpoint.ragEnabled,
                                ragMode = customEndpoint.ragMode ?: RagMode.HYBRID,
                                activitiesOnly = options.activitiesOnly ?: false
                            )
                            Triple(objectMapper.writeValueAsString(result), result.criticalElements.size, result.amountOfRetries)
                        }
                        CustomAnalysisResponseType.MULTICLASS -> {
                            val result = analysisService.analyzeCustomMulticlass(
                                bpmnXml = model.bpmnXml,
                                promptText = customEndpoint.promptText,
                                llmPropsOverride = llmProps,
                                useRag = customEndpoint.ragEnabled,
                                ragMode = customEndpoint.ragMode ?: RagMode.HYBRID,
                                activitiesOnly = options.activitiesOnly ?: false
                            )
                            Triple(objectMapper.writeValueAsString(result), result.classifiedElements.size, result.amountOfRetries)
                        }
                    }
                } else throw invalidEndpoint("Unknown analysis endpoint '$endpoint'")
            }

            val totalElements = BpmnExtractor().extractBpmnElements(model.bpmnXml).size
            repository.markDone(id, model.bpmnXml, resultJson, criticalCount, totalElements, amountOfRetries)
        } catch (e: Exception) {
            // The message is stored with the process model and shown to the user, so it comes from FailureMapper, not from e.message
            val failure = log.logFailure(
                "Analysis",
                e,
                mapOf("processModelId" to id, "endpoint" to endpoint, "model" to llmProps.modelName)
            )
            repository.markError(id, failure.message)
        }
    }

    private fun invalidEndpoint(message: String) =
        ServiceFailureException(Failure(FailureCode.ANALYSIS_ENDPOINT_INVALID, message))
}
