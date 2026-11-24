"use client"

import React, {createContext, ReactNode, useCallback, useContext, useEffect, useRef, useState} from "react";
import {EvaluationMetadataReport, EvaluationReportStepInfo} from "@/models/dto/ReportData";
import {ErrorsByRun, groupEvents, SummaryByRun, TestCasesByRun} from "@/lib/evaluation-report";
import {MultiEvaluationRequest} from "@/models/dto/MultiEvaluationRequest";
import {authenticatedFetch} from "@/lib/authenticated-fetch";
import {extractErrorDetails, toErrorMessage} from "@/lib/http-error";
import {useToast} from "@/components/ui/toast";
import {
    EvaluationRun,
    EvaluationRunDetail,
    EvaluationRunEvent,
    isActiveRun
} from "@/models/dto/EvaluationRun";

type StepInfo = EvaluationReportStepInfo & { modelLabel: string; runNumber: number };

const RUNS_URL = "/api/gdpr/evaluation/runs";
const EVENT_PAGE_SIZE = 500;
const POLL_INTERVAL_MS = 2000;

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
    /** A run is being started or is running on the server. */
    isLoading: boolean;
    isFinished: boolean;
    setIsFinished: (finished: boolean) => void;
    /** The server-side run whose results are shown, or null (nothing loaded or an uploaded JSON report). */
    run: EvaluationRun | null;
    startEvaluation: () => Promise<void>;
    cancelEvaluation: () => Promise<void>;
    resumeEvaluation: (id: number) => Promise<void>;
    /** Shows the stored results of a run; keeps polling while it is active. */
    loadRun: (id: number) => Promise<void>;
    /** Stops showing a server-side run, e.g. before showing an uploaded JSON report. */
    detachRun: () => void;
    /** Shows the user's active run, if there is one (e.g. after a reload). */
    attachToActiveRun: () => Promise<void>;
}

const EvaluationJobContext = createContext<EvaluationJobContextValue | null>(null);

async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
    const response = await authenticatedFetch(url, init);
    if (!response.ok) {
        throw new Error(await extractErrorDetails(response));
    }
    return (response.status === 204 ? undefined : await response.json()) as T;
}

/**
 * Evaluations run as background jobs on the server: closing the tab, reloading or a
 * sleeping computer does not stop them, and every finished test case is stored there.
 * This provider starts a run, then polls its status and newly stored results into the
 * same state the result views and exports use. It is mounted at the root layout so a
 * run keeps updating while the user is on other pages, and on mount it attaches to the
 * user's active run, so a reload shows it again.
 */
export function EvaluationJobProvider({children}: { children: ReactNode }) {
    const {showError} = useToast();
    const [evaluationRequest, setEvaluationRequest] = useState<MultiEvaluationRequest | null>(null);

    const [metadata, setMetadata] = useState<EvaluationMetadataReport | null>(null);
    const [testCasesByRun, setTestCasesByRun] = useState<TestCasesByRun>(new Map());
    const [summaryByRun, setSummaryByRun] = useState<SummaryByRun>(new Map());
    const [currentStepInfos, setCurrentStepInfos] = useState<StepInfo[]>([]);
    const [errorsByRun, setErrorsByRun] = useState<ErrorsByRun>(new Map());
    const [isFinished, setIsFinished] = useState(false);
    const [isStarting, setIsStarting] = useState(false);
    const [run, setRun] = useState<EvaluationRun | null>(null);

    /** Id of the run being shown; responses for any other run are dropped. */
    const runIdRef = useRef<number | null>(null);
    const lastEventIdRef = useRef(0);
    /** Syncs run one after another, so the same events are never fetched twice. */
    const syncQueueRef = useRef<Promise<void>>(Promise.resolve());
    const pollBusyRef = useRef(false);

    const resetResults = useCallback(() => {
        setMetadata(null);
        setTestCasesByRun(new Map());
        setSummaryByRun(new Map());
        setCurrentStepInfos([]);
        setErrorsByRun(new Map());
        setIsFinished(false);
    }, []);

    const applyEvents = useCallback((events: EvaluationRunEvent[]) => {
        const {testCasesByRun: cases, summaryByRun: summaries, errorsByRun: errors} = groupEvents(events);
        if (cases.size > 0) {
            setTestCasesByRun((prev) => {
                const next = new Map(prev);
                cases.forEach((added, runNumber) => next.set(runNumber, [...(next.get(runNumber) ?? []), ...added]));
                return next;
            });
        }
        if (summaries.size > 0) {
            setSummaryByRun((prev) => {
                const next = new Map(prev);
                summaries.forEach((added, runNumber) => {
                    const merged = new Map(next.get(runNumber) ?? []);
                    added.forEach((summary, label) => merged.set(label, summary));
                    next.set(runNumber, merged);
                });
                return next;
            });
        }
        if (errors.size > 0) {
            setErrorsByRun((prev) => {
                const next = new Map(prev);
                errors.forEach((added, runNumber) => next.set(runNumber, [...(next.get(runNumber) ?? []), ...added]));
                return next;
            });
        }
    }, []);

    const syncRun = useCallback(async (id: number) => {
        // Status first, events second: once the status is final, the events fetched
        // afterwards are guaranteed to include everything the run stored.
        const detail = await fetchJson<EvaluationRunDetail>(`${RUNS_URL}/${id}`);
        if (runIdRef.current !== id) return;
        while (true) {
            const events = await fetchJson<EvaluationRunEvent[]>(
                `${RUNS_URL}/${id}/events?after=${lastEventIdRef.current}&limit=${EVENT_PAGE_SIZE}`
            );
            if (runIdRef.current !== id) return;
            applyEvents(events);
            if (events.length > 0) lastEventIdRef.current = events[events.length - 1].id;
            if (events.length < EVENT_PAGE_SIZE) break;
        }
        setRun(detail.run);
        if (detail.metadata) setMetadata(detail.metadata);
        setCurrentStepInfos(detail.currentSteps.map(({modelLabel, runNumber, report}) => ({
            ...(report as EvaluationReportStepInfo), modelLabel, runNumber
        })));
        setIsFinished(!isActiveRun(detail.run.status));
    }, [applyEvents]);

    const enqueueSync = useCallback((id: number) => {
        const next = syncQueueRef.current.then(() => syncRun(id));
        syncQueueRef.current = next.catch(() => undefined);
        return next;
    }, [syncRun]);

    const loadRun = useCallback(async (id: number) => {
        runIdRef.current = id;
        lastEventIdRef.current = 0;
        setRun(null);
        resetResults();
        try {
            await enqueueSync(id);
        } catch (error) {
            showError("Could not load the evaluation", toErrorMessage(error));
        }
    }, [enqueueSync, resetResults, showError]);

    const detachRun = useCallback(() => {
        runIdRef.current = null;
        lastEventIdRef.current = 0;
        setRun(null);
        setCurrentStepInfos([]);
    }, []);

    const attachToActiveRun = useCallback(async () => {
        try {
            const runs = await fetchJson<EvaluationRun[]>(RUNS_URL);
            const active = runs.find((candidate) => isActiveRun(candidate.status));
            if (active && runIdRef.current !== active.id) {
                await loadRun(active.id);
            }
        } catch {
            // Not logged in or no evaluation role: there is nothing to attach to.
        }
    }, [loadRun]);

    useEffect(() => {
        void attachToActiveRun();
    }, [attachToActiveRun]);

    // Poll the shown run while it is active, and right away when the tab becomes visible again.
    const runId = run?.id;
    const runIsActive = isActiveRun(run?.status);
    useEffect(() => {
        if (runId === undefined || !runIsActive) return;
        const tick = () => {
            if (pollBusyRef.current) return;
            pollBusyRef.current = true;
            enqueueSync(runId)
                .catch((error) => console.error("Polling the evaluation failed:", error))
                .finally(() => { pollBusyRef.current = false; });
        };
        const onVisible = () => {
            if (document.visibilityState === "visible") tick();
        };
        const interval = setInterval(tick, POLL_INTERVAL_MS);
        document.addEventListener("visibilitychange", onVisible);
        window.addEventListener("pageshow", onVisible);
        return () => {
            clearInterval(interval);
            document.removeEventListener("visibilitychange", onVisible);
            window.removeEventListener("pageshow", onVisible);
        };
    }, [runId, runIsActive, enqueueSync]);

    const startEvaluation = useCallback(async () => {
        if (!evaluationRequest) return;
        setIsStarting(true);
        try {
            const {id} = await fetchJson<{ id: number }>(RUNS_URL, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(evaluationRequest)
            });
            await loadRun(id);
        } catch (error) {
            showError("Could not start the evaluation", toErrorMessage(error));
        } finally {
            setIsStarting(false);
        }
    }, [evaluationRequest, loadRun, showError]);

    const cancelEvaluation = useCallback(async () => {
        const id = runIdRef.current;
        if (id === null) return;
        try {
            await fetchJson<void>(`${RUNS_URL}/${id}/cancel`, {method: "POST"});
            await enqueueSync(id);
        } catch (error) {
            showError("Could not cancel the evaluation", toErrorMessage(error));
        }
    }, [enqueueSync, showError]);

    const resumeEvaluation = useCallback(async (id: number) => {
        setIsStarting(true);
        try {
            await fetchJson<{ id: number }>(`${RUNS_URL}/${id}/resume`, {method: "POST"});
            // Failed test cases are evaluated again, so their old errors were removed: load from scratch.
            await loadRun(id);
        } catch (error) {
            showError("Could not resume the evaluation", toErrorMessage(error));
        } finally {
            setIsStarting(false);
        }
    }, [loadRun, showError]);

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
        isLoading: isStarting || runIsActive,
        isFinished,
        setIsFinished,
        run,
        startEvaluation,
        cancelEvaluation,
        resumeEvaluation,
        loadRun,
        detachRun,
        attachToActiveRun,
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
