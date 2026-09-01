package de.mertendieckmann.griplbackend.evaluation.service

import de.mertendieckmann.griplbackend.model.dto.AnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.EvaluationRequest
import de.mertendieckmann.griplbackend.model.dto.ExpectedValue
import de.mertendieckmann.griplbackend.model.dto.MulticlassAnalysisResponse
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

@Service
class HttpEvaluator(
    @Value("\${server.port:8080}") private val serverPort: Int
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
                if (
                    evaluationRequest.evaluationEndpoint.contains(
                        "multiclass",
                        ignoreCase = true
                    )
                ) {
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