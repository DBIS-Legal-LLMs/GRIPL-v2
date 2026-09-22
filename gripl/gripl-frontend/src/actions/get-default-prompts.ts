"use server"

import {DefaultAnalysisPrompt} from "@/models/dto/DefaultAnalysisPrompt";

export default async function getDefaultPrompts(): Promise<DefaultAnalysisPrompt[]> {
    const result = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/gdpr/analysis/default-prompts`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
        },
        cache: "no-store"
    })
        .then(data => {
            if (!data.ok) {
                throw new Error(`Failed to fetch default prompts: ${data.statusText}`);
            }
            return data.json()
        })
        .catch(error => {
            console.error("There was an error fetching the default prompts:", error);
            return []
        })

    return result as DefaultAnalysisPrompt[];
}
