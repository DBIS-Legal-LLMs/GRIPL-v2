"use client"

import React, { Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Image from "next/image";
import { useAuth } from "@/context/auth-context";
import { toErrorMessage } from "@/lib/http-error";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { PasswordInput } from "@/components/ui/input-password";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";

export default function LoginPage() {
    return <Suspense>
        <LoginForm />
    </Suspense>;
}

function LoginForm() {
    const { login, register } = useAuth();
    const router = useRouter();
    const searchParams = useSearchParams();

    const [loginId, setLoginId] = useState("");
    const [loginPassword, setLoginPassword] = useState("");
    const [regEmail, setRegEmail] = useState("");
    const [regUsername, setRegUsername] = useState("");
    const [regPassword, setRegPassword] = useState("");
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

    async function handleRegister(e: React.FormEvent) {
        e.preventDefault();
        setError(null);
        setIsSubmitting(true);
        try {
            await register(regEmail, regUsername, regPassword);
            router.push(searchParams.get("from") || "/");
        } catch (err) {
            setError(toErrorMessage(err));
        } finally {
            setIsSubmitting(false);
        }
    }

    return <div className="h-full w-full flex items-center justify-center p-6">
        <Card className="w-full max-w-sm">
            <CardHeader className="items-center text-center">
                <Image src="/logo.png" alt="GRIPL" width={100} height={100} className="w-24 h-auto mb-2" />
                <CardTitle>Sign in to GRIPL</CardTitle>
                <CardDescription>Shared account with RAGulate</CardDescription>
            </CardHeader>
            <CardContent>
                <Tabs defaultValue="login">
                    <TabsList className="w-full">
                        <TabsTrigger value="login" className="flex-1">Log in</TabsTrigger>
                        <TabsTrigger value="register" className="flex-1">Register</TabsTrigger>
                    </TabsList>

                    <TabsContent value="login">
                        <form onSubmit={handleLogin} className="space-y-3 mt-2">
                            <div className="space-y-1">
                                <Label htmlFor="login-id">Email or username</Label>
                                <Input id="login-id" value={loginId} onChange={(e) => setLoginId(e.target.value)} required autoFocus />
                            </div>
                            <div className="space-y-1">
                                <Label htmlFor="login-password">Password</Label>
                                <PasswordInput id="login-password" value={loginPassword} onChange={(e) => setLoginPassword(e.target.value)} required />
                            </div>
                            {error && <p className="text-sm text-destructive">{error}</p>}
                            <Button type="submit" className="w-full" disabled={isSubmitting}>
                                {isSubmitting ? <Spinner className="h-4 w-4" /> : "Log in"}
                            </Button>
                        </form>
                    </TabsContent>

                    <TabsContent value="register">
                        <form onSubmit={handleRegister} className="space-y-3 mt-2">
                            <div className="space-y-1">
                                <Label htmlFor="reg-email">Email</Label>
                                <Input id="reg-email" type="email" value={regEmail} onChange={(e) => setRegEmail(e.target.value)} required />
                            </div>
                            <div className="space-y-1">
                                <Label htmlFor="reg-username">Username</Label>
                                <Input id="reg-username" value={regUsername} onChange={(e) => setRegUsername(e.target.value)} required />
                            </div>
                            <div className="space-y-1">
                                <Label htmlFor="reg-password">Password</Label>
                                <PasswordInput id="reg-password" value={regPassword} onChange={(e) => setRegPassword(e.target.value)} required minLength={8} />
                                <p className="text-xs text-muted-foreground">At least 8 characters, with upper/lowercase, a digit, and a special character.</p>
                            </div>
                            {error && <p className="text-sm text-destructive">{error}</p>}
                            <Button type="submit" className="w-full" disabled={isSubmitting}>
                                {isSubmitting ? <Spinner className="h-4 w-4" /> : "Create account"}
                            </Button>
                        </form>
                    </TabsContent>
                </Tabs>
            </CardContent>
        </Card>
    </div>;
}
