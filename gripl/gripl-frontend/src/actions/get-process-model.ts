"use server"

import {ProcessModelDetail} from "@/models/dto/ProcessModel";

export default async function getProcessModel(id: number): Promise<ProcessModelDetail | null> {
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/process-models/${id}`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
        },
        cache: "no-store"
    }).catch(error => {
        console.error(`There was an error fetching process model ${id}:`, error);
        return null
    })

    if (!response || !response.ok) {
        return null;
    }

    return await response.json() as ProcessModelDetail;
}
