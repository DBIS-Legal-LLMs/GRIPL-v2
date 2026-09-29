"use server"

import {authenticatedServerFetch} from "@/lib/authenticated-server-fetch";

export default async function getOpenRouterKey(): Promise<string | null> {
    const result = await authenticatedServerFetch(`${process.env.AUTH_SERVICE_INTERNAL_URL}/users/me`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
        },
        cache: "no-store"
    })
        .then(async data => {
            if (!data.ok) {
                throw new Error(`Failed to fetch profile: ${data.statusText}`);
            }
            return await data.json()
        })
        .catch(error => {
            console.error("There was an error fetching the OpenRouter API key:", error);
            return null
        })

    return result?.openrouter_api_key ?? null;
}
