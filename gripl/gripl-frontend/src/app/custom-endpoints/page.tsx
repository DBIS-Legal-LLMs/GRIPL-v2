import React from "react";
import getCustomEndpoints from "@/actions/get-custom-endpoints";
import getDefaultPrompts from "@/actions/get-default-prompts";
import CustomEndpointList from "@/components/custom-endpoints/custom-endpoint-list";
import BuiltInPromptList from "@/components/custom-endpoints/built-in-prompt-list";
import CreateCustomEndpointButton from "@/components/custom-endpoints/create-custom-endpoint-button";

export default async function CustomEndpointsPage() {
    const [endpoints, defaultPrompts] = await Promise.all([
        getCustomEndpoints(),
        getDefaultPrompts(),
    ]);

    return <div className="h-full w-full p-6 overflow-y-auto">
        <div className="container mx-auto space-y-8">
            <div className="flex flex-row justify-between items-start">
                <h2 className="font-bold text-3xl">Custom Analysis Endpoints</h2>
                <CreateCustomEndpointButton/>
            </div>
            <CustomEndpointList endpoints={endpoints}/>

            <div>
                <h3 className="font-semibold text-xl mb-1">Built-in Endpoints</h3>
                <p className="text-sm text-muted-foreground mb-4">
                    Read-only — these ship with GRIPL and can&apos;t be edited or deleted.
                </p>
                <BuiltInPromptList prompts={defaultPrompts}/>
            </div>
        </div>
    </div>
}
