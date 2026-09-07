package de.mertendieckmann.griplbackend.security

import io.github.oshai.kotlinlogging.KotlinLogging
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.ProtectedHeader
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/** Key under which the authenticated user's id (JWT `sub`) is stored on the exchange. */
const val AUTHENTICATED_USER_ID_ATTRIBUTE = "userId"

private val PUBLIC_PATH_PREFIXES = listOf(
    "/actuator/health",
    "/swagger-ui",
    "/v3/api-docs",
    "/thesis/pdf",
)

/**
 * Verifies JWTs issued by `auth-service` (RS256, verified against its published
 * JWKS — see [JwksProvider]). This is pure signature/expiry verification — no
 * database lookup — since identity is owned entirely by `auth-service`.
 *
 * Replaces the previous static-secret HS256 check (GRIPL-v2#32): gripl-backend
 * no longer shares a secret with any other service.
 */
@Component
class JwtAuthenticationWebFilter(
    private val jwksProvider: JwksProvider,
) : WebFilter {

    private val log = KotlinLogging.logger {}

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path.value()
        if (PUBLIC_PATH_PREFIXES.any { path.startsWith(it) }) {
            return chain.filter(exchange)
        }

        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        val token = authHeader?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")?.trim()
            ?: return unauthorized(exchange)

        // JWKS resolution / verification can touch the network (cache miss or
        // key rotation) — keep it off the event loop.
        return Mono.fromCallable { verifyAndExtractSubject(token) }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { userId ->
                exchange.attributes[AUTHENTICATED_USER_ID_ATTRIBUTE] = userId
                chain.filter(exchange)
            }
            .onErrorResume { e ->
                log.debug(e) { "Rejecting request to $path: JWT verification failed" }
                unauthorized(exchange)
            }
    }

    private fun verifyAndExtractSubject(token: String): String {
        val subject = Jwts.parser()
            .keyLocator { header ->
                val kid = (header as? ProtectedHeader)?.keyId
                    ?: throw JwtException("JWT is missing the 'kid' header")
                jwksProvider.publicKey(kid)
            }
            .clockSkewSeconds(30)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject

        return subject?.takeIf { it.isNotBlank() }
            ?: throw JwtException("JWT is missing the 'sub' claim")
    }

    private fun unauthorized(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        return exchange.response.setComplete()
    }
}
