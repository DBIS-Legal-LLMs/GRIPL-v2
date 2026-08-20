package de.mertendieckmann.griplbackend.security

import de.mertendieckmann.griplbackend.config.JwtProperties
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/** Key under which the authenticated user's id (JWT `sub`) is stored on the exchange. */
const val AUTHENTICATED_USER_ID_ATTRIBUTE = "userId"

private val PUBLIC_PATH_PREFIXES = listOf(
    "/actuator/health",
    "/swagger-ui",
    "/v3/api-docs",
    "/thesis/pdf",
)

/**
 * Verifies JWTs issued by ragulate-backend (HS256, shared secret). This is
 * pure signature/expiry verification — no database lookup — since identity
 * is owned entirely by ragulate-backend's MongoDB `users` collection.
 */
@Component
class JwtAuthenticationWebFilter(
    private val jwtProperties: JwtProperties
) : WebFilter {

    private val signingKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(Charsets.UTF_8))
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path.value()
        if (PUBLIC_PATH_PREFIXES.any { path.startsWith(it) }) {
            return chain.filter(exchange)
        }

        val authHeader = exchange.request.headers.getFirst("Authorization")
        val token = authHeader?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
            ?: return unauthorized(exchange)

        val userId = try {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
                .subject
        } catch (e: JwtException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }

        if (userId.isNullOrBlank()) {
            return unauthorized(exchange)
        }

        exchange.attributes[AUTHENTICATED_USER_ID_ATTRIBUTE] = userId
        return chain.filter(exchange)
    }

    private fun unauthorized(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        return exchange.response.setComplete()
    }
}
