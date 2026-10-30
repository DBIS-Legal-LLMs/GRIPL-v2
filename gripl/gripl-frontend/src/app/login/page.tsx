"use client"

import React, { Suspense } from "react";
import Image from "next/image";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import LoginForm from "./login-form";
import RegisterForm from "./register-form";

export default function LoginPage() {
    return (
        <Suspense>
            <div className="relative flex h-full w-full items-center justify-center overflow-hidden p-6">
                <div className="absolute inset-0 -z-10 bg-gradient-to-br from-primary/20 via-background to-muted opacity-60 blur-3xl" />
                <Card className="w-full max-w-sm border-border/50 bg-background/80 shadow-xl backdrop-blur-sm">
                    <CardHeader className="items-center text-center">
                        <Image src="/logo.png" alt="GRIPL" width={100} height={100} className="mb-2 h-auto w-24" />
                        <CardTitle className="text-2xl">Welcome to GRIPL</CardTitle>
                        <CardDescription>Sign in to continue</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <Tabs defaultValue="login">
                            <TabsList className="w-full">
                                <TabsTrigger value="login" className="flex-1">Log in</TabsTrigger>
                                <TabsTrigger value="register" className="flex-1">Register</TabsTrigger>
                            </TabsList>
                            <TabsContent value="login">
                                <LoginForm />
                            </TabsContent>
                            <TabsContent value="register">
                                <RegisterForm />
                            </TabsContent>
                        </Tabs>
                    </CardContent>
                </Card>
            </div>
        </Suspense>
    );
}
