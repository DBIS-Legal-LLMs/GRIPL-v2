package de.mertendieckmann.griplbackend.security

import de.mertendieckmann.griplbackend.config.JwtProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.security.Jwks
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.PublicKey
import java.time.Duration

/**
 * Fetches and caches `auth-service`'s JWKS (`/.well-known/jwks.json`) and hands
 * out the RSA public key for a given `kid`.
 *
 * Mirrors RAGulate's `core/jwt_verification.py` (the other consumer of the same
 * `auth-service`): the key set is cached for [CACHE_TTL] and re-fetched on
 * expiry, and an unknown `kid` triggers one immediate forced refresh before
 * giving up — so a signing-key rotation on `auth-service` is picked up without
 * a restart here.
 *
 * All methods block on network IO and must be called off the Netty event loop
 * (see [JwtAuthenticationWebFilter], which runs verification on a bounded
 * elastic scheduler).
 */
@Component
class JwksProvider(
    private val jwtProperties: JwtProperties,
) {
    private val log = KotlinLogging.logger {}

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    private val lock = Any()

    @Volatile
    private var keysByKid: Map<String, PublicKey> = emptyMap()

    @Volatile
    private var lastRefreshEpochMs: Long = 0L

    fun publicKey(kid: String): PublicKey {
        val cached = keysByKid[kid]
        if (cached != null && isFresh()) return cached

        synchronized(lock) {
            // Re-check: another thread may have refreshed while we waited.
            keysByKid[kid]?.let { if (isFresh()) return it }

            val refreshed = fetchKeys()
            keysByKid = refreshed
            lastRefreshEpochMs = System.currentTimeMillis()

            return refreshed[kid]
                ?: throw JwtException("auth-service JWKS has no key for kid=$kid")
        }
    }

    private fun isFresh(): Boolean =
        System.currentTimeMillis() - lastRefreshEpochMs < CACHE_TTL.toMillis()

    private fun fetchKeys(): Map<String, PublicKey> {
        val uri = jwtProperties.jwksUri
        val request = HttpRequest.newBuilder(URI.create(uri))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build()

        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            throw JwtException("Could not reach auth-service JWKS at $uri", e)
        }

        if (response.statusCode() !in 200..299) {
            throw JwtException("auth-service JWKS fetch failed: HTTP ${response.statusCode()} from $uri")
        }

        val jwkSet = try {
            Jwks.setParser().build().parse(response.body())
        } catch (e: Exception) {
            throw JwtException("auth-service JWKS at $uri is not a valid key set", e)
        }

        val keys = HashMap<String, PublicKey>()
        for (jwk in jwkSet) {
            val kid = jwk.id ?: continue
            val key = jwk.toKey()
            if (key is PublicKey) {
                keys[kid] = key
            }
        }

        if (keys.isEmpty()) {
            throw JwtException("auth-service JWKS at $uri contained no usable keys")
        }
        log.info { "Loaded ${keys.size} signing key(s) from auth-service JWKS ($uri)" }
        return keys
    }

    private companion object {
        val CACHE_TTL: Duration = Duration.ofMinutes(5)
    }
}
