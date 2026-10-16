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

/**
 * Key under which the caller's resolved GRIPL role (JWT `app_roles.gripl`,
 * e.g. `"admin"`, `"researcher"`, `"dpo"`, `"end-user"`) is stored on the
 * exchange, when present. auth-service embeds a fully-resolved role for every
 * app it knows about (GRIPL-v2#5/#6/#7 on that side) — absent means either the
 * token predates GRIPL being registered there, or auth-service doesn't have
 * GRIPL registered at all. See [de.mertendieckmann.griplbackend.security.requireGriplRole]
 * (GRIPL-v2#40).
 */
const val AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE = "griplRole"

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
        return Mono.fromCallable { verifyAndExtractIdentity(token) }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { identity ->
                exchange.attributes[AUTHENTICATED_USER_ID_ATTRIBUTE] = identity.userId
                identity.griplRole?.let { exchange.attributes[AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE] = it }
                chain.filter(exchange)
            }
            .onErrorResume { e ->
                log.debug(e) { "Rejecting request to $path: JWT verification failed" }
                unauthorized(exchange)
            }
    }

    private data class AuthenticatedIdentity(val userId: String, val griplRole: String?)

    private fun verifyAndExtractIdentity(token: String): AuthenticatedIdentity {
        val claims = Jwts.parser()
            .keyLocator { header ->
                val kid = (header as? ProtectedHeader)?.keyId
                    ?: throw JwtException("JWT is missing the 'kid' header")
                jwksProvider.publicKey(kid)
            }
            .clockSkewSeconds(30)
            .build()
            .parseSignedClaims(token)
            .payload

        val subject = claims.subject?.takeIf { it.isNotBlank() }
            ?: throw JwtException("JWT is missing the 'sub' claim")

        @Suppress("UNCHECKED_CAST")
        val griplRole = (claims["app_roles"] as? Map<String, String>)?.get("gripl")

        return AuthenticatedIdentity(subject, griplRole)
    }

    private fun unauthorized(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        return exchange.response.setComplete()
    }
}
