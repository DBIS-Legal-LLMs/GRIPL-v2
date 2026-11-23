import { cookies, headers } from "next/headers";
import { AUTH_COOKIE_NAME, REFRESH_COOKIE_NAME, SESSION_MAX_AGE_SECONDS } from "@/lib/auth-cookie";
import { isRefreshableFailure, REFRESH_AHEAD_SECONDS } from "@/lib/auth-retry";
import { tokenExpiresWithin } from "@/lib/gripl-role";
import { clearSessionCookies, exchangeRefreshToken, isSecureRequest, setSessionCookies } from "@/lib/session";

// Server-side counterpart to authenticated-fetch.ts's client-side helper.
// Drop-in replacement for fetch() in Server Actions calling gripl-backend or
// auth-service directly — attaches the logged-in user's bearer token by
// reading the auth cookie via next/headers (no Authorization header cookies()
// can't see, since the browser never sends one to a Server Action call).
//
// Like the browser helper it refreshes the access token with the refresh cookie when it is about to
// expire, and once more (then repeats the request once) after a 401 or a 403 about the GRIPL role.
// Refreshing needs to store the new cookies, which only Server Actions and route handlers may do. When this runs
// while a Server Component renders (the pages call the same actions), it does not refresh, because a refresh whose
// new tokens cannot be stored would use up the refresh token; middleware.ts keeps the cookies fresh for page renders.
export async function authenticatedServerFetch(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
    let token = (await cookies()).get(AUTH_COOKIE_NAME)?.value;
    if (!token || tokenExpiresWithin(token, REFRESH_AHEAD_SECONDS)) {
        token = (await refreshSession()) ?? token;
    }

    const response = await send(input, init, token);
    if (!(await isRefreshableFailure(response))) return response;

    const fresh = await refreshSession();
    return fresh ? send(input, init, fresh) : response;
}

function send(input: RequestInfo | URL, init: RequestInit, token: string | undefined): Promise<Response> {
    const headers = new Headers(init.headers);
    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }
    return fetch(input, { ...init, headers });
}

/** Exchanges the refresh cookie for new cookies; returns the new access token, or null if there is none. */
async function refreshSession(): Promise<string | null> {
    const store = await cookies();
    const refreshToken = store.get(REFRESH_COOKIE_NAME)?.value;
    if (!refreshToken) return null;

    const secure = isSecureRequest(await headers());
    // writing the unchanged refresh cookie back is a no-op where cookies are writable and throws where they are not
    try {
        store.set(REFRESH_COOKIE_NAME, refreshToken, { httpOnly: true, secure, sameSite: "lax", path: "/", maxAge: SESSION_MAX_AGE_SECONDS });
    } catch {
        return null;
    }

    const outcome = await exchangeRefreshToken(refreshToken);
    if (outcome.status === "invalid") {
        clearSessionCookies((name, value, options) => store.set(name, value, options), secure);
        return null;
    }
    if (outcome.status !== "ok") return null;

    setSessionCookies((name, value, options) => store.set(name, value, options), outcome.tokens, secure);
    return outcome.tokens.accessToken;
}
