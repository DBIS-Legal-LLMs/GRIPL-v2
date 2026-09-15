"use server"

import {EnqueueAnalysisRequest, EnqueueAnalysisResponse} from "@/models/dto/ProcessModel";

export default async function analyzeProcessModels(request: EnqueueAnalysisRequest): Promise<EnqueueAnalysisResponse> {
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/process-models/analyze`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(request),
    });

    if (!response.ok) {
        throw new Error(`Failed to enqueue analysis: ${response.statusText}`);
    }

    return await response.json() as EnqueueAnalysisResponse;
}
