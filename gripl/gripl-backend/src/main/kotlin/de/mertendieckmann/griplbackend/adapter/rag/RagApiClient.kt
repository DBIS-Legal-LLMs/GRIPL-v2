package de.mertendieckmann.griplbackend.adapter.rag

import de.mertendieckmann.griplbackend.config.RagApiProperties
import de.mertendieckmann.griplbackend.model.dto.RagMode
import de.mertendieckmann.griplbackend.model.dto.RagRequest
import de.mertendieckmann.griplbackend.model.dto.RagResponseWrapper
import de.mertendieckmann.griplbackend.model.dto.RagStatusResponse
import de.mertendieckmann.griplbackend.application.failure.Failure
import de.mertendieckmann.griplbackend.application.failure.FailureCode
import de.mertendieckmann.griplbackend.application.failure.FailureMapper
import de.mertendieckmann.griplbackend.application.failure.ServiceFailureException
import de.mertendieckmann.griplbackend.application.failure.logFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.concurrent.TimeoutException
import java.time.Duration
import java.util.Collections
import java.util.LinkedHashMap

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
     * Bounded, process-lifetime cache of RAG responses keyed by normalized query text +
     * mode. Element query texts (activity/lane/label combinations) frequently repeat
     * across elements within one analysis and across repeated/similar analyses, and a
     * cache hit skips a full LightRAG retrieval (embedding + graph traversal + keyword
     * extraction) entirely. LRU-evicted via access-order LinkedHashMap; synchronized
     * since queryRag is called concurrently from multiple coroutines.
     */
    private val responseCache: MutableMap<String, RagResponseWrapper> =
        Collections.synchronizedMap(object : LinkedHashMap<String, RagResponseWrapper>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, RagResponseWrapper>): Boolean =
                size > MAX_CACHE_ENTRIES
        })

    /**
     * Calls the RAG service and returns a typed response, reusing a cached response for
     * an identical (mode, normalized query text) pair when available.
     */
    suspend fun queryRag(queryText: String, ragMode: RagMode): RagResponseWrapper {
        val cacheKey = cacheKey(queryText, ragMode)
        responseCache[cacheKey]?.let {
            log.debug { "RAG cache hit for mode=$ragMode" }
            return it
        }

        log.info { "Querying RAG service with mode=$ragMode" }

        val response = try {
            webClient.post()
                .uri("/api/query")
                .bodyValue(RagRequest(query = queryText, mode = ragMode))
                .retrieve()
                .bodyToMono(RagResponseWrapper::class.java)
                .timeout(Duration.ofMinutes(10))
                .awaitSingle()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // the query text is user content and stays out of the log
            throw ragFailure("RAG query", mapOf("mode" to ragMode), e)
        }

        responseCache[cacheKey] = response
        return response
    }

    private fun cacheKey(queryText: String, ragMode: RagMode): String =
        "$ragMode::${queryText.trim().lowercase()}"

    /**
     * Checks whether the RAG service's knowledge graph currently holds any ingested
     * data. Not cached — always hits the RAG service live so it reflects the current
     * state (e.g. after a re-ingestion run).
     */
    suspend fun checkStatus(): RagStatusResponse {
        return try {
            webClient.get()
                .uri("/api/status")
                .retrieve()
                .bodyToMono(RagStatusResponse::class.java)
                .timeout(Duration.ofSeconds(10))
                .awaitSingle()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ragFailure("RAG status check", emptyMap(), e)
        }
    }

    private fun ragFailure(operation: String, context: Map<String, Any?>, e: Exception): ServiceFailureException {
        val status = (e as? WebClientResponseException)?.statusCode?.value()
        val failure = Failure(
            FailureCode.RAG_UNAVAILABLE,
            if (status != null) "The GDPR knowledge graph service answered with status $status."
            else if (e is TimeoutException) "The GDPR knowledge graph service did not answer in time."
            else "The GDPR knowledge graph service could not be reached.",
            upstreamStatus = status,
            upstreamBody = (e as? WebClientResponseException)?.responseBodyAsString?.let { FailureMapper.sanitize(it, 1000) }
        )
        val serviceFailure = ServiceFailureException(failure, e)
        log.logFailure(operation, serviceFailure, context)
        return serviceFailure
    }

    companion object {
        private const val MAX_CACHE_ENTRIES = 500
    }
}