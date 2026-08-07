        package de.mertendieckmann.griplbackend.evaluation.service

        import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
        import de.mertendieckmann.griplbackend.model.dto.AnalysisResponse
        import de.mertendieckmann.griplbackend.model.dto.EvaluationRequest
        import de.mertendieckmann.griplbackend.model.dto.ModelRunConfig
        import de.mertendieckmann.griplbackend.model.dto.ExpectedValue
        import org.springframework.beans.factory.annotation.Value
        import org.springframework.core.io.ByteArrayResource
        import org.springframework.http.MediaType
        import org.springframework.http.client.MultipartBodyBuilder
        import org.springframework.stereotype.Service
        import org.springframework.web.reactive.function.BodyInserters
        import org.springframework.web.reactive.function.client.WebClient
        import org.springframework.web.reactive.function.client.WebClientResponseException
        import org.springframework.web.reactive.function.client.awaitBody
        import de.mertendieckmann.griplbackend.model.dto.MulticlassAnalysisResponse

        @Service
        class HttpEvaluator(
            @Value("\${server.port:8080}") private val serverPort: Int
        ): Evaluator {

            private val webClient = WebClient.builder().build()

            override suspend fun evaluate(bpmnXml: String, evaluationRequest: EvaluationRequest): Pair<List<ExpectedValue>, Int?> {
                val bodyBuilder = MultipartBodyBuilder()
                bodyBuilder.part("bpmnFile", ByteArrayResource(bpmnXml.toByteArray()))
                    .header("Content-Disposition", "form-data; name=\"bpmnFile\"; filename=\"process.bpmn\"")
                    .contentType(MediaType.APPLICATION_XML)
                evaluationRequest.llmProps?.let { overrides ->
                    bodyBuilder.part("llmProps", jacksonObjectMapper().writeValueAsString(overrides))
                        .header("Content-Disposition", "form-data; name=\"llmProps\"")
                        .contentType(MediaType.APPLICATION_JSON)
                }

                val absoluteEndpoint = if (evaluationRequest.evaluationEndpoint.startsWith("http://") || evaluationRequest.evaluationEndpoint.startsWith("https://")) {
                    evaluationRequest.evaluationEndpoint
                } else {
                    "http://localhost:$serverPort${evaluationRequest.evaluationEndpoint}"
                }

                try {
                return if (evaluationRequest.evaluationEndpoint.contains("multiclass", ignoreCase = true)) {
                    val analysisResponse: MulticlassAnalysisResponse = webClient
                        .post()
                        .uri(absoluteEndpoint)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                        .retrieve()
                        .awaitBody()

                    Pair(
                        analysisResponse.classifiedElements.map {
                            ExpectedValue(
                                value = it.id,
                                reason = it.reason,
                                classification = it.classification
                            )
                        },
                        analysisResponse.amountOfRetries
                    )
                } else {
                    val analysisResponse: AnalysisResponse = webClient
                        .post()
                        .uri(absoluteEndpoint)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                        .retrieve()
                        .awaitBody()

                    Pair(
                        analysisResponse.criticalElements.map {
                            ExpectedValue(
                                value = it.id,
                                reason = it.reason
                            )
                        },
                        analysisResponse.amountOfRetries
                    )
                }
            } catch (e: WebClientResponseException) {
                throw RuntimeException(
                    "Failed to evaluate BPMN XML at endpoint '$absoluteEndpoint': ${e.responseBodyAsString}",
                    e
                )
            }
            }
        }