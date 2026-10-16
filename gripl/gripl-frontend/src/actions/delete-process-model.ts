"use server"

import {authenticatedServerFetch} from "@/lib/authenticated-server-fetch";

export default async function deleteProcessModel(id: number) {

    const response = await authenticatedServerFetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/process-models/${id}`, {
        method: "DELETE",
    });

    if (!response.ok) {
        throw new Error(`Error deleting process model: ${response.statusText}`);
    }

    return;
}
