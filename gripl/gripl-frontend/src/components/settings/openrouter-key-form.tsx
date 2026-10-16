"use client"

import {useState} from "react";
import {Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter} from "@/components/ui/card";
import {Label} from "@/components/ui/label";
import {PasswordInput} from "@/components/ui/input-password";
import {Button} from "@/components/ui/button";
import {Spinner} from "@/components/ui/spinner";
import {useToast} from "@/components/ui/toast";
import setOpenRouterKey from "@/actions/set-openrouter-key";

interface OpenRouterKeyFormProps {
    initialKey: string | null;
}

export default function OpenRouterKeyForm({initialKey}: OpenRouterKeyFormProps) {
    const [apiKey, setApiKey] = useState(initialKey ?? "");
    const [isSaving, setIsSaving] = useState(false);
    const {showToast, showError} = useToast();

    async function handleSave() {
        setIsSaving(true);
        try {
            await setOpenRouterKey(apiKey.trim() || null);
            showToast({
                title: apiKey.trim() ? "OpenRouter API key saved" : "OpenRouter API key cleared",
                variant: "success",
            });
        } catch (error) {
            console.error("Error saving OpenRouter API key:", error);
            showError("Failed to save your OpenRouter API key", error instanceof Error ? error.message : undefined);
        } finally {
            setIsSaving(false);
        }
    }

    return <Card>
        <CardHeader>
            <CardTitle>OpenRouter API Key</CardTitle>
            <CardDescription>
                Every analysis and evaluation you run is billed to this key — GRIPL
                never uses a shared key on your behalf. Get one at{" "}
                <a href="https://openrouter.ai/keys" target="_blank" rel="noopener noreferrer" className="underline">
                    openrouter.ai/keys
                </a>.
            </CardDescription>
        </CardHeader>
        <CardContent className="space-y-1">
            <Label htmlFor="openrouter-api-key">API Key</Label>
            <PasswordInput
                id="openrouter-api-key"
                className="w-full"
                placeholder="sk-or-v1-..."
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
            />
        </CardContent>
        <CardFooter>
            <Button onClick={handleSave} disabled={isSaving}>
                {isSaving ? <Spinner size="small" className="mr-2 h-4 w-4"/> : null}
                Save
            </Button>
        </CardFooter>
    </Card>
}
