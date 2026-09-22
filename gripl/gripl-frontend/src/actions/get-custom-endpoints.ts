"use server"

import {CustomAnalysisEndpoint} from "@/models/dto/CustomAnalysisEndpoint";

export default async function getCustomEndpoints(): Promise<CustomAnalysisEndpoint[]> {
    const result = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/custom-analysis-endpoints`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
        },
        cache: "no-store"
    })
        .then(data => {
            if (!data.ok) {
                throw new Error(`Failed to fetch custom analysis endpoints: ${data.statusText}`);
            }
            return data.json()
        })
        .catch(error => {
            console.error("There was an error fetching the custom analysis endpoints:", error);
            return []
        })

    return result as CustomAnalysisEndpoint[];
}
