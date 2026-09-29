import React from "react";
import getOpenRouterKey from "@/actions/get-openrouter-key";
import OpenRouterKeyForm from "@/components/settings/openrouter-key-form";

export default async function SettingsPage() {
    const initialKey = await getOpenRouterKey();

    return <div className="h-full w-full p-6 overflow-y-auto">
        <div className="container mx-auto max-w-xl space-y-8">
            <h2 className="font-bold text-3xl">Account Settings</h2>
            <OpenRouterKeyForm initialKey={initialKey}/>
        </div>
    </div>
}
