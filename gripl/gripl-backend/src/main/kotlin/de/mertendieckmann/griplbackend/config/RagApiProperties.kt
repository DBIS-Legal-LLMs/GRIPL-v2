package de.mertendieckmann.griplbackend.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@ConfigurationProperties(prefix = "rag.api")
@Validated
class RagApiProperties {
    @NotBlank
    lateinit var url: String

    /**
     * The RAG service base URL, with no endpoint path. Accepts either a plain
     * base URL (e.g. http://gripl-rag:8081) or a legacy value that includes
     * the query endpoint path (e.g. http://gripl-rag:8081/api/query).
     */
    val baseUrl: String
        get() = url.trimEnd('/').removeSuffix("/query").removeSuffix("/api")

    /**
     * Max number of concurrent /api/query calls fired per analysis. The RAG
     * service currently only calls out to hosted LLM/embedding APIs (no local
     * CPU models), so this is I/O-bound and can run higher than a CPU-bound
     * default would allow — tune down if the upstream LLM/embedding provider
     * starts rate-limiting.
     */
    var maxConcurrency: Int = 16
}
