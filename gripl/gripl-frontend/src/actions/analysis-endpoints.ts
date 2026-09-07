"use server"

import { cookies } from "next/headers";
import { AUTH_COOKIE_NAME } from "@/lib/auth-cookie";

export default async function fetchAnalysisEndpoints(): Promise<AnalysisEndpoint[]> {
    const token = (await cookies()).get(AUTH_COOKIE_NAME)?.value;
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/gdpr/analysis/endpoints`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
    if (!response.ok) {
        throw new Error(`Failed to fetch analysis endpoints: ${response.status} ${response.statusText}`);
    }
    return await response.json();
}