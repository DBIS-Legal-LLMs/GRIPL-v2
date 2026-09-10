package de.mertendieckmann.griplbackend.security

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange

/**
 * The auth-service user id (JWT `sub`) for the current request, as put on the
 * exchange by [JwtAuthenticationWebFilter].
 *
 * The filter rejects every non-public request that lacks a valid token before
 * it reaches a controller, so on any protected endpoint this is always present;
 * the missing-attribute branch is a defensive 401 in case the endpoint is ever
 * added to the filter's public allow-list by mistake.
 */
fun ServerWebExchange.authenticatedUserId(): String =
    getAttribute<String>(AUTHENTICATED_USER_ID_ATTRIBUTE)
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated")
