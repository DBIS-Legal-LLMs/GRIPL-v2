"use client"

import React, {useCallback, useEffect, useState} from "react";
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card";
import {Button} from "@/components/ui/button";
import {Badge} from "@/components/ui/badge";
import {Spinner} from "@/components/ui/spinner";
import {ConfirmationDialog} from "@/components/ui/confirmation-dialog";
import {useToast} from "@/components/ui/toast";
import {authenticatedFetch} from "@/lib/authenticated-fetch";
import {extractErrorDetails, toErrorMessage} from "@/lib/http-error";
import {useEvaluationJob} from "@/components/providers/evaluation-job-provider";
import {EvaluationRun, EvaluationRunStatus, isActiveRun, isResumableRun} from "@/models/dto/EvaluationRun";
import {Eye, Play, Square, Trash2} from "lucide-react";

const RUNS_URL = "/api/gdpr/evaluation/runs";
const POLL_INTERVAL_MS = 5000;

const STATUS_LABELS: Record<EvaluationRunStatus, string> = {
    QUEUED: "Queued",
    RUNNING: "Running",
    DONE: "Done",
    CANCELLED: "Cancelled",
    INTERRUPTED: "Interrupted",
    ERROR: "Failed",
};

function statusBadge(status: EvaluationRunStatus) {
    switch (status) {
        case "QUEUED":
        case "RUNNING":
            return <Badge variant="outline" className="gap-1.5">
                <Spinner size="small" className="h-3 w-3"/> {STATUS_LABELS[status]}
            </Badge>;
        case "DONE":
            return <Badge variant="outline" className="border-emerald-500 text-emerald-600 dark:text-emerald-400">
                {STATUS_LABELS[status]}
            </Badge>;
        case "ERROR":
            return <Badge variant="destructive">{STATUS_LABELS[status]}</Badge>;
        default:
            return <Badge variant="outline" className="border-amber-500 text-amber-600 dark:text-amber-400">
                {STATUS_LABELS[status]}
            </Badge>;
    }
}

function formatDate(iso: string) {
    return new Date(iso).toLocaleString(undefined, {dateStyle: "medium", timeStyle: "short"});
}

/**
 * The user's evaluation runs, newest first. Every run is stored on the server, so it can be
 * opened again later, and a cancelled, interrupted or failed run can be resumed.
 */
export default function EvaluationRunList() {
    const {run: shownRun, loadRun, resumeEvaluation, detachRun, isLoading} = useEvaluationJob();
    const {showError} = useToast();
    const [runs, setRuns] = useState<EvaluationRun[] | null>(null);
    const [runToDelete, setRunToDelete] = useState<EvaluationRun | null>(null);

    const refresh = useCallback(async () => {
        try {
            const response = await authenticatedFetch(RUNS_URL);
            if (!response.ok) throw new Error(await extractErrorDetails(response));
            setRuns(await response.json());
        } catch (error) {
            console.error("Could not load the evaluation runs:", error);
        }
    }, []);

    // Reload when the shown run changes status or makes progress, and poll while a run is active.
    const anyActive = runs?.some((candidate) => isActiveRun(candidate.status)) ?? false;
    useEffect(() => {
        void refresh();
    }, [refresh, shownRun?.id, shownRun?.status, shownRun?.completedTestCases]);
    useEffect(() => {
        if (!anyActive) return;
        const interval = setInterval(() => void refresh(), POLL_INTERVAL_MS);
        return () => clearInterval(interval);
    }, [anyActive, refresh]);

    async function cancel(id: number) {
        try {
            const response = await authenticatedFetch(`${RUNS_URL}/${id}/cancel`, {method: "POST"});
            if (!response.ok) throw new Error(await extractErrorDetails(response));
            if (shownRun?.id === id) await loadRun(id);
        } catch (error) {
            showError("Could not cancel the evaluation", toErrorMessage(error));
        }
        await refresh();
    }

    async function confirmDelete() {
        const target = runToDelete;
        setRunToDelete(null);
        if (!target) return;
        try {
            const response = await authenticatedFetch(`${RUNS_URL}/${target.id}`, {method: "DELETE"});
            if (!response.ok) throw new Error(await extractErrorDetails(response));
            if (shownRun?.id === target.id) detachRun();
        } catch (error) {
            showError("Could not delete the evaluation", toErrorMessage(error));
        }
        await refresh();
    }

    return <Card className="mb-6">
        <CardHeader>
            <CardTitle>Evaluation runs</CardTitle>
            <CardDescription>
                Evaluations run on the server: you can close this tab or let your computer sleep, the results
                are stored as they come in. A cancelled or interrupted run can be resumed, only the missing and
                failed test cases are evaluated again.
            </CardDescription>
        </CardHeader>
        <CardContent>
            {runs === null && <Spinner className="h-5 w-5"/>}
            {runs !== null && runs.length === 0 && (
                <p className="text-sm text-muted-foreground">No evaluation runs yet.</p>
            )}
            <div className="flex flex-col gap-2">
                {runs?.map((item) => {
                    const isShown = shownRun?.id === item.id;
                    return <div
                        key={item.id}
                        className={`flex flex-row flex-wrap items-center gap-3 rounded-md border p-3 ${isShown ? "border-primary bg-muted/50" : ""}`}
                    >
                        <div className="flex min-w-0 flex-1 flex-col gap-1">
                            <div className="flex flex-row flex-wrap items-center gap-2">
                                {statusBadge(item.status)}
                                <span className="text-sm font-medium">{formatDate(item.createdAt)}</span>
                                <span className="text-sm text-muted-foreground">
                                    {item.completedTestCases} / {item.totalTestCases ?? "?"} test cases
                                </span>
                                {isShown && <span className="text-xs text-muted-foreground">(shown below)</span>}
                            </div>
                            <span className="truncate text-xs text-muted-foreground"
                                  title={`${item.modelLabels.join(", ")} · ${item.datasetNames.join(", ")}`}>
                                Models: {item.modelLabels.join(", ")}
                                {item.datasetNames.length > 0 && <> · Datasets: {item.datasetNames.join(", ")}</>}
                            </span>
                            {item.errorMessage && (
                                <span className="text-xs text-destructive">{item.errorMessage}</span>
                            )}
                        </div>
                        <div className="flex flex-row gap-2">
                            <Button variant="outline" size="sm" disabled={isShown} onClick={() => loadRun(item.id)}>
                                <Eye className="h-4 w-4"/> Open
                            </Button>
                            {isActiveRun(item.status) && (
                                <Button variant="outline" size="sm" onClick={() => cancel(item.id)}>
                                    <Square className="h-4 w-4"/> Cancel
                                </Button>
                            )}
                            {isResumableRun(item.status) && (
                                <Button variant="outline" size="sm" disabled={isLoading || anyActive}
                                        title={anyActive ? "Another evaluation is running" : undefined}
                                        onClick={() => resumeEvaluation(item.id)}>
                                    <Play className="h-4 w-4"/> Resume
                                </Button>
                            )}
                            <Button variant="outline" size="icon" title="Delete"
                                    disabled={isActiveRun(item.status)}
                                    onClick={() => setRunToDelete(item)}>
                                <Trash2 className="h-4 w-4"/>
                            </Button>
                        </div>
                    </div>;
                })}
            </div>
        </CardContent>
        <ConfirmationDialog
            isOpen={runToDelete !== null}
            onClose={() => setRunToDelete(null)}
            onConfirm={confirmDelete}
            title="Delete evaluation run"
            description="The run and all its results will be deleted. Download the reports first if you still need them. This cannot be undone."
            confirmLabel="Delete"
        />
    </Card>;
}
