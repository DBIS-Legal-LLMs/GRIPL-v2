"use server"

import {ProcessModelListItem} from "@/models/dto/ProcessModel";
import {extractErrorDetails} from "@/lib/http-error";
import {authenticatedServerFetch} from "@/lib/authenticated-server-fetch";

export default async function renameProcessModel(id: number, name: string): Promise<ProcessModelListItem> {
    const response = await authenticatedServerFetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/process-models/${id}/name`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ name }),
    });

    if (!response.ok) {
        throw new Error(await extractErrorDetails(response));
    }

    return response.json() as Promise<ProcessModelListItem>;
}
