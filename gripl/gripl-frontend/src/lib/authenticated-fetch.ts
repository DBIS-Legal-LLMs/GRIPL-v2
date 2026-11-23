"use client"

import { readClientToken } from "@/lib/auth-cookie";
import { isRefreshableFailure, REFRESH_AHEAD_SECONDS } from "@/lib/auth-retry";
import { refreshSession } from "@/lib/client-session";
import { tokenExpiresWithin } from "@/lib/gripl-role";

// Drop-in replacement for fetch() that attaches the logged-in user's bearer
// token, for client components calling the /api rewrite proxy to gripl-backend.
// The access token is short-lived: it is renewed shortly before it expires, and once
// more (with the request repeated once) after a 401 or a 403 about the GRIPL role.
// A repeated request is sent again with the same `init`, so a body must be re-readable (string, FormData, Blob).
export async function authenticatedFetch(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
    let token = readClientToken();
    if ((!token || tokenExpiresWithin(token, REFRESH_AHEAD_SECONDS)) && (await refreshSession())) {
        token = readClientToken();
    }

    const response = await send(input, init, token);
    if (!(await isRefreshableFailure(response)) || !(await refreshSession())) return response;
    return send(input, init, readClientToken());
}

function send(input: RequestInfo | URL, init: RequestInit, token: string | null): Promise<Response> {
    const headers = new Headers(init.headers);
    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }
    return fetch(input, { ...init, headers });
}
