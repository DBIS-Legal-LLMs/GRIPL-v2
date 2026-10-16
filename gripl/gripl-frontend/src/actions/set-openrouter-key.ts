"use server"

import {authenticatedServerFetch} from "@/lib/authenticated-server-fetch";

export default async function setOpenRouterKey(apiKey: string | null): Promise<void> {
    const response = await authenticatedServerFetch(`${process.env.AUTH_SERVICE_INTERNAL_URL}/users/me`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({openrouter_api_key: apiKey}),
    });

    if (!response.ok) {
        throw new Error(`Failed to update OpenRouter API key: ${response.statusText}`);
    }
}
