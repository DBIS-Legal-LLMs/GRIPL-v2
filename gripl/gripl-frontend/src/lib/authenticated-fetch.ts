"use client"

import { readClientToken } from "@/lib/auth-cookie";

// Drop-in replacement for fetch() that attaches the logged-in user's bearer
// token, for client components calling the /api or /chat rewrite proxies.
export function authenticatedFetch(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
    const token = readClientToken();
    const headers = new Headers(init.headers);
    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }
    return fetch(input, { ...init, headers });
}
