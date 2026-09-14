import { NextRequest, NextResponse } from "next/server";
import { AUTH_COOKIE_NAME } from "@/lib/auth-cookie";
import { decodeGriplRole, isPrivilegedGriplRole } from "@/lib/gripl-role";

// GRIPL-v2#40: the paths below are admin/researcher only. Listed as prefixes,
// checked against the pathname, not the sidebar — hiding the nav entry alone
// wouldn't stop direct navigation.
const PRIVILEGED_PATH_PREFIXES = ["/labeling", "/evaluation"];

// Page-level redirect only — the actual enforcement happens server-side,
// independently, in gripl-backend's JwtAuthenticationWebFilter (which verifies
// the token against auth-service's JWKS and, for these same paths,
// requireGriplRole — GRIPL-v2#40).
export function middleware(request: NextRequest) {
    const token = request.cookies.get(AUTH_COOKIE_NAME)?.value;
    if (!token) {
        const loginUrl = new URL("/login", request.url);
        loginUrl.searchParams.set("from", request.nextUrl.pathname);
        return NextResponse.redirect(loginUrl);
    }

    const path = request.nextUrl.pathname;
    if (PRIVILEGED_PATH_PREFIXES.some((prefix) => path.startsWith(prefix))) {
        if (!isPrivilegedGriplRole(decodeGriplRole(token))) {
            return NextResponse.redirect(new URL("/", request.url));
        }
    }

    return NextResponse.next();
}

export const config = {
    matcher: [
        "/((?!login|api|rag|auth|_next/static|_next/image|favicon.ico|logo.png|icon.png).*)",
    ],
};
