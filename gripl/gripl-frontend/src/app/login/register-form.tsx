"use client"

import React, { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { CheckCircle2, Circle, RefreshCw } from "lucide-react";
import { useAuth } from "@/context/auth-context";
import { extractErrorDetails, toErrorMessage } from "@/lib/http-error";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PasswordInput } from "@/components/ui/input-password";
import { Spinner } from "@/components/ui/spinner";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";

const PASSWORD_POLICY_MESSAGE =
    "Password must be at least 8 characters long and include an uppercase letter, a lowercase letter, a digit, and a special character.";

function meetsPasswordPolicy(password: string): boolean {
    return password.length >= 8
        && /[A-Z]/.test(password)
        && /[a-z]/.test(password)
        && /[0-9]/.test(password)
        && /[!@#$%^&*()\-_=+{}\[\]|;:'",.<>/?]/.test(password);
}

function emailHasValidFormat(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function ValidationIndicator({ valid, children }: { valid: boolean; children: React.ReactNode }) {
    const Icon = valid ? CheckCircle2 : Circle;
    return (
        <li
            aria-live="polite"
            className={valid
                ? "flex items-center gap-1.5 text-xs font-medium leading-4 text-green-600 dark:text-green-500"
                : "flex items-center gap-1.5 text-xs font-medium leading-4 "}
        >
            <Icon className="h-3 w-3 shrink-0" aria-hidden="true" />
            {children}
        </li>
    );
}

export default function RegisterForm() {
    const { register } = useAuth();
    const router = useRouter();
    const searchParams = useSearchParams();
    const [regEmail, setRegEmail] = useState("");
    const [regUsername, setRegUsername] = useState("");
    const [regPassword, setRegPassword] = useState("");
    const [isGeneratingUsername, setIsGeneratingUsername] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const emailIsValid = emailHasValidFormat(regEmail);
    const passwordChecks = [
        { label: "At least 8 characters", valid: regPassword.length >= 8 },
        { label: "One uppercase letter", valid: /[A-Z]/.test(regPassword) },
        { label: "One lowercase letter", valid: /[a-z]/.test(regPassword) },
        { label: "One digit", valid: /[0-9]/.test(regPassword) },
        { label: "One special character", valid: /[!@#$%^&*()\-_=+{}\[\]|;:'",.<>/?]/.test(regPassword) },
    ];

    async function generateUsername() {
        setError(null);
        setIsGeneratingUsername(true);
        try {
            const response = await fetch("/auth/register/genuser");
            if (!response.ok) {
                throw new Error(await extractErrorDetails(response));
            }

            const suggestion: unknown = await response.json();
            if (typeof suggestion !== "string" || !suggestion.trim()) {
                throw new Error("The server did not return a username");
            }
            setRegUsername(suggestion.trim());
        } catch (err) {
            setError(toErrorMessage(err));
        } finally {
            setIsGeneratingUsername(false);
        }
    }

    async function handleRegister(e: React.FormEvent) {
        e.preventDefault();
        setError(null);
        if (!meetsPasswordPolicy(regPassword)) {
            setError(PASSWORD_POLICY_MESSAGE);
            return;
        }

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

    return (
        <form onSubmit={handleRegister} className="space-y-3 mt-2">
            <div className="space-y-1">
                <Label htmlFor="reg-email">Email</Label>
                <Input
                    id="reg-email"
                    type="email"
                    value={regEmail}
                    placeholder="Enter a valid email address"
                    onChange={(e) => setRegEmail(e.target.value)}
                    required
                    aria-invalid={regEmail.length > 0 && !emailIsValid}
                    className={`placeholder:text-muted-foreground/60 ${emailIsValid
                        ? "border-green-400 focus-visible:ring-green-400 dark:border-green-300"
                        : ""}`}
                />
            </div>
            <div className="space-y-1">
                <div className="flex items-center justify-between gap-2">
                    <Label htmlFor="reg-username">Username</Label>
                    <TooltipProvider delayDuration={300}>
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    type="button"
                                    variant="outline"
                                    size="icon"
                                    onClick={generateUsername}
                                    disabled={isGeneratingUsername || isSubmitting}
                                    aria-label="Generate a random username"
                                >
                                    {isGeneratingUsername
                                        ? <Spinner className="h-4 w-4" />
                                        : <RefreshCw className="h-4 w-4" aria-hidden="true" />}
                                </Button>
                            </TooltipTrigger>
                            <TooltipContent>Generate a random username</TooltipContent>
                        </Tooltip>
                    </TooltipProvider>
                </div>
                <Input 
                id="reg-username" 
                value={regUsername} 
                placeholder="Enter a username or generate one"
                onChange={(e) => setRegUsername(e.target.value)}
                className="placeholder:text-muted-foreground/60" 
                required 
                />
            </div>
            <div className="space-y-1">
                <Label htmlFor="reg-password">Password</Label>
                <PasswordInput
                    id="reg-password"
                    value={regPassword}
                    placeholder="Meet all the requirements below"
                    onChange={(e) => setRegPassword(e.target.value)}
                    required
                    aria-invalid={!meetsPasswordPolicy(regPassword)}
                    aria-describedby="reg-password-requirements"
                    className={`placeholder:text-muted-foreground/60 ${meetsPasswordPolicy(regPassword)
                        ? "border-green-600 focus-visible:ring-green-600 dark:border-green-500"
                        : "border-black-600 focus-visible:ring-black-600 dark:border-black-500"}
                            `}
                />
                <ul id="reg-password-requirements" className="space-y-1 pt-1">
                    {passwordChecks.map((check) => (
                        <ValidationIndicator key={check.label} valid={check.valid}>
                            {check.label}
                        </ValidationIndicator>
                    ))}
                </ul>
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
            <Button type="submit" className="w-full" disabled={isSubmitting}>
                {isSubmitting ? <Spinner className="h-4 w-4" /> : "Create account"}
            </Button>
        </form>
    );
}
