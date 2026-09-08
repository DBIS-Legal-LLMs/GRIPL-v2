"use server"

import {EvaluationDataMeta} from "@/models/dto/EvaluationData";
import {extractErrorDetails} from "@/lib/http-error";

export default async function updateTestcaseName(testcaseId: number, name: string): Promise<EvaluationDataMeta> {
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/dataset/testcase/${testcaseId}/name`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ name }),
    });

    if (!response.ok) {
        throw new Error(await extractErrorDetails(response));
    }

    return response.json() as Promise<EvaluationDataMeta>;
}
