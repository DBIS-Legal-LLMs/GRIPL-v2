"use client"

import React, {createContext, ReactNode, useCallback, useContext, useEffect, useState} from "react";
import {
    EvaluationMetadataReport,
    EvaluationReport,
    EvaluationReportError,
    EvaluationReportStepInfo,
    EvaluationReportSummary,
    TestCaseReport
} from "@/models/dto/ReportData";
import {MultiEvaluationRequest} from "@/models/dto/MultiEvaluationRequest";

type ModelReportEnvelope = {
    modelLabel: string;
    report: EvaluationReport;
    runNumber: number;
};

type TestCasesByRun = Map<number, (TestCaseReport & { modelLabel: string })[]>;
type SummaryByRun = Map<number, Map<string, EvaluationReportSummary>>;
type ErrorsByRun = Map<number, (EvaluationReportError & { modelLabel: string })[]>;
type StepInfo = EvaluationReportStepInfo & { modelLabel: string; runNumber: number };

interface EvaluationJobContextValue {
    evaluationRequest: MultiEvaluationRequest | null;
    setEvaluationRequest: (request: MultiEvaluationRequest | null) => void;
    metadata: EvaluationMetadataReport | null;
    setMetadata: (metadata: EvaluationMetadataReport | null) => void;
    testCasesByRun: TestCasesByRun;
    setTestCasesByRun: React.Dispatch<React.SetStateAction<TestCasesByRun>>;
    summaryByRun: SummaryByRun;
    setSummaryByRun: React.Dispatch<React.SetStateAction<SummaryByRun>>;
    currentStepInfos: StepInfo[];
    errorsByRun: ErrorsByRun;
    setErrorsByRun: React.Dispatch<React.SetStateAction<ErrorsByRun>>;
    isLoading: boolean;
    isFinished: boolean;
    setIsFinished: (finished: boolean) => void;
    startEvaluation: () => Promise<void>;
}

const EvaluationJobContext = createContext<EvaluationJobContextValue | null>(null);

/**
 * Owns the evaluation run's state and the NDJSON stream-reading loop so a run
 * started on the evaluation page keeps accumulating results even if the user
 * navigates elsewhere and back — mounted at the root layout, so unlike the
 * page component it never unmounts on route change.
 */
export function EvaluationJobProvider({children}: { children: ReactNode }) {
    const [evaluationRequest, setEvaluationRequest] = useState<MultiEvaluationRequest | null>(null);

    const [metadata, setMetadata] = useState<EvaluationMetadataReport | null>(null);
    const [testCasesByRun, setTestCasesByRun] = useState<TestCasesByRun>(new Map());
    const [summaryByRun, setSummaryByRun] = useState<SummaryByRun>(new Map());
    const [currentStepInfos, setCurrentStepInfos] = useState<StepInfo[]>([]);
    const [errorsByRun, setErrorsByRun] = useState<ErrorsByRun>(new Map());
    const [isLoading, setIsLoading] = useState(false);
    const [isFinished, setIsFinished] = useState(false);

    const processNdjsonStream = useCallback(async (res: Response) => {
        if (!res.ok || !res.body) {
            console.error("Request failed:", res.status, res.statusText);
            setIsLoading(false);
            return;
        }

        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";

        while (true) {
            const {done, value} = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, {stream: true});
            const lines = buffer.split("\n");

            for (let i = 0; i < lines.length - 1; i++) {
                const line = lines[i].trim();
                if (!line) continue;

                try {
                    const env = JSON.parse(line) as ModelReportEnvelope;
                    const {modelLabel, report, runNumber} = env;

                    if (report.type === "metadata") {
                        setMetadata(report);
                    } else if (report.type === "testCase") {
                        setTestCasesByRun((prev) => {
                            const next = new Map(prev);
                            const runCases = next.get(runNumber) || [];
                            next.set(runNumber, [...runCases, {...(report as TestCaseReport), modelLabel}]);
                            return next;
                        });
                    } else if (report.type === "summary") {
                        setSummaryByRun((prev) => {
                            const next = new Map(prev);
                            const runSummaries = next.get(runNumber) || new Map();
                            runSummaries.set(modelLabel, report as EvaluationReportSummary);
                            next.set(runNumber, runSummaries);
                            return next;
                        });
                    } else if (report.type === "stepInfo") {
                        setCurrentStepInfos((prev) => [...prev, {...(report as EvaluationReportStepInfo), modelLabel, runNumber}]);
                    } else if (report.type === "error") {
                        setErrorsByRun((prev) => {
                            const next = new Map(prev);
                            const runErrors = next.get(runNumber) || [];
                            next.set(runNumber, [...runErrors, {...(report as EvaluationReportError), modelLabel}]);
                            return next;
                        });
                    } else {
                        console.warn("Unknown report type:", report);
                    }
                } catch (e) {
                    console.error("Failed to parse NDJSON line:", e);
                }
            }

            buffer = lines[lines.length - 1];
        }

        setIsLoading(false);
        setIsFinished(true);
    }, []);

    const resetState = useCallback(() => {
        setMetadata(null);
        setTestCasesByRun(new Map());
        setSummaryByRun(new Map());
        setCurrentStepInfos([]);
        setErrorsByRun(new Map());
        setIsLoading(true);
        setIsFinished(false);
    }, []);

    const startEvaluation = useCallback(async () => {
        if (!evaluationRequest) return;
        resetState();
        console.log("Sending request", evaluationRequest);
        const res = await fetch(`/api/gdpr/evaluation/stream`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(evaluationRequest)
        });
        await processNdjsonStream(res);
    }, [evaluationRequest, resetState, processNdjsonStream]);

    // Prunes "currently evaluating X" entries once their test case has landed as a
    // result or an error — runs regardless of whether the evaluation page is mounted.
    useEffect(() => {
        setCurrentStepInfos((infos) =>
            infos.filter((info) => {
                const runTestCases = testCasesByRun.get(info.runNumber) || [];
                const runErrors = errorsByRun.get(info.runNumber) || [];
                return !runTestCases.some(
                    (testCase) =>
                        testCase.modelLabel === info.modelLabel &&
                        testCase.testCaseId === info.currentTestCaseId
                ) && !runErrors.some(
                    (error) =>
                        error.modelLabel === info.modelLabel &&
                        error.testCaseId === info.currentTestCaseId
                );
            })
        );
    }, [testCasesByRun, errorsByRun]);

    const value: EvaluationJobContextValue = {
        evaluationRequest,
        setEvaluationRequest,
        metadata,
        setMetadata,
        testCasesByRun,
        setTestCasesByRun,
        summaryByRun,
        setSummaryByRun,
        currentStepInfos,
        errorsByRun,
        setErrorsByRun,
        isLoading,
        isFinished,
        setIsFinished,
        startEvaluation,
    };

    return <EvaluationJobContext.Provider value={value}>{children}</EvaluationJobContext.Provider>;
}

export function useEvaluationJob() {
    const context = useContext(EvaluationJobContext);
    if (!context) {
        throw new Error("useEvaluationJob must be used within EvaluationJobProvider");
    }
    return context;
}
