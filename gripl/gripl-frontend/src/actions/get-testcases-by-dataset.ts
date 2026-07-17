"use server"

import { EvaluationDataMeta } from "@/models/dto/EvaluationData";
import { extractErrorDetails, toErrorMessage } from "@/lib/http-error";

export default async function getTestcasesByDataset(datasetId: number): Promise<EvaluationDataMeta[]> {
    try {
        const res = await fetch(
            `${process.env.NEXT_PUBLIC_API_BASE_URL}/dataset/testcase?datasetId=${datasetId}`,
            {
                method: "GET",
                headers: { "Content-Type": "application/json" },
                cache: "no-store",
            }
        );
        if (!res.ok) {
            throw new Error(await extractErrorDetails(res));
        }
        return (await res.json()) as EvaluationDataMeta[];
    } catch (error) {
        console.error("Error fetching testcases:", toErrorMessage(error));
        return [];
    }
}
