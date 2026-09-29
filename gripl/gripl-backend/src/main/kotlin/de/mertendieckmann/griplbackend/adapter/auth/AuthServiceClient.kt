package de.mertendieckmann.griplbackend.adapter.auth

import com.fasterxml.jackson.annotation.JsonProperty
import de.mertendieckmann.griplbackend.config.AuthServiceProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

/**
 * gripl-backend never holds its own LLM provider key — every analysis is
 * billed to the *calling user's own* OpenRouter account (auth-service#8
 * follow-up). This fetches that key by forwarding the caller's own
 * already-verified bearer token to auth-service's `GET /users/me` —
 * auth-service independently re-verifies the token itself, so no
 * service-to-service secret is needed between the two backends.
 */
@Service
class AuthServiceClient(
    webClientBuilder: WebClient.Builder,
    authServiceProperties: AuthServiceProperties
) {
    private val log = KotlinLogging.logger {}

    private val webClient: WebClient = webClientBuilder
        .baseUrl(authServiceProperties.baseUrl)
        .build()

    private data class AuthServiceProfile(
        @JsonProperty("openrouter_api_key") val openrouterApiKey: String? = null
    )

    /** The caller's stored OpenRouter API key, or null if they haven't set one. */
    suspend fun getOpenRouterApiKey(bearerToken: String): String? {
        val profile = try {
            webClient.get()
                .uri("/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $bearerToken")
                .retrieve()
                .bodyToMono(AuthServiceProfile::class.java)
                .timeout(Duration.ofSeconds(10))
                .awaitSingle()
        } catch (e: Exception) {
            log.error(e) { "Could not reach auth-service to look up the caller's OpenRouter API key" }
            throw RuntimeException("Could not reach auth-service to look up your OpenRouter API key", e)
        }
        return profile.openrouterApiKey
    }
}
