import { NextRequest, NextResponse } from "next/server";
import { REFRESH_COOKIE_NAME } from "@/lib/auth-cookie";
import { clearSessionCookies, exchangeRefreshToken, isSecureRequest, setSessionCookies } from "@/lib/session";

// Called by the browser (authenticated-fetch.ts, auth-context.tsx) when the access token is about to expire or
// was rejected. 401 means the session is over and both cookies are cleared; 502 means auth-service could not be
// asked, which leaves the session as it is.
export async function POST(request: NextRequest) {
    const secure = isSecureRequest(request.headers, request.nextUrl.protocol);
    const refreshToken = request.cookies.get(REFRESH_COOKIE_NAME)?.value;

    const outcome = refreshToken ? await exchangeRefreshToken(refreshToken) : ({ status: "invalid" } as const);
    if (outcome.status === "ok") {
        const response = NextResponse.json({ access_token: outcome.tokens.accessToken, user: outcome.tokens.user });
        setSessionCookies((name, value, options) => response.cookies.set(name, value, options), outcome.tokens, secure);
        return response;
    }
    if (outcome.status === "invalid") {
        const response = NextResponse.json({ detail: "Invalid refresh token" }, { status: 401 });
        clearSessionCookies((name, value, options) => response.cookies.set(name, value, options), secure);
        return response;
    }
    return NextResponse.json({ detail: "The login service is not reachable" }, { status: 502 });
}
