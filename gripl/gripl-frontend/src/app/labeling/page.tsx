import React from "react";
import {cookies} from "next/headers";
import {EvaluationDataMeta} from "@/models/dto/EvaluationData";
import DatasetList from "@/components/labeling/dataset-list";
import getDatasets from "@/actions/get-datasets";
import CreateDatasetButton from "@/components/labeling/create-dataset-button";
import {AUTH_COOKIE_NAME} from "@/lib/auth-cookie";

export default async function LabelingPage() {

    const token = (await cookies()).get(AUTH_COOKIE_NAME)?.value;
    const datasets = await getDatasets()
    const evaluationMetadata: EvaluationDataMeta[] = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/dataset/testcase`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            ...(token ? {Authorization: `Bearer ${token}`} : {}),
        },
    })
        .then(data => {
            console.log("Data", data);
            return data.json()
        })
        .catch(error => {
            console.log("There was an error fetching the dataset metadata:", error);
            return []
        })

    console.log("Evaluation Metadata", evaluationMetadata);
    console.log("Datasets", datasets);

    return <div className="h-full w-full p-6 overflow-y-auto">
        <div className="container mx-auto">
            <div className="flex flex-row justify-between items-start">
                <h2 className="font-bold text-3xl mb-6">Labeling Datasets</h2>
                <CreateDatasetButton />
            </div>
            <DatasetList datasets={datasets} evaluationMetadata={evaluationMetadata}/>
        </div>
    </div>
}