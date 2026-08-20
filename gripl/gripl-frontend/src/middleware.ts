import { NextRequest, NextResponse } from "next/server";
import { AUTH_COOKIE_NAME } from "@/lib/auth-cookie";

// Page-level redirect only — the actual enforcement happens server-side,
// independently, in gripl-backend's JwtAuthenticationWebFilter and
// ragulate-backend's get_current_user dependency.
export function middleware(request: NextRequest) {
    const token = request.cookies.get(AUTH_COOKIE_NAME)?.value;
    if (!token) {
        const loginUrl = new URL("/login", request.url);
        loginUrl.searchParams.set("from", request.nextUrl.pathname);
        return NextResponse.redirect(loginUrl);
    }
    return NextResponse.next();
}

export const config = {
    matcher: [
        "/((?!login|api|rag|chat|_next/static|_next/image|favicon.ico|logo.png|icon.png).*)",
    ],
};
