// Server-side half of the refresh-token session (auth-service#13): the three auth-service endpoints and the
// cookies that carry the session. No `next/headers` here, so middleware.ts (edge runtime), the route handlers
// under api/session and the Server Actions can all share it. Never import it from client code.
import { AUTH_COOKIE_NAME, REFRESH_COOKIE_NAME, SESSION_MAX_AGE_SECONDS } from "@/lib/auth-cookie";

// Same default as next.config.ts's /auth rewrite; the Docker stacks set AUTH_SERVICE_INTERNAL_URL at build time.
const authBase = () => process.env.AUTH_SERVICE_INTERNAL_URL ?? "http://localhost:8100";

export interface SessionTokens {
    accessToken: string;
    refreshToken: string;
    /** auth-service's `user` object, as returned by login and refresh. */
    user?: { username?: string };
}

export type RefreshOutcome =
    | { status: "ok"; tokens: SessionTokens }
    /** 401: unknown, expired, revoked or reused refresh token. The session is over. */
    | { status: "invalid" }
    /** auth-service unreachable or failing: the session may still be fine, so do not log the user out. */
    | { status: "unavailable" };

interface SessionResponse {
    access_token: string;
    refresh_token: string;
    user?: { username?: string };
}

export function toSessionTokens(body: SessionResponse): SessionTokens {
    return { accessToken: body.access_token, refreshToken: body.refresh_token, user: body.user };
}

export async function exchangeRefreshToken(refreshToken: string): Promise<RefreshOutcome> {
    try {
        const res = await fetch(`${authBase()}/auth/refresh`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ refresh_token: refreshToken }),
            cache: "no-store",
        });
        if (res.status === 401) return { status: "invalid" };
        if (!res.ok) return { status: "unavailable" };
        return { status: "ok", tokens: toSessionTokens((await res.json()) as SessionResponse) };
    } catch {
        return { status: "unavailable" };
    }
}

/** Revokes the whole session on the server. Best effort: the caller clears its cookies either way. */
export async function revokeRefreshToken(refreshToken: string): Promise<void> {
    try {
        await fetch(`${authBase()}/auth/logout`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ refresh_token: refreshToken }),
            cache: "no-store",
        });
    } catch {
        /* auth-service unreachable: the refresh token expires on its own */
    }
}

export async function loginWithPassword(username: string, password: string): Promise<Response> {
    const body = new URLSearchParams({ grant_type: "password", username, password });
    return fetch(`${authBase()}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body,
        cache: "no-store",
    });
}

/** `Secure` only when the browser reached us over https (behind Traefik that is the forwarded protocol). */
export function isSecureRequest(headers: Headers, protocol?: string): boolean {
    const forwarded = headers.get("x-forwarded-proto")?.split(",")[0].trim();
    return (forwarded ?? protocol?.replace(":", "")) === "https";
}

type CookieSetter = (name: string, value: string, options: {
    httpOnly: boolean;
    secure: boolean;
    sameSite: "lax";
    path: string;
    maxAge: number;
}) => unknown;

export function setSessionCookies(set: CookieSetter, tokens: SessionTokens, secure: boolean): void {
    // the access token stays readable by JS (authenticated-fetch.ts, the sidebar); the refresh token never is
    set(AUTH_COOKIE_NAME, tokens.accessToken, { httpOnly: false, secure, sameSite: "lax", path: "/", maxAge: SESSION_MAX_AGE_SECONDS });
    set(REFRESH_COOKIE_NAME, tokens.refreshToken, { httpOnly: true, secure, sameSite: "lax", path: "/", maxAge: SESSION_MAX_AGE_SECONDS });
}

export function clearSessionCookies(set: CookieSetter, secure: boolean): void {
    for (const [name, httpOnly] of [[AUTH_COOKIE_NAME, false], [REFRESH_COOKIE_NAME, true]] as const) {
        set(name, "", { httpOnly, secure, sameSite: "lax", path: "/", maxAge: 0 });
    }
}
