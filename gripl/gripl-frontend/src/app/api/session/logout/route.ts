import { NextRequest, NextResponse } from "next/server";
import { REFRESH_COOKIE_NAME } from "@/lib/auth-cookie";
import { clearSessionCookies, isSecureRequest, revokeRefreshToken } from "@/lib/session";

// Revokes the session at auth-service and clears both cookies, whatever auth-service answers.
export async function POST(request: NextRequest) {
    const refreshToken = request.cookies.get(REFRESH_COOKIE_NAME)?.value;
    if (refreshToken) await revokeRefreshToken(refreshToken);

    const response = new NextResponse(null, { status: 204 });
    clearSessionCookies((name, value, options) => response.cookies.set(name, value, options), isSecureRequest(request.headers, request.nextUrl.protocol));
    return response;
}
