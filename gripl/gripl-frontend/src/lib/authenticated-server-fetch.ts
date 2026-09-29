import { cookies } from "next/headers";
import { AUTH_COOKIE_NAME } from "@/lib/auth-cookie";

// Server-side counterpart to authenticated-fetch.ts's client-side helper.
// Drop-in replacement for fetch() in Server Actions/Components calling
// gripl-backend directly — attaches the logged-in user's bearer token by
// reading the auth cookie via next/headers (no Authorization header cookies()
// can't see, since the browser never sends one to a Server Action call).
export async function authenticatedServerFetch(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
    const token = (await cookies()).get(AUTH_COOKIE_NAME)?.value;
    const headers = new Headers(init.headers);
    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }
    return fetch(input, { ...init, headers });
}
