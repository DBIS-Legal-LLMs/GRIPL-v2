package de.mertendieckmann.griplbackend.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

/**
 * Signing secret shared with ragulate-backend, which is the only service that
 * issues tokens. This service only ever verifies signatures — it must be the
 * exact same secret (min. 32 bytes for HS256) configured on ragulate-backend.
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Validated
class JwtProperties {
    @NotBlank
    lateinit var secret: String
}
