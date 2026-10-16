package de.mertendieckmann.griplbackend.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

/**
 * Base URL of `auth-service`, with no endpoint path — used to fetch a caller's
 * own profile (specifically their OpenRouter API key, see [AuthServiceClient])
 * by forwarding the caller's own already-verified bearer token.
 *
 * Separate from [JwtProperties] (which points at the JWKS document
 * specifically) since this is a plain base URL other endpoints get appended to.
 */
@Component
@ConfigurationProperties(prefix = "app.auth-service")
@Validated
class AuthServiceProperties {
    @NotBlank
    lateinit var baseUrl: String
}
