"use client"

import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { clearClientToken, readClientToken, writeClientToken } from "@/lib/auth-cookie";
import { extractErrorDetails } from "@/lib/http-error";

interface AuthContextValue {
    token: string | null;
    /** The JWT `sub` claim (ragulate-backend's Mongo user id), decoded client-side for display only. */
    userId: string | null;
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

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [token, setToken] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const router = useRouter();

    useEffect(() => {
        setToken(readClientToken());
        setIsLoading(false);
    }, []);

    const login = useCallback(async (usernameOrEmail: string, password: string) => {
        const body = new URLSearchParams();
        body.set("username", usernameOrEmail);
        body.set("password", password);

        const res = await fetch("/chat/api/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body,
        });
        if (!res.ok) {
            throw new Error(await extractErrorDetails(res));
        }
        const data = (await res.json()) as { access_token: string };
        writeClientToken(data.access_token);
        setToken(data.access_token);
    }, []);

    const register = useCallback(async (email: string, username: string, password: string, fullName?: string) => {
        const res = await fetch("/chat/api/auth/register", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, username, password, full_name: fullName || undefined }),
        });
        if (!res.ok) {
            throw new Error(await extractErrorDetails(res));
        }
        // Registration doesn't return a token — chain a login.
        await login(username, password);
    }, [login]);

    const logout = useCallback(() => {
        clearClientToken();
        setToken(null);
        router.push("/login");
    }, [router]);

    const userId = token ? decodeSubject(token) : null;

    return <AuthContext.Provider value={{ token, userId, isLoading, login, register, logout }}>
        {children}
    </AuthContext.Provider>;
}
