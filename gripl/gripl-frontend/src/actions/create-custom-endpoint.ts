"use server"

import {CustomAnalysisEndpoint, CustomAnalysisResponseType} from "@/models/dto/CustomAnalysisEndpoint";

export default async function createCustomEndpoint(
    name: string,
    promptText: string,
    responseType: CustomAnalysisResponseType,
    ragEnabled: boolean,
    ragMode?: string
): Promise<CustomAnalysisEndpoint> {
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/custom-analysis-endpoints`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({name, promptText, responseType, ragEnabled, ragMode}),
    });

    if (!response.ok) {
        throw new Error(`Failed to create custom analysis endpoint: ${response.statusText}`);
    }

    return await response.json() as CustomAnalysisEndpoint;
}
