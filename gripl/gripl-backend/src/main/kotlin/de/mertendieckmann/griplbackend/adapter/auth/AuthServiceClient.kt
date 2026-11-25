package de.mertendieckmann.griplbackend.adapter.auth

import com.fasterxml.jackson.annotation.JsonProperty
import de.mertendieckmann.griplbackend.config.AuthServiceProperties
import de.mertendieckmann.griplbackend.application.failure.Failure
import de.mertendieckmann.griplbackend.application.failure.FailureCode
import de.mertendieckmann.griplbackend.application.failure.FailureMapper
import de.mertendieckmann.griplbackend.application.failure.ServiceFailureException
import de.mertendieckmann.griplbackend.application.failure.logFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val status = (e as? WebClientResponseException)?.statusCode?.value()
            val failure = if (status == 401 || status == 403) {
                Failure(
                    FailureCode.AUTH_SERVICE_REJECTED_TOKEN,
                    "The login service did not accept your session. Please sign in again.",
                    upstreamStatus = status
                )
            } else {
                Failure(
                    FailureCode.AUTH_SERVICE_UNAVAILABLE,
                    "The login service could not be reached to look up your OpenRouter API key. Please try again.",
                    upstreamStatus = status,
                    upstreamBody = (e as? WebClientResponseException)?.responseBodyAsString?.let { FailureMapper.sanitize(it, 1000) }
                )
            }
            val serviceFailure = ServiceFailureException(failure, e)
            log.logFailure("Look up of the caller's OpenRouter API key", serviceFailure)
            throw serviceFailure
        }
        return profile.openrouterApiKey
    }
}
