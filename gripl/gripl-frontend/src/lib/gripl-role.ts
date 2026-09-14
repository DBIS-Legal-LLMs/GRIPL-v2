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
