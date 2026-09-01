"use client"

import {Label} from "@/components/ui/label";
import {Input} from "@/components/ui/input";
import {PasswordInput} from "@/components/ui/input-password";
import LlmBaseUrlDatalist from "@/components/datalist/llm-base-url-datalist";
import LlmModelNameDatalist from "@/components/datalist/llm-model-name-datalist";
import LlmApiKeyPlaceholderDatalist from "@/components/datalist/llm-api-key-placeholder-datalist";
import {GenerateRandomInput} from "@/components/ui/input-generate-random";
import {safeFloatOrNull} from "@/lib/evaluation-config-utils";
import {Switch} from "@/components/ui/switch";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select";
import {AnalysisSettings} from "@/hooks/use-analysis-settings";

interface AnalysisSettingsFieldsProps {
    settings: AnalysisSettings;
    isMulticlass: boolean;
    idPrefix?: string;
}

export default function AnalysisSettingsFields({settings, isMulticlass, idPrefix = ""}: AnalysisSettingsFieldsProps) {
    const {
        llmBaseUrl, setLlmBaseUrl,
        modelName, setModelName,
        apiKey, setApiKey,
        seed, setSeed,
        temperature, setTemperature,
        topP, setTopP,
        useRag, setUseRag,
        searchMode, setSearchMode,
    } = settings;

    return <div className="flex flex-col space-y-2">
        <div className="space-y-1">
            <Label>LLM Base URL</Label>
            <Input
                type="text"
                className="w-full"
                placeholder="https://api.openai.com/v1"
                value={llmBaseUrl}
                onChange={(e) => setLlmBaseUrl(e.target.value)}
                list={`${idPrefix}llm-base-urls`}
            />
            <LlmBaseUrlDatalist id={`${idPrefix}llm-base-urls`}/>
        </div>
        <div className="space-y-1">
            <Label>Model Name</Label>
            <Input
                type="text"
                className="w-full"
                placeholder="gpt-3.5-turbo-0125"
                value={modelName}
                onChange={(e) => setModelName(e.target.value)}
                list={`${idPrefix}llm-model-names`}
            />
            <LlmModelNameDatalist id={`${idPrefix}llm-model-names`}/>
        </div>
        <div className="space-y-1">
            <Label>API Key</Label>
            <PasswordInput
                className="w-full"
                placeholder="sk-..."
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                list={`${idPrefix}llm-api-key-placeholders`}
            />
            <LlmApiKeyPlaceholderDatalist id={`${idPrefix}llm-api-key-placeholders`}/>
        </div>
        <div className="space-y-1">
            <Label>Seed</Label>
            <GenerateRandomInput id={`${idPrefix}seed`} placeholder="Seed for reproducibility"
                                 length={8}
                                 value={seed || ""}
                                 alphabet={"0123456789"}
                                 onChange={(e) => setSeed(parseInt(e.target.value))}
                                 className="w-full"/>
        </div>
        <div className="space-y-2">
            <Label>Temperature</Label>
            <Input type="number" placeholder="1.0" value={temperature ?? ""}
                   onChange={(e) => setTemperature(safeFloatOrNull(e.target.value))}/>
        </div>
        <div className="space-y-2">
            <Label>Top P</Label>
            <Input type="number" placeholder="1.0" value={topP ?? ""}
                   onChange={(e) => setTopP(safeFloatOrNull(e.target.value))}/>
        </div>
        {!isMulticlass && (
            <>
                <div className="flex items-center space-x-2 pt-2">
                    <Switch
                        id={`${idPrefix}use-rag`}
                        checked={useRag}
                        onCheckedChange={setUseRag}
                    />
                    <Label htmlFor={`${idPrefix}use-rag`} className="cursor-pointer">Use RAG for the Analysis</Label>
                </div>
                {useRag && (
                    <div className="space-y-1 pt-2">
                        <Label>RAG Search Mode</Label>
                        <Select value={searchMode} onValueChange={setSearchMode}>
                            <SelectTrigger className="w-full">
                                <SelectValue placeholder="Select search mode"/>
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="naive">Naive</SelectItem>
                                <SelectItem value="local">Local</SelectItem>
                                <SelectItem value="global">Global</SelectItem>
                                <SelectItem value="hybrid">Hybrid</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>
                )}
            </>
        )}
    </div>
}
