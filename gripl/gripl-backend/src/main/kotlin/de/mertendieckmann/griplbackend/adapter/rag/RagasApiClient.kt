package de.mertendieckmann.griplbackend.adapter.rag

import de.mertendieckmann.griplbackend.config.RagApiProperties
import de.mertendieckmann.griplbackend.model.dto.RagasEvaluationRequest
import de.mertendieckmann.griplbackend.model.dto.RagasEvaluationResponse
import de.mertendieckmann.griplbackend.application.failure.Failure
import de.mertendieckmann.griplbackend.application.failure.FailureCode
import de.mertendieckmann.griplbackend.application.failure.FailureMapper
import de.mertendieckmann.griplbackend.application.failure.ServiceFailureException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.concurrent.TimeoutException
import java.time.Duration


@Service
class RagasApiClient(
    private val webClientBuilder: WebClient.Builder,
    private val ragApiProperties: RagApiProperties
) {

    private val log = KotlinLogging.logger {}

    private val evaluationUrl: String by lazy { "${ragApiProperties.baseUrl}/api/evaluate/ragas" }
    private val webClient: WebClient by lazy {
        webClientBuilder
            .codecs { it.defaultCodecs().maxInMemorySize(32 * 1024 * 1024) }
            .build()
    }

    suspend fun evaluate(request: RagasEvaluationRequest): RagasEvaluationResponse {
        log.info { "Submitting ${request.samples.size} sample(s) to Ragas evaluator at $evaluationUrl" }

        return try {
            webClient.post()
                .uri(evaluationUrl)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RagasEvaluationResponse::class.java)
                .timeout(Duration.ofMinutes(15))
                .awaitSingle()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // logged once, with context, by the caller that decides what the failure means
            val status = (e as? WebClientResponseException)?.statusCode?.value()
            throw ServiceFailureException(
                Failure(
                    FailureCode.RAGAS_UNAVAILABLE,
                    if (status != null) "The RAG quality scoring service answered with status $status."
                    else if (e is TimeoutException) "The RAG quality scoring service did not answer in time."
                    else "The RAG quality scoring service could not be reached.",
                    upstreamStatus = status,
                    upstreamBody = (e as? WebClientResponseException)?.responseBodyAsString?.let { FailureMapper.sanitize(it, 1000) }
                ),
                e
            )
        }
    }
}
