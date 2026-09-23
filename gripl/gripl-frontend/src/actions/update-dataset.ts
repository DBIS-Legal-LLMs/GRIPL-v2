"use server"

import {Dataset} from "@/models/dto/Dataset";
import {extractErrorDetails} from "@/lib/http-error";

export default async function updateDataset(datasetId: number, name: string, description: string): Promise<Dataset> {
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/dataset/${datasetId}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ name, description }),
    });

    if (!response.ok) {
        throw new Error(await extractErrorDetails(response));
    }

    return response.json() as Promise<Dataset>;
}
