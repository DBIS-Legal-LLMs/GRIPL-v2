package de.mertendieckmann.griplbackend.adapter.rag

import de.mertendieckmann.griplbackend.config.RagApiProperties
import de.mertendieckmann.griplbackend.model.dto.RagMode
import de.mertendieckmann.griplbackend.model.dto.RagRequest
import de.mertendieckmann.griplbackend.model.dto.RagResponseWrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Service
class RagApiClient(
    webClientBuilder: WebClient.Builder,
    ragApiProperties: RagApiProperties
) {

    private val log = KotlinLogging.logger {}

    private val webClient: WebClient = webClientBuilder
        .baseUrl(ragApiProperties.baseUrl)
        // Hybrid-mode responses carry full document chunks and can exceed the 256 KB default.
        .codecs { it.defaultCodecs().maxInMemorySize(32 * 1024 * 1024) }
        .build()

    /**
     * Calls the RAG service and returns a typed response.
     */
    suspend fun queryRag(queryText: String, ragMode: RagMode): RagResponseWrapper {
        log.info { "Querying RAG service with mode=$ragMode" }

        return try {
            webClient.post()
                .uri("/api/query")
                .bodyValue(RagRequest(query = queryText, mode = ragMode))
                .retrieve()
                .bodyToMono(RagResponseWrapper::class.java)
                .timeout(Duration.ofMinutes(10))
                .awaitSingle()
        } catch (e: Exception) {
            log.error(e) { "RAG service call failed for query: $queryText" }
            throw RuntimeException("Failed to query RAG service", e)
        }
    }
}