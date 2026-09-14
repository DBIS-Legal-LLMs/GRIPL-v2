"use server"

import {cookies} from "next/headers";
import {AUTH_COOKIE_NAME} from "@/lib/auth-cookie";

export default async function deleteDataset(datasetId: number) {

    const token = (await cookies()).get(AUTH_COOKIE_NAME)?.value;
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/dataset/${datasetId}`, {
        method: "DELETE",
        headers: token ? {Authorization: `Bearer ${token}`} : {},
    });

    if (!response.ok) {
        throw new Error(`Error deleting dataset: ${response.statusText}`);
    }

    return;
}