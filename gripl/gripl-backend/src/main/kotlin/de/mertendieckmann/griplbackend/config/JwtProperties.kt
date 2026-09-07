package de.mertendieckmann.griplbackend.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

/**
 * Points gripl-backend at `auth-service`'s JWKS endpoint. gripl-backend never
 * issues tokens and never holds a shared secret — it fetches `auth-service`'s
 * public signing key(s) from this URL and verifies incoming RS256 JWTs locally
 * against them (see [de.mertendieckmann.griplbackend.security.JwksProvider]).
 *
 * Replaces the old `app.jwt.secret` (a static HS256 secret manually kept in
 * sync with ragulate-backend) — see GRIPL-v2#32.
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Validated
class JwtProperties {
    /**
     * Full URL of `auth-service`'s JWKS document, e.g.
     * `http://localhost:8100/.well-known/jwks.json`.
     */
    @NotBlank
    lateinit var jwksUri: String
}
