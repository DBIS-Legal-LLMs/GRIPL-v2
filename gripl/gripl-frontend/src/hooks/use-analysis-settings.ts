"use client"

import {useState} from "react";
import {LlmPropsOverride} from "@/models/dto/MultiEvaluationRequest";

/**
 * Holds the LLM override + RAG settings shared by the single-model tool card
 * and the batch "Analyze Selected" dialog, so both build the exact same
 * enqueue payload shape from the same fields.
 */
export function useAnalysisSettings() {
    const [llmBaseUrl, setLlmBaseUrl] = useState<string>("")
    const [modelName, setModelName] = useState<string>("")
    const [apiKey, setApiKey] = useState<string>("")
    const [seed, setSeed] = useState<number | null>(null)
    const [temperature, setTemperature] = useState<number | null>(null)
    const [topP, setTopP] = useState<number | null>(null)
    const [useRag, setUseRag] = useState<boolean>(false)
    const [searchMode, setSearchMode] = useState<string>("hybrid")

    function buildEnqueueParams(isMulticlass: boolean) {
        const llmProps = {
            baseUrl: llmBaseUrl || null,
            modelName: modelName || null,
            apiKey: apiKey || null,
            seed: seed || null,
            temperature: temperature || null,
            topP: topP || null
        } as LlmPropsOverride;

        return {
            llmProps,
            useRag: isMulticlass ? undefined : useRag,
            ragMode: isMulticlass || !useRag ? undefined : searchMode,
        }
    }

    return {
        llmBaseUrl, setLlmBaseUrl,
        modelName, setModelName,
        apiKey, setApiKey,
        seed, setSeed,
        temperature, setTemperature,
        topP, setTopP,
        useRag, setUseRag,
        searchMode, setSearchMode,
        buildEnqueueParams,
    }
}

export type AnalysisSettings = ReturnType<typeof useAnalysisSettings>;
