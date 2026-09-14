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

/**
 * The caller's resolved GRIPL role (`app_roles.gripl`), or null if the token
 * doesn't carry one — see [AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE].
 */
fun ServerWebExchange.authenticatedGriplRole(): String? =
    getAttribute<String>(AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE)

/** The two roles that currently have access to everything beyond the sandbox
 * (GRIPL-v2#40: "researcher — everything except admin-only actions", and
 * nothing admin-only exists in GRIPL yet, so the two are equivalent today). */
val PRIVILEGED_GRIPL_ROLES = setOf("admin", "researcher")

/**
 * Gate an endpoint to one of `allowedRoles`. Throws 403 — with a message
 * naming the caller's actual role, or noting its absence — when the caller
 * doesn't qualify (GRIPL-v2#40).
 */
fun ServerWebExchange.requireGriplRole(allowedRoles: Set<String>) {
    val role = authenticatedGriplRole()
    if (role == null || role !in allowedRoles) {
        val reason = role?.let { "caller's GRIPL role is '$it'" }
            ?: "token carries no GRIPL role — is GRIPL registered in auth-service's application registry?"
        throw ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Requires one of $allowedRoles; $reason",
        )
    }
}

/** Shorthand for the common case: gate to [PRIVILEGED_GRIPL_ROLES]. */
fun ServerWebExchange.requirePrivilegedGriplRole() = requireGriplRole(PRIVILEGED_GRIPL_ROLES)
