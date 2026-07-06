"use client"

import React, {createContext, ReactNode, useContext, useEffect, useMemo, useState} from "react";

type AnalysisEndpointMode = "binary" | "multiclass";

interface AnalysisEndpointContextValue {
    mode: AnalysisEndpointMode;
    setMode: (mode: AnalysisEndpointMode) => void;
    apiEndpoint: string;
    backendEndpoint: string;
    isMulticlass: boolean;
}

const STORAGE_KEY = "gripl.analysis.endpoint.mode";

const AnalysisEndpointContext = createContext<AnalysisEndpointContextValue | null>(null);

export function AnalysisEndpointProvider({children}: { children: ReactNode }) {
    const [mode, setMode] = useState<AnalysisEndpointMode>("binary");

    useEffect(() => {
        const storedMode = window.localStorage.getItem(STORAGE_KEY);
        if (storedMode === "binary" || storedMode === "multiclass") {
            setMode(storedMode);
        }
    }, []);

    useEffect(() => {
        window.localStorage.setItem(STORAGE_KEY, mode);
    }, [mode]);

    const value = useMemo<AnalysisEndpointContextValue>(() => {
        const isMulticlass = mode === "multiclass";
        return {
            mode,
            setMode,
            isMulticlass,
            apiEndpoint: isMulticlass
                ? "/api/gdpr/analysis/multiclass"
                : "/api/gdpr/analysis/prompt-engineering",
            backendEndpoint: isMulticlass
                ? "/gdpr/analysis/multiclass"
                : "/gdpr/analysis/prompt-engineering",
        };
    }, [mode]);

    return <AnalysisEndpointContext.Provider value={value}>{children}</AnalysisEndpointContext.Provider>;
}

export function useAnalysisEndpoint() {
    const context = useContext(AnalysisEndpointContext);
    if (!context) {
        throw new Error("useAnalysisEndpoint must be used within AnalysisEndpointProvider");
    }
    return context;
}
