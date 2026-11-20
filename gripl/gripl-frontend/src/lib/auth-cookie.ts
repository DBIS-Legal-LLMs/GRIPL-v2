// Shared between client and server code.
//
// `gripl_token` (the access token, a short-lived JWT) is non-httpOnly on purpose: client
// components attach it manually as an Authorization header (see authenticated-fetch.ts), and
// Server Components/Actions read it via next/headers `cookies()` to forward the same header.
// `gripl_refresh` (auth-service's opaque refresh token) is httpOnly: only the server code in
// session.ts, the route handlers under api/session and middleware.ts ever see it, never
// browser JavaScript, so there is deliberately no client reader for it.
export const AUTH_COOKIE_NAME = "gripl_token";
export const REFRESH_COOKIE_NAME = "gripl_refresh";

// The refresh token lives 30 days from the login (auth-service); the access token's own `exp` is much shorter.
export const SESSION_MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

export function readClientToken(): string | null {
    if (typeof document === "undefined") return null;
    const match = document.cookie.match(new RegExp(`(?:^|; )${AUTH_COOKIE_NAME}=([^;]*)`));
    return match ? decodeURIComponent(match[1]) : null;
}

/** Local fallback for a logout that could not reach the server; the server clears both cookies itself. */
export function clearClientToken(): void {
    document.cookie = `${AUTH_COOKIE_NAME}=; Path=/; Max-Age=0; SameSite=Lax`;
}
