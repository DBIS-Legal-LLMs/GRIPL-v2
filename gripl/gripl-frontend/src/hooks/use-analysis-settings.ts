"use client"

import {useEffect, useState} from "react";
import {LlmPropsOverride} from "@/models/dto/MultiEvaluationRequest";
import {authenticatedFetch} from "@/lib/authenticated-fetch";
import {useAuth} from "@/context/auth-context";

// Settings used to live in localStorage under this key — shared by every user
// of the browser, and it held a plaintext API key. Cleared on first load now.
const LEGACY_STORAGE_KEY = "gripl.analysis.settings";

interface StoredAnalysisSettings {
    llmBaseUrl: string;
    modelName: string;
    seed: number | null;
    temperature: number;
    topP: number;
    useRag: boolean;
    ragMode: string;
    configured: boolean;
}

/**
 * Holds the LLM override + RAG settings used across the dashboard (settings
 * dialog, batch "Analyze Selected") and the single-model detail view.
 * Stored per user in the GRIPL backend and loaded once the user is logged in,
 * so they follow the account rather than the browser. Edits stay local until
 * `save()` — `configured` is true once the user has saved at least once.
 */
export function useAnalysisSettings() {
    const {token, isLoading: isAuthLoading} = useAuth()
    const [llmBaseUrl, setLlmBaseUrl] = useState<string>("")
    const [modelName, setModelName] = useState<string>("")
    const [seed, setSeed] = useState<number | null>(null)
    const [temperature, setTemperature] = useState<number | null>(null)
    const [topP, setTopP] = useState<number | null>(null)
    const [useRag, setUseRag] = useState<boolean>(false)
    const [searchMode, setSearchMode] = useState<string>("hybrid")
    const [isConfigured, setIsConfigured] = useState<boolean>(false)
    const [isLoaded, setIsLoaded] = useState<boolean>(false)

    // The backend fills in defaults (base URL, model, temperature, top-p) for
    // anything left empty, so always show back what it actually stored.
    function applyStored(stored: StoredAnalysisSettings) {
        setLlmBaseUrl(stored.llmBaseUrl)
        setModelName(stored.modelName)
        setSeed(stored.seed ?? null)
        setTemperature(stored.temperature)
        setTopP(stored.topP)
        setUseRag(stored.useRag)
        setSearchMode(stored.ragMode)
        setIsConfigured(stored.configured)
    }

    useEffect(() => {
        try {
            window.localStorage.removeItem(LEGACY_STORAGE_KEY)
        } catch {
            // storage unavailable — nothing to clear
        }
    }, [])

    useEffect(() => {
        if (isAuthLoading) return
        // Logged out (or a different user logging in): never show the previous
        // user's values.
        setIsLoaded(false)
        setLlmBaseUrl("")
        setModelName("")
        setSeed(null)
        setTemperature(null)
        setTopP(null)
        setUseRag(false)
        setSearchMode("hybrid")
        setIsConfigured(false)
        if (!token) return

        let cancelled = false
        authenticatedFetch("/api/analysis-settings")
            .then(async (response) => {
                if (!response.ok) throw new Error(`Failed to load analysis settings: ${response.status}`)
                return await response.json() as StoredAnalysisSettings
            })
            .then((stored) => {
                if (cancelled) return
                applyStored(stored)
            })
            .catch((error) => {
                console.error("Error loading analysis settings:", error)
            })
            .finally(() => {
                if (!cancelled) setIsLoaded(true)
            })
        return () => {
            cancelled = true
        }
    }, [token, isAuthLoading])

    async function save() {
        const response = await authenticatedFetch("/api/analysis-settings", {
            method: "PUT",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                llmBaseUrl: llmBaseUrl || null,
                modelName: modelName || null,
                seed,
                temperature,
                topP,
                useRag,
                ragMode: searchMode,
            }),
        })
        if (!response.ok) {
            throw new Error(`Failed to save analysis settings: ${response.statusText || response.status}`)
        }
        applyStored(await response.json() as StoredAnalysisSettings)
    }

    function buildEnqueueParams() {
        // The OpenRouter key is not sent from here: the backend fetches the
        // caller's own key from auth-service (Account Settings).
        const llmProps = {
            baseUrl: llmBaseUrl || null,
            modelName: modelName || null,
            seed: seed || null,
            temperature: temperature ?? null,
            topP: topP ?? null
        } as LlmPropsOverride;

        return {
            llmProps,
            useRag,
            ragMode: useRag ? searchMode : undefined,
        }
    }

    return {
        llmBaseUrl, setLlmBaseUrl,
        modelName, setModelName,
        seed, setSeed,
        temperature, setTemperature,
        topP, setTopP,
        useRag, setUseRag,
        searchMode, setSearchMode,
        isConfigured,
        isLoaded,
        save,
        buildEnqueueParams,
    }
}

export type AnalysisSettings = ReturnType<typeof useAnalysisSettings>;
