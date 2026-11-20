"use client"

import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { clearClientToken, readClientToken } from "@/lib/auth-cookie";
import { refreshSession, SESSION_ENDED_EVENT, TOKEN_CHANGED_EVENT } from "@/lib/client-session";
import { extractErrorDetails } from "@/lib/http-error";
import { decodeGriplRole, isTokenExpired, msUntilRefresh } from "@/lib/gripl-role";

// Registration is proxied to auth-service through the /auth/* rewrite (see
// next.config.ts) — the browser only ever talks to this origin, and
// auth-service issues the RS256 JWT that both gripl-backend and RAGulate verify.
// Login and logout go through our own route handlers instead (app/api/session), because they
// also have to set and clear the httpOnly refresh cookie that JavaScript must not touch.
const AUTH_REGISTER_URL = "/auth/register";
const SESSION_LOGIN_URL = "/api/session/login";
const SESSION_LOGOUT_URL = "/api/session/logout";


// Display-only, kept alongside the token cookie so the sidebar can show who is
// logged in after a reload without decoding it out of the (username-less) JWT.
const USERNAME_STORAGE_KEY = "gripl_username";

interface AuthContextValue {
    token: string | null;
    /** The JWT `sub` claim (auth-service's Mongo user id), decoded client-side for display only. */
    userId: string | null;
    /** The logged-in user's username, from auth-service's login response. */
    username: string | null;
    /** The JWT `app_roles.gripl` claim, decoded client-side for display/UI
     * gating only (GRIPL-v2#40) — see `@/lib/gripl-role`. */
    griplRole: string | null;
    isLoading: boolean;
    login: (usernameOrEmail: string, password: string) => Promise<void>;
    register: (email: string, username: string, password: string, fullName?: string) => Promise<void>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return context;
}

// Not signature-verified — this is only ever used for UI display, never for
// authorization (the backends independently verify the signature).
function decodeSubject(token: string): string | null {
    try {
        const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
        return typeof payload.sub === "string" ? payload.sub : null;
    } catch {
        return null;
    }
}

function readStoredUsername(): string | null {
    if (typeof window === "undefined") return null;
    try {
        return window.localStorage.getItem(USERNAME_STORAGE_KEY);
    } catch {
        return null;
    }
}

function writeStoredUsername(username: string | null): void {
    if (typeof window === "undefined") return;
    try {
        if (username) {
            window.localStorage.setItem(USERNAME_STORAGE_KEY, username);
        } else {
            window.localStorage.removeItem(USERNAME_STORAGE_KEY);
        }
    } catch {
        /* private mode / storage disabled — username just won't persist */
    }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [token, setToken] = useState<string | null>(null);
    const [username, setUsername] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const router = useRouter();
    const pathname = usePathname();

    const endSession = useCallback(() => {
        clearClientToken();
        writeStoredUsername(null);
        setToken(null);
        setUsername(null);
    }, []);

    useEffect(() => {
        const stored = readClientToken();
        if (stored && isTokenExpired(stored)) {
            // normally middleware.ts renewed it before the page loaded; try once more, then give up
            void refreshSession().then((renewed) => {
                if (!renewed) endSession();
            });
            setUsername(readStoredUsername());
        } else {
            setToken(stored);
            setUsername(readStoredUsername());
        }
        setIsLoading(false);
    }, [endSession]);

    // a refresh happened somewhere (authenticated-fetch.ts, the timer below): the new cookie carries the current role
    useEffect(() => {
        const onTokenChanged = () => setToken(readClientToken());
        window.addEventListener(TOKEN_CHANGED_EVENT, onTokenChanged);
        return () => window.removeEventListener(TOKEN_CHANGED_EVENT, onTokenChanged);
    }, []);

    // auth-service rejected the refresh token: the session is over
    useEffect(() => {
        const onSessionEnded = () => {
            endSession();
            if (pathname !== "/login") {
                router.push(`/login?from=${encodeURIComponent(pathname)}`);
            }
        };
        window.addEventListener(SESSION_ENDED_EVENT, onSessionEnded);
        return () => window.removeEventListener(SESSION_ENDED_EVENT, onSessionEnded);
    }, [endSession, pathname, router]);

    useEffect(() => {
        // renew before expiry even without requests, so a user who is just reading keeps a current token and
        // sees a role change within one token lifetime
        const delay = token ? msUntilRefresh(token) : null;
        if (delay === null) return;
        const timer = setTimeout(() => void refreshSession(), delay);
        return () => clearTimeout(timer);
    }, [token]);

    const login = useCallback(async (usernameOrEmail: string, password: string) => {
        const res = await fetch(SESSION_LOGIN_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username: usernameOrEmail, password }),
        });
        if (!res.ok) {
            throw new Error(await extractErrorDetails(res));
        }
        // the route handler has already set the cookies; this is only the state for the UI
        const data = (await res.json()) as { access_token: string; user?: { username?: string } };
        setToken(data.access_token);

        const resolvedUsername = data.user?.username ?? null;
        writeStoredUsername(resolvedUsername);
        setUsername(resolvedUsername);
    }, []);

    const register = useCallback(async (email: string, newUsername: string, password: string, fullName?: string) => {
        const res = await fetch(AUTH_REGISTER_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, username: newUsername, password, full_name: fullName || undefined }),
        });
        if (!res.ok) {
            throw new Error(await extractErrorDetails(res));
        }
        // Registration doesn't return a token — chain a login.
        await login(newUsername, password);
    }, [login]);

    const logout = useCallback(() => {
        // revoke the session on the server and clear its cookies; the UI is logged out whatever that answers
        void fetch(SESSION_LOGOUT_URL, { method: "POST", credentials: "same-origin" }).catch(() => undefined);
        endSession();
        router.push("/login");
    }, [endSession, router]);

    const userId = token ? decodeSubject(token) : null;
    const griplRole = token ? decodeGriplRole(token) : null;

    return <AuthContext.Provider value={{ token, userId, username, griplRole, isLoading, login, register, logout }}>
        {children}
    </AuthContext.Provider>;
}
