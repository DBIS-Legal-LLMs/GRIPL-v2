"use client"

import React, {createContext, ReactNode, useContext, useEffect, useMemo, useState} from "react";
import {AnalysisEndpoint} from "@/models/evaluation/Config";

type AnalysisEndpointMode = "binary" | "multiclass";

interface AnalysisEndpointContextValue {
    mode: AnalysisEndpointMode;
    setMode: (mode: AnalysisEndpointMode) => void;
    selectedEndpoint: string;
    setSelectedEndpoint: (endpoint: string) => void;
    availableEndpoints: AnalysisEndpoint[];
    apiEndpoint: string;
    backendEndpoint: string;
    isMulticlass: boolean;
}

const STORAGE_KEY = "gripl.analysis.endpoint.selected";
const DEFAULT_BINARY_ENDPOINT = "/gdpr/analysis/prompt-engineering";
const DEFAULT_MULTICLASS_ENDPOINT = "/gdpr/analysis/multiclass";
const FALLBACK_ENDPOINTS: AnalysisEndpoint[] = [
    { name: "Preprocessing & Prompt Engineering Analysis", endpoint: DEFAULT_BINARY_ENDPOINT },
    { name: "Baseline Analysis", endpoint: "/gdpr/analysis/baseline" },
    { name: "Multiclass Analysis", endpoint: DEFAULT_MULTICLASS_ENDPOINT },
];

const AnalysisEndpointContext = createContext<AnalysisEndpointContextValue | null>(null);

export function AnalysisEndpointProvider({children}: { children: ReactNode }) {
    const [availableEndpoints, setAvailableEndpoints] = useState<AnalysisEndpoint[]>([]);
    const [selectedEndpoint, setSelectedEndpoint] = useState<string>(DEFAULT_BINARY_ENDPOINT);

    const isMulticlass = selectedEndpoint.toLowerCase().includes("multiclass");
    const mode: AnalysisEndpointMode = isMulticlass ? "multiclass" : "binary";

    const setMode = (nextMode: AnalysisEndpointMode) => {
        const preferred = nextMode === "multiclass"
            ? DEFAULT_MULTICLASS_ENDPOINT
            : DEFAULT_BINARY_ENDPOINT;

        if (availableEndpoints.length === 0) {
            setSelectedEndpoint(preferred);
            return;
        }

        if (nextMode === "multiclass") {
            const multiclassMatch = availableEndpoints.find((endpoint) =>
                endpoint.endpoint.toLowerCase().includes("multiclass")
            );
            setSelectedEndpoint(multiclassMatch?.endpoint ?? preferred);
            return;
        }

        const promptMatch = availableEndpoints.find((endpoint) =>
            endpoint.endpoint === DEFAULT_BINARY_ENDPOINT
        );
        const nonMulticlass = availableEndpoints.find((endpoint) =>
            !endpoint.endpoint.toLowerCase().includes("multiclass")
        );
        setSelectedEndpoint(promptMatch?.endpoint ?? nonMulticlass?.endpoint ?? preferred);
    };

    useEffect(() => {
        fetch("/api/gdpr/analysis/endpoints")
            .then(async (response) => {
                if (!response.ok) {
                    throw new Error("Failed to load analysis endpoints");
                }
                return (await response.json()) as AnalysisEndpoint[];
            })
            .then((endpoints) => {
                if (!Array.isArray(endpoints) || endpoints.length === 0) {
                    setAvailableEndpoints(FALLBACK_ENDPOINTS);
                    return;
                }
                setAvailableEndpoints(endpoints);
            })
            .catch(() => {
                setAvailableEndpoints(FALLBACK_ENDPOINTS);
            });
    }, []);

    useEffect(() => {
        const storedEndpoint = window.localStorage.getItem(STORAGE_KEY);
        if (storedEndpoint) {
            setSelectedEndpoint(storedEndpoint);
        }
    }, []);

    useEffect(() => {
        if (availableEndpoints.length === 0) {
            return;
        }

        const hasSelected = availableEndpoints.some((endpoint) => endpoint.endpoint === selectedEndpoint);
        if (hasSelected) {
            return;
        }

        const fallback = availableEndpoints.find((endpoint) =>
            endpoint.endpoint === DEFAULT_BINARY_ENDPOINT
        )?.endpoint ?? availableEndpoints[0].endpoint;
        setSelectedEndpoint(fallback);
    }, [availableEndpoints, selectedEndpoint]);

    useEffect(() => {
        window.localStorage.setItem(STORAGE_KEY, selectedEndpoint);
    }, [selectedEndpoint]);

    const apiEndpoint = selectedEndpoint.startsWith("/gdpr/")
        ? `/api${selectedEndpoint}`
        : selectedEndpoint;

    const value = useMemo<AnalysisEndpointContextValue>(() => {
        return {
            mode,
            setMode,
            selectedEndpoint,
            setSelectedEndpoint,
            availableEndpoints,
            isMulticlass,
            apiEndpoint,
            backendEndpoint: selectedEndpoint,
        };
    }, [mode, selectedEndpoint, availableEndpoints, isMulticlass, apiEndpoint]);

    return <AnalysisEndpointContext.Provider value={value}>{children}</AnalysisEndpointContext.Provider>;
}

export function useAnalysisEndpoint() {
    const context = useContext(AnalysisEndpointContext);
    if (!context) {
        throw new Error("useAnalysisEndpoint must be used within AnalysisEndpointProvider");
    }
    return context;
}
