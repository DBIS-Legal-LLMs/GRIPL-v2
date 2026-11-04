"use client"

import React, { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "@/context/auth-context";
import { toErrorMessage } from "@/lib/http-error";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PasswordInput } from "@/components/ui/input-password";
import { Spinner } from "@/components/ui/spinner";

export default function LoginForm() {
    const { login } = useAuth();
    const router = useRouter();
    const searchParams = useSearchParams();
    const [loginId, setLoginId] = useState("");
    const [loginPassword, setLoginPassword] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    async function handleLogin(e: React.FormEvent) {
        e.preventDefault();
        setError(null);
        setIsSubmitting(true);
        try {
            await login(loginId, loginPassword);
            router.push(searchParams.get("from") || "/");
        } catch (err) {
            setError(toErrorMessage(err));
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <form onSubmit={handleLogin} className="space-y-3 mt-2">
            <div className="space-y-1">
                <Label htmlFor="login-id"></Label>
                <Input id="login-id" value={loginId} placeholder="Email or username" onChange={(e) => setLoginId(e.target.value)} required autoFocus />
            </div>
            <div className="space-y-1">
                <Label htmlFor="login-password"></Label>
                <PasswordInput id="login-password" value={loginPassword} placeholder="Password" onChange={(e) => setLoginPassword(e.target.value)}  required />
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
            <Button type="submit" className="w-full" disabled={isSubmitting}>
                {isSubmitting ? <Spinner className="h-4 w-4" /> : "Log in"}
            </Button>
        </form>
    );
}
