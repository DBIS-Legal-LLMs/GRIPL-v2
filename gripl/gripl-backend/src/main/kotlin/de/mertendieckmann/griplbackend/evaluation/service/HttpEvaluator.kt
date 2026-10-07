package de.mertendieckmann.griplbackend.evaluation.service

import de.mertendieckmann.griplbackend.application.analyzer.AnalysisService
import de.mertendieckmann.griplbackend.model.dto.AnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.CustomAnalysisResponseType
import de.mertendieckmann.griplbackend.model.dto.EvaluationRequest
import de.mertendieckmann.griplbackend.model.dto.ExpectedValue
import de.mertendieckmann.griplbackend.model.dto.MulticlassAnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.RagMode
import de.mertendieckmann.griplbackend.repository.CustomAnalysisEndpointRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.time.Duration.Companion.minutes

private const val PROMPT_ENGINEERING_ENDPOINT = "/gdpr/analysis/prompt-engineering"
private const val MULTICLASS_ENDPOINT = "/gdpr/analysis/multiclass"
private const val CUSTOM_ENDPOINT_PREFIX = "/gdpr/analysis/custom/"

/**
 * Runs one evaluation test case against an analysis endpoint.
 *
 * Endpoints of this app (`/gdpr/analysis/...`) are run **in-process** through [AnalysisService] —
 * the same code the HTTP controllers call. They used to be reached by an HTTP request from the
 * backend back to itself, which carried no login token and so was rejected by the JWT filter
 * (every test case failed with an empty 401), and a forwarded user token would have expired
 * mid-run on long evaluations. Only absolute `http(s)://` endpoints (external services) still go
 * over HTTP.
 */
@Service
class HttpEvaluator(
    private val customAnalysisEndpointRepository: CustomAnalysisEndpointRepository,
    private val analysisService: AnalysisService
) : Evaluator {

    companion object {
        private val EVALUATION_CALL_TIMEOUT = 15.minutes
    }

    private val webClient = WebClient.builder()
        .codecs {
            it.defaultCodecs()
                .maxInMemorySize(32 * 1024 * 1024)
        }
        .build()

    override suspend fun evaluate(
        bpmnXml: String,
        evaluationRequest: EvaluationRequest
    ): EvaluationCallResult {
        val endpoint = evaluationRequest.evaluationEndpoint
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return evaluateOverHttp(bpmnXml, evaluationRequest, endpoint)
        }

        try {
            return withTimeout(EVALUATION_CALL_TIMEOUT) {
                // AnalysisService is blocking (JDBC, LLM and RAG calls).
                withContext(Dispatchers.IO) { evaluateInProcess(bpmnXml, evaluationRequest) }
            }
        } catch (e: TimeoutCancellationException) {
            throw RuntimeException(
                "Evaluation call to endpoint '$endpoint' timed out after $EVALUATION_CALL_TIMEOUT",
                e
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException("Failed to evaluate BPMN XML at endpoint '$endpoint': ${e.message}", e)
        }
    }

    /**
     * Mirrors what the `/gdpr/analysis/...` controllers do, minus the HTTP hop. Custom endpoints'
     * RAG settings are a fixed property of the endpoint itself, not of the request.
     */
    private fun evaluateInProcess(bpmnXml: String, request: EvaluationRequest): EvaluationCallResult {
        val endpoint = request.evaluationEndpoint
        return when {
            endpoint == PROMPT_ENGINEERING_ENDPOINT -> binaryResult(
                analysisService.analyzePromptEngineering(
                    bpmnXml = bpmnXml,
                    llmPropsOverride = request.llmProps,
                    useRag = request.useRag,
                    ragMode = request.ragMode,
                    activitiesOnly = request.activitiesOnly
                )
            )
            endpoint == MULTICLASS_ENDPOINT -> multiclassResult(
                analysisService.analyzeMulticlass(
                    bpmnXml = bpmnXml,
                    llmPropsOverride = request.llmProps,
                    useRag = request.useRag,
                    ragMode = request.ragMode,
                    activitiesOnly = request.activitiesOnly
                )
            )
            endpoint.startsWith(CUSTOM_ENDPOINT_PREFIX) -> {
                val id = endpoint.removePrefix(CUSTOM_ENDPOINT_PREFIX).toLongOrNull()
                    ?: throw IllegalArgumentException("Malformed custom analysis endpoint '$endpoint'")
                // Ownership was already checked where the evaluation request came in
                // (EvaluationController); this runs with no user context.
                val custom = customAnalysisEndpointRepository.getById(id)
                    ?: throw IllegalArgumentException("No custom analysis endpoint found for id $id")
                when (custom.responseType) {
                    CustomAnalysisResponseType.BINARY -> binaryResult(
                        analysisService.analyzeCustomBinary(
                            bpmnXml = bpmnXml,
                            promptText = custom.promptText,
                            llmPropsOverride = request.llmProps,
                            useRag = custom.ragEnabled,
                            ragMode = custom.ragMode ?: RagMode.HYBRID,
                            activitiesOnly = request.activitiesOnly
                        )
                    )
                    CustomAnalysisResponseType.MULTICLASS -> multiclassResult(
                        analysisService.analyzeCustomMulticlass(
                            bpmnXml = bpmnXml,
                            promptText = custom.promptText,
                            llmPropsOverride = request.llmProps,
                            useRag = custom.ragEnabled,
                            ragMode = custom.ragMode ?: RagMode.HYBRID,
                            activitiesOnly = request.activitiesOnly
                        )
                    )
                }
            }
            else -> throw IllegalArgumentException("Unknown analysis endpoint '$endpoint'")
        }
    }

    private fun binaryResult(response: AnalysisResponse) = EvaluationCallResult(
        expectedValues = response.criticalElements.map { ExpectedValue(value = it.id, reason = it.reason) },
        amountOfRetries = response.amountOfRetries,
        analysisResponse = response
    )

    private fun multiclassResult(response: MulticlassAnalysisResponse) = EvaluationCallResult(
        expectedValues = response.classifiedElements.map {
            ExpectedValue(value = it.id, reason = it.reason, classification = it.classification)
        },
        amountOfRetries = response.amountOfRetries,
        // Shaped like a binary response so downstream scoring needs only one form.
        analysisResponse = AnalysisResponse(
            criticalElements = response.classifiedElements.map {
                AnalysisResponse.CriticalElement(id = it.id, name = it.name, reason = it.reason)
            },
            amountOfRetries = response.amountOfRetries
        )
    )

    /** External `http(s)://` endpoints only — this app's own endpoints never go over HTTP. */
    private suspend fun evaluateOverHttp(
        bpmnXml: String,
        evaluationRequest: EvaluationRequest,
        absoluteEndpoint: String
    ): EvaluationCallResult {
        val bodyBuilder = MultipartBodyBuilder()

        bodyBuilder.part("bpmnFile", ByteArrayResource(bpmnXml.toByteArray()))
            .header("Content-Disposition", "form-data; name=\"bpmnFile\"; filename=\"process.bpmn\"")
            .contentType(MediaType.APPLICATION_XML)

        evaluationRequest.llmProps?.let { overrides ->
            bodyBuilder.part("llmProps", jacksonObjectMapper().writeValueAsString(overrides))
                .header("Content-Disposition", "form-data; name=\"llmProps\"")
                .contentType(MediaType.APPLICATION_JSON)
        }

        bodyBuilder.part("useRag", evaluationRequest.useRag.toString())
        bodyBuilder.part("ragMode", evaluationRequest.ragMode.toString())
        bodyBuilder.part("activitiesOnly", evaluationRequest.activitiesOnly.toString())

        try {
            return withTimeout(EVALUATION_CALL_TIMEOUT) {
                // External endpoints are assumed binary: there's no registry to ask.
                val analysisResponse: AnalysisResponse = webClient
                    .post()
                    .uri(absoluteEndpoint)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .awaitBody()
                binaryResult(analysisResponse)
            }
        } catch (e: TimeoutCancellationException) {
            throw RuntimeException(
                "Evaluation call to endpoint '$absoluteEndpoint' timed out after $EVALUATION_CALL_TIMEOUT",
                e
            )
        } catch (e: WebClientResponseException) {
            throw RuntimeException(
                "Failed to evaluate BPMN XML at endpoint '$absoluteEndpoint': " + e.responseBodyAsString,
                e
            )
        }
    }
}
