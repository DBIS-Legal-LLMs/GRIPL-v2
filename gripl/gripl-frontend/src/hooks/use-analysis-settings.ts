"use client"

import {useEffect, useState} from "react";
import {LlmPropsOverride} from "@/models/dto/MultiEvaluationRequest";

const STORAGE_KEY = "gripl.analysis.settings";

interface StoredAnalysisSettings {
    llmBaseUrl: string;
    modelName: string;
    apiKey: string;
    seed: number | null;
    temperature: number | null;
    topP: number | null;
    useRag: boolean;
    searchMode: string;
    configured: boolean;
}

const DEFAULT_SETTINGS: StoredAnalysisSettings = {
    llmBaseUrl: "",
    modelName: "",
    apiKey: "",
    seed: null,
    temperature: null,
    topP: null,
    useRag: false,
    searchMode: "hybrid",
    configured: false,
};

/**
 * Holds the LLM override + RAG settings used across the dashboard (settings
 * dialog, batch "Analyze Selected") and the single-model detail view.
 * Persisted to localStorage so the settings are configured once, up front,
 * rather than re-entered every time an analysis is started — `configured`
 * only flips to true once the user has explicitly saved the settings dialog.
 */
export function useAnalysisSettings() {
    const [llmBaseUrl, setLlmBaseUrl] = useState<string>(DEFAULT_SETTINGS.llmBaseUrl)
    const [modelName, setModelName] = useState<string>(DEFAULT_SETTINGS.modelName)
    const [apiKey, setApiKey] = useState<string>(DEFAULT_SETTINGS.apiKey)
    const [seed, setSeed] = useState<number | null>(DEFAULT_SETTINGS.seed)
    const [temperature, setTemperature] = useState<number | null>(DEFAULT_SETTINGS.temperature)
    const [topP, setTopP] = useState<number | null>(DEFAULT_SETTINGS.topP)
    const [useRag, setUseRag] = useState<boolean>(DEFAULT_SETTINGS.useRag)
    const [searchMode, setSearchMode] = useState<string>(DEFAULT_SETTINGS.searchMode)
    const [isConfigured, setIsConfigured] = useState<boolean>(DEFAULT_SETTINGS.configured)
    const [isLoaded, setIsLoaded] = useState<boolean>(false)

    useEffect(() => {
        try {
            const raw = window.localStorage.getItem(STORAGE_KEY)
            if (raw) {
                const stored = JSON.parse(raw) as Partial<StoredAnalysisSettings>
                if (stored.llmBaseUrl !== undefined) setLlmBaseUrl(stored.llmBaseUrl)
                if (stored.modelName !== undefined) setModelName(stored.modelName)
                if (stored.apiKey !== undefined) setApiKey(stored.apiKey)
                if (stored.seed !== undefined) setSeed(stored.seed)
                if (stored.temperature !== undefined) setTemperature(stored.temperature)
                if (stored.topP !== undefined) setTopP(stored.topP)
                if (stored.useRag !== undefined) setUseRag(stored.useRag)
                if (stored.searchMode !== undefined) setSearchMode(stored.searchMode)
                if (stored.configured !== undefined) setIsConfigured(stored.configured)
            }
        } catch (error) {
            console.error("Error reading analysis settings from local storage:", error)
        } finally {
            setIsLoaded(true)
        }
    }, [])

    function persist(next: Partial<StoredAnalysisSettings>) {
        try {
            const current: StoredAnalysisSettings = {
                llmBaseUrl, modelName, apiKey, seed, temperature, topP, useRag, searchMode,
                configured: isConfigured,
                ...next,
            }
            window.localStorage.setItem(STORAGE_KEY, JSON.stringify(current))
        } catch (error) {
            console.error("Error saving analysis settings to local storage:", error)
        }
    }

    function save() {
        setIsConfigured(true)
        persist({configured: true})
    }

    function buildEnqueueParams() {
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
            useRag,
            ragMode: useRag ? searchMode : undefined,
        }
    }

    return {
        llmBaseUrl, setLlmBaseUrl: (v: string) => { setLlmBaseUrl(v); persist({llmBaseUrl: v}) },
        modelName, setModelName: (v: string) => { setModelName(v); persist({modelName: v}) },
        apiKey, setApiKey: (v: string) => { setApiKey(v); persist({apiKey: v}) },
        seed, setSeed: (v: number | null) => { setSeed(v); persist({seed: v}) },
        temperature, setTemperature: (v: number | null) => { setTemperature(v); persist({temperature: v}) },
        topP, setTopP: (v: number | null) => { setTopP(v); persist({topP: v}) },
        useRag, setUseRag: (v: boolean) => { setUseRag(v); persist({useRag: v}) },
        searchMode, setSearchMode: (v: string) => { setSearchMode(v); persist({searchMode: v}) },
        isConfigured,
        isLoaded,
        save,
        buildEnqueueParams,
    }
}

export type AnalysisSettings = ReturnType<typeof useAnalysisSettings>;
