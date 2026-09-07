"use client"

import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { clearClientToken, readClientToken, writeClientToken } from "@/lib/auth-cookie";
import { extractErrorDetails } from "@/lib/http-error";

// Login/register are proxied to auth-service through the /auth/* rewrite (see
// next.config.ts) — the browser only ever talks to this origin, and
// auth-service issues the RS256 JWT that both gripl-backend and RAGulate verify.
const AUTH_LOGIN_URL = "/auth/login";
const AUTH_REGISTER_URL = "/auth/register";

// Display-only, kept alongside the token cookie so the sidebar can show who is
// logged in after a reload without decoding it out of the (username-less) JWT.
const USERNAME_STORAGE_KEY = "gripl_username";

interface AuthContextValue {
    token: string | null;
    /** The JWT `sub` claim (auth-service's Mongo user id), decoded client-side for display only. */
    userId: string | null;
    /** The logged-in user's username, from auth-service's login response. */
    username: string | null;
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

    useEffect(() => {
        setToken(readClientToken());
        setUsername(readStoredUsername());
        setIsLoading(false);
    }, []);

    const login = useCallback(async (usernameOrEmail: string, password: string) => {
        const body = new URLSearchParams();
        body.set("grant_type", "password");
        body.set("username", usernameOrEmail);
        body.set("password", password);

        const res = await fetch(AUTH_LOGIN_URL, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body,
        });
        if (!res.ok) {
            throw new Error(await extractErrorDetails(res));
        }
        const data = (await res.json()) as { access_token: string; user?: { username?: string } };
        writeClientToken(data.access_token);
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
        clearClientToken();
        writeStoredUsername(null);
        setToken(null);
        setUsername(null);
        router.push("/login");
    }, [router]);

    const userId = token ? decodeSubject(token) : null;

    return <AuthContext.Provider value={{ token, userId, username, isLoading, login, register, logout }}>
        {children}
    </AuthContext.Provider>;
}
