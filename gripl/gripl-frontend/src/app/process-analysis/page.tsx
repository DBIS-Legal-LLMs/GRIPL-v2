import React from "react";
import getProcessModels from "@/actions/get-process-models";
import ProcessModelList from "@/components/process-analysis/process-model-list";

export default async function ProcessAnalysisPage() {
    const models = await getProcessModels();

    return <div className="h-full w-full p-6 overflow-y-auto">
        <div className="container mx-auto">
            <h2 className="font-bold text-3xl mb-6">Process Analysis</h2>
            <ProcessModelList initialModels={models}/>
        </div>
    </div>
}
