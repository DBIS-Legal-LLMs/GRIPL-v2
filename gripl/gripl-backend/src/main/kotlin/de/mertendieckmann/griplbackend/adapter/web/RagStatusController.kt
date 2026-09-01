package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.adapter.rag.RagApiClient
import de.mertendieckmann.griplbackend.model.dto.RagStatusResponse
import io.swagger.v3.oas.annotations.Operation
import kotlinx.coroutines.reactor.mono
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/gdpr/rag")
class RagStatusController(
    private val ragApiClient: RagApiClient
) {

    @Operation(
        summary = "Checks whether the GDPR knowledge graph currently holds ingested data",
        description = "Proxies the RAG service's /api/status endpoint so the frontend can warn " +
            "when the knowledge graph is empty and a (re-)ingestion is needed."
    )
    @GetMapping("/status", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getRagStatus(): Mono<RagStatusResponse> = mono { ragApiClient.checkStatus() }
}
