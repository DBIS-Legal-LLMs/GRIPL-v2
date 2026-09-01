"use client"

import React, {createContext, ReactNode, useCallback, useContext, useState} from "react";
import emptyDiagram from "@/data/empty-diagram.bpmn";
import {AnalysisResponse} from "@/models/dto/AnalysisDto";
import {useToast} from "@/components/ui/toast";
import {extractErrorDetails, toErrorMessage} from "@/lib/http-error";

interface AnalysisJobContextValue {
    diagram: string;
    setDiagram: (xml: string) => void;
    analysisResult: AnalysisResponse | null;
    setAnalysisResult: (result: AnalysisResponse | null) => void;
    isAnalyzing: boolean;
    startAnalysis: (apiEndpoint: string, formData: FormData) => void;
}

const AnalysisJobContext = createContext<AnalysisJobContextValue | null>(null);

/**
 * Owns the sandbox's loaded diagram, analysis in-flight flag, and result so an
 * analysis keeps running (and its result is still there) even if the user
 * navigates elsewhere and back — mounted at the root layout, so unlike the
 * page component it never unmounts on route change.
 */
export function AnalysisJobProvider({children}: { children: ReactNode }) {
    const [diagram, setDiagram] = useState<string>(emptyDiagram as string);
    const [analysisResult, setAnalysisResult] = useState<AnalysisResponse | null>(null);
    const [isAnalyzing, setIsAnalyzing] = useState<boolean>(false);
    const {showError} = useToast();

    const startAnalysis = useCallback((apiEndpoint: string, formData: FormData) => {
        setAnalysisResult(null);
        setIsAnalyzing(true);

        fetch(apiEndpoint, {
            method: "POST",
            headers: {
                Accept: "application/json"
            },
            body: formData
        } as RequestInit).then(async response => {
            if (!response.ok) {
                const details = await extractErrorDetails(response);
                throw new Error(details);
            }
            return response.json();
        }).then((data: AnalysisResponse) => {
            console.log("Analysis complete:", data);
            setIsAnalyzing(false);
            setAnalysisResult(data);
        }).catch(error => {
            console.error("Error during analysis:", error);
            setIsAnalyzing(false);
            showError("Failed to analyze the diagram", toErrorMessage(error));
        });
    }, [showError]);

    const value: AnalysisJobContextValue = {
        diagram,
        setDiagram,
        analysisResult,
        setAnalysisResult,
        isAnalyzing,
        startAnalysis,
    };

    return <AnalysisJobContext.Provider value={value}>{children}</AnalysisJobContext.Provider>;
}

export function useAnalysisJob() {
    const context = useContext(AnalysisJobContext);
    if (!context) {
        throw new Error("useAnalysisJob must be used within AnalysisJobProvider");
    }
    return context;
}
