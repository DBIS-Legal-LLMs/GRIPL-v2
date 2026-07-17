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
}
