// Shared between client and server code. Non-httpOnly on purpose: client
// components attach it manually as an Authorization header (see
// authenticated-fetch.ts), and Server Components/Actions read it via
// next/headers `cookies()` to forward the same header server-side.
export const AUTH_COOKIE_NAME = "gripl_token";

const MAX_AGE_SECONDS = 60 * 60 * 24 * 30; // 30 days — the JWT itself still expires per its own `exp` claim

export function readClientToken(): string | null {
    if (typeof document === "undefined") return null;
    const match = document.cookie.match(new RegExp(`(?:^|; )${AUTH_COOKIE_NAME}=([^;]*)`));
    return match ? decodeURIComponent(match[1]) : null;
}

export function writeClientToken(token: string): void {
    document.cookie = `${AUTH_COOKIE_NAME}=${encodeURIComponent(token)}; Path=/; Max-Age=${MAX_AGE_SECONDS}; SameSite=Lax`;
}

export function clearClientToken(): void {
    document.cookie = `${AUTH_COOKIE_NAME}=; Path=/; Max-Age=0; SameSite=Lax`;
}
