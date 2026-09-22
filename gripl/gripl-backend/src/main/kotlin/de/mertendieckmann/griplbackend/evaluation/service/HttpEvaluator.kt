package de.mertendieckmann.griplbackend.evaluation.service

import de.mertendieckmann.griplbackend.model.dto.AnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.CustomAnalysisResponseType
import de.mertendieckmann.griplbackend.model.dto.EvaluationRequest
import de.mertendieckmann.griplbackend.model.dto.ExpectedValue
import de.mertendieckmann.griplbackend.model.dto.MulticlassAnalysisResponse
import de.mertendieckmann.griplbackend.repository.CustomAnalysisEndpointRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.springframework.beans.factory.annotation.Value
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

private const val MULTICLASS_ENDPOINT = "/gdpr/analysis/multiclass"
private const val CUSTOM_ENDPOINT_PREFIX = "/gdpr/analysis/custom/"

@Service
class HttpEvaluator(
    @Value("\${server.port:8080}") private val serverPort: Int,
    private val customAnalysisEndpointRepository: CustomAnalysisEndpointRepository
) : Evaluator {

    companion object {
        private val EVALUATION_CALL_TIMEOUT = 15.minutes
    }

    /**
     * Whether [endpoint] produces a multiclass response, resolved from the same source of truth
     * the endpoint dispatch itself uses — not by guessing from the URL string (a literal
     * "multiclass" substring check would silently misclassify every custom endpoint, whose URL is
     * just `/gdpr/analysis/custom/{id}`, as binary regardless of how it was actually configured).
     */
    private fun isMulticlassEndpoint(endpoint: String): Boolean {
        if (endpoint == MULTICLASS_ENDPOINT) return true
        if (endpoint.startsWith(CUSTOM_ENDPOINT_PREFIX)) {
            val id = endpoint.removePrefix(CUSTOM_ENDPOINT_PREFIX).toLongOrNull() ?: return false
            return customAnalysisEndpointRepository.getById(id)?.responseType == CustomAnalysisResponseType.MULTICLASS
        }
        return false
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
        val bodyBuilder = MultipartBodyBuilder()

        bodyBuilder.part(
            "bpmnFile",
            ByteArrayResource(bpmnXml.toByteArray())
        )
            .header(
                "Content-Disposition",
                "form-data; name=\"bpmnFile\"; filename=\"process.bpmn\""
            )
            .contentType(MediaType.APPLICATION_XML)

        evaluationRequest.llmProps?.let { overrides ->
            bodyBuilder.part(
                "llmProps",
                jacksonObjectMapper().writeValueAsString(overrides)
            )
                .header(
                    "Content-Disposition",
                    "form-data; name=\"llmProps\""
                )
                .contentType(MediaType.APPLICATION_JSON)
        }

        bodyBuilder.part(
            "useRag",
            evaluationRequest.useRag.toString()
        )
        bodyBuilder.part(
            "ragMode",
            evaluationRequest.ragMode.toString()
        )
        bodyBuilder.part(
            "activitiesOnly",
            evaluationRequest.activitiesOnly.toString()
        )

        val absoluteEndpoint =
            if (
                evaluationRequest.evaluationEndpoint
                    .startsWith("http://") ||
                evaluationRequest.evaluationEndpoint
                    .startsWith("https://")
            ) {
                evaluationRequest.evaluationEndpoint
            } else {
                "http://localhost:$serverPort" +
                    evaluationRequest.evaluationEndpoint
            }

        try {
            return withTimeout(EVALUATION_CALL_TIMEOUT) {
                if (isMulticlassEndpoint(evaluationRequest.evaluationEndpoint)) {
                    val multiclassResponse:
                        MulticlassAnalysisResponse =
                        webClient
                            .post()
                            .uri(absoluteEndpoint)
                            .contentType(
                                MediaType.MULTIPART_FORM_DATA
                            )
                            .body(
                                BodyInserters.fromMultipartData(
                                    bodyBuilder.build()
                                )
                            )
                            .retrieve()
                            .awaitBody()

                    val expectedValues =
                        multiclassResponse.classifiedElements.map {
                            ExpectedValue(
                                value = it.id,
                                reason = it.reason,
                                classification = it.classification
                            )
                        }

                    val compatibleAnalysisResponse =
                        AnalysisResponse(
                            criticalElements =
                                multiclassResponse
                                    .classifiedElements
                                    .map {
                                        AnalysisResponse.CriticalElement(
                                            id = it.id,
                                            name = it.name,
                                            reason = it.reason
                                        )
                                    },
                            amountOfRetries =
                                multiclassResponse.amountOfRetries
                        )

                    EvaluationCallResult(
                        expectedValues = expectedValues,
                        amountOfRetries =
                            multiclassResponse.amountOfRetries,
                        analysisResponse =
                            compatibleAnalysisResponse
                    )
                } else {
                    val analysisResponse: AnalysisResponse =
                        webClient
                            .post()
                            .uri(absoluteEndpoint)
                            .contentType(
                                MediaType.MULTIPART_FORM_DATA
                            )
                            .body(
                                BodyInserters.fromMultipartData(
                                    bodyBuilder.build()
                                )
                            )
                            .retrieve()
                            .awaitBody()

                    EvaluationCallResult(
                        expectedValues =
                            analysisResponse.criticalElements.map {
                                ExpectedValue(
                                    value = it.id,
                                    reason = it.reason
                                )
                            },
                        amountOfRetries =
                            analysisResponse.amountOfRetries,
                        analysisResponse = analysisResponse
                    )
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw RuntimeException(
                "Evaluation call to endpoint '$absoluteEndpoint' timed out after $EVALUATION_CALL_TIMEOUT",
                e
            )
        } catch (e: WebClientResponseException) {
            throw RuntimeException(
                "Failed to evaluate BPMN XML at endpoint " +
                    "'$absoluteEndpoint': " +
                    e.responseBodyAsString,
                e
            )
        }
    }
}