"use server"

import {ProcessModelListItem} from "@/models/dto/ProcessModel";

export default async function getProcessModels(): Promise<ProcessModelListItem[]> {
    const result = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/process-models`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
        },
        cache: "no-store"
    })
        .then(data => {
            if (!data.ok) {
                throw new Error(`Failed to fetch process models: ${data.statusText}`);
            }
            return data.json()
        })
        .catch(error => {
            console.error("There was an error fetching the process models:", error);
            return []
        })

    return result as ProcessModelListItem[];
}
