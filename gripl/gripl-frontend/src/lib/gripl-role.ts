// GRIPL-v2#40 — role-based access control. Pure functions so both a client
// component (AuthContext, for hiding sidebar entries) and middleware.ts
// (edge runtime, for blocking direct navigation) can share the same logic.
//
// Decoding here is NOT signature-verified — display/UX only, same caveat as
// AuthContext's decodeSubject. gripl-backend's JwtAuthenticationWebFilter +
// requireGriplRole is the actual enforcement; this just keeps the UI from
// showing/letting the user into pages the backend will reject anyway.

/** Roles with access beyond the sandbox (Labeling, Evaluation). Mirrors
 * gripl-backend's PRIVILEGED_GRIPL_ROLES (security/AuthenticatedUser.kt). */
const PRIVILEGED_GRIPL_ROLES = new Set(["admin", "researcher"]);

export function isPrivilegedGriplRole(role: string | null | undefined): boolean {
    return !!role && PRIVILEGED_GRIPL_ROLES.has(role);
}

/** The caller's resolved GRIPL role (JWT `app_roles.gripl`), or null if the
 * token is missing/malformed/has no gripl entry. */
export function decodeGriplRole(token: string): string | null {
    try {
        const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
        const role = payload?.app_roles?.gripl;
        return typeof role === "string" ? role : null;
    } catch {
        return null;
    }
}

function tokenPayload(token: string): { exp?: unknown; iat?: unknown } | null {
    try {
        return JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
    } catch {
        return null;
    }
}

/** The token's `exp` claim in seconds since the epoch, or null if the token is malformed. */
export function tokenExpiry(token: string): number | null {
    const exp = tokenPayload(token)?.exp;
    return typeof exp === "number" ? exp : null;
}

/** Milliseconds until a token should be renewed: a minute before it expires, or sooner for tokens that live less
 * than four minutes (a quarter of the lifetime), but never within 5 seconds. null if the token has no expiry. */
export function msUntilRefresh(token: string): number | null {
    const payload = tokenPayload(token);
    if (typeof payload?.exp !== "number") return null;
    const lifetime = typeof payload.iat === "number" ? payload.exp - payload.iat : 900;
    const lead = Math.min(60, lifetime / 4);
    return Math.max((payload.exp - lead) * 1000 - Date.now(), 5_000);
}

/** True if the token is malformed or its `exp` claim has passed (30s of
 * clock-skew slack, matching gripl-backend). The cookie holding the token lives
 * as long as the session, so without this check the UI keeps looking logged in
 * after the access token has expired and every request 401s; the refresh token
 * (session.ts) is what replaces it. */
export function isTokenExpired(token: string): boolean {
    const exp = tokenExpiry(token);
    return exp === null || exp * 1000 < Date.now() - 30_000;
}

/** True if the token is missing its expiry or expires within [seconds]; the time to refresh it up front. */
export function tokenExpiresWithin(token: string, seconds: number): boolean {
    const exp = tokenExpiry(token);
    return exp === null || exp * 1000 < Date.now() + seconds * 1000;
}
