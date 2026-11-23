import { NextRequest, NextResponse } from "next/server";
import { AUTH_COOKIE_NAME, REFRESH_COOKIE_NAME } from "@/lib/auth-cookie";
import { decodeGriplRole, isPrivilegedGriplRole, isTokenExpired } from "@/lib/gripl-role";
import { clearSessionCookies, exchangeRefreshToken, isSecureRequest, setSessionCookies } from "@/lib/session";

// GRIPL-v2#40: the paths below are admin/researcher only. Listed as prefixes,
// checked against the pathname, not the sidebar — hiding the nav entry alone
// wouldn't stop direct navigation.
const PRIVILEGED_PATH_PREFIXES = ["/labeling", "/evaluation"];

// Page-level redirect only — the actual enforcement happens server-side,
// independently, in gripl-backend's JwtAuthenticationWebFilter (which verifies
// the token against auth-service's JWKS and, for these same paths,
// requireGriplRole — GRIPL-v2#40).
type Renewal = Awaited<ReturnType<typeof exchangeRefreshToken>>;

export async function middleware(request: NextRequest) {
    let token = request.cookies.get(AUTH_COOKIE_NAME)?.value;
    const refreshToken = request.cookies.get(REFRESH_COOKIE_NAME)?.value;
    let renewed: Renewal | null = null;

    if (!token || isTokenExpired(token)) {
        // an expired access token is not the end of the session: swap it for a new one with the refresh cookie
        renewed = refreshToken ? await exchangeRefreshToken(refreshToken) : null;
        if (renewed?.status !== "ok") {
            return redirectToLogin(request, renewed?.status !== "unavailable");
        }
        token = renewed.tokens.accessToken;
    }

    const path = request.nextUrl.pathname;
    if (PRIVILEGED_PATH_PREFIXES.some((prefix) => path.startsWith(prefix))) {
        if (!isPrivilegedGriplRole(decodeGriplRole(token)) && refreshToken && !renewed) {
            // the role in the token may be out of date (changed in auth-service): look at the current one before refusing
            const current = await exchangeRefreshToken(refreshToken);
            if (current.status === "ok") {
                renewed = current;
                token = current.tokens.accessToken;
            }
        }
        if (!isPrivilegedGriplRole(decodeGriplRole(token))) {
            return withSession(NextResponse.redirect(new URL("/", request.url)), request, renewed);
        }
    }

    if (renewed?.status === "ok") {
        // the page rendered for this very request has to see the new cookies as well
        request.cookies.set(AUTH_COOKIE_NAME, renewed.tokens.accessToken);
        request.cookies.set(REFRESH_COOKIE_NAME, renewed.tokens.refreshToken);
        return withSession(NextResponse.next({ request: { headers: request.headers } }), request, renewed);
    }
    return NextResponse.next();
}

function withSession(response: NextResponse, request: NextRequest, renewed: Renewal | null): NextResponse {
    if (renewed?.status === "ok") {
        setSessionCookies((name, value, options) => response.cookies.set(name, value, options), renewed.tokens, isSecureRequest(request.headers, request.nextUrl.protocol));
    }
    return response;
}

/** [endSession]: also drop the cookies. Not done when auth-service could not be reached, since the session may be fine. */
function redirectToLogin(request: NextRequest, endSession: boolean): NextResponse {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("from", request.nextUrl.pathname);
    const response = NextResponse.redirect(loginUrl);
    if (endSession) {
        clearSessionCookies((name, value, options) => response.cookies.set(name, value, options), isSecureRequest(request.headers, request.nextUrl.protocol));
    }
    return response;
}

export const config = {
    matcher: [
        "/((?!login|api|rag|auth|_next/static|_next/image|favicon.ico|logo.png|icon.png).*)",
    ],
};
