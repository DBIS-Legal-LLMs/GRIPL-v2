"use client"

import React, {useCallback, useEffect, useRef, useState} from "react";
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
import {Check, Download, Pencil, Play, Square, Trash2, X} from "lucide-react";
import {Input} from "@/components/ui/input";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger
} from "@/components/ui/dropdown-menu";
import {useColors} from "@/components/evaluation/charts/common/color-context";
import {ALL_CLASSES_FILTER} from "@/lib/evaluation-class-metrics";
import {
    buildJsonReport,
    buildMarkdownReport,
    computeAggregateStats,
    downloadTextFile,
    fetchRunResults,
    hasResults,
    isMulticlassMetadata,
    reportFileBaseName
} from "@/lib/evaluation-report";

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

/** Keeps clicks and key presses on controls inside a tile from opening the run. */
const stopPropagation = (event: React.SyntheticEvent) => event.stopPropagation();

/**
 * The user's evaluation runs, newest first. Every run is stored on the server, so it can be
 * opened again later, and a cancelled, interrupted or failed run can be resumed.
 */
export default function EvaluationRunList() {
    const {run: shownRun, loadRun, resumeEvaluation, detachRun, isLoading} = useEvaluationJob();
    const {showError} = useToast();
    const [runs, setRuns] = useState<EvaluationRun[] | null>(null);
    const [runToDelete, setRunToDelete] = useState<EvaluationRun | null>(null);
    const [downloadingId, setDownloadingId] = useState<number | null>(null);
    const [editing, setEditing] = useState<{ id: number; name: string } | null>(null);
    const [savingNameId, setSavingNameId] = useState<number | null>(null);
    /** Set once an edit is saved or cancelled, so a blur that follows (Enter, Escape, unmount) does nothing. */
    const editDoneRef = useRef(false);
    const cancelEdit = () => {
        editDoneRef.current = true;
        setEditing(null);
    };
    const {colors} = useColors();

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

    /** Builds the report from the results stored on the server, so it works for any run, not only the shown one. */
    async function downloadReport(item: EvaluationRun, format: "markdown" | "json") {
        setDownloadingId(item.id);
        try {
            const data = await fetchRunResults(item.id);
            if (!hasResults(data)) {
                showError("No results yet", "This run has no finished test cases to put in a report.");
                return;
            }
            const allClasses = [ALL_CLASSES_FILTER];
            const isMulticlass = isMulticlassMetadata(data.metadata);
            const aggregateStats = computeAggregateStats(data, isMulticlass, allClasses);
            const baseName = reportFileBaseName(item.id, item.createdAt, item.name);
            if (format === "markdown") {
                downloadTextFile(`${baseName}.md`, buildMarkdownReport(data, aggregateStats, isMulticlass, allClasses), "text/markdown");
            } else {
                downloadTextFile(`${baseName}.json`, buildJsonReport(data, aggregateStats, colors), "application/json");
            }
        } catch (error) {
            showError("Could not download the report", toErrorMessage(error));
        } finally {
            setDownloadingId(null);
        }
    }

    async function saveName() {
        const target = editing;
        if (!target || editDoneRef.current) return;
        editDoneRef.current = true;
        setEditing(null);
        const current = runs?.find((candidate) => candidate.id === target.id);
        const name = target.name.trim();
        if (!current || name === (current.name ?? "")) return;
        setSavingNameId(target.id);
        try {
            const response = await authenticatedFetch(`${RUNS_URL}/${target.id}`, {
                method: "PATCH",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({name: name || null})
            });
            if (!response.ok) throw new Error(await extractErrorDetails(response));
            const updated: EvaluationRun = await response.json();
            setRuns((prev) => prev?.map((candidate) =>
                candidate.id === updated.id ? {...candidate, name: updated.name} : candidate) ?? prev);
        } catch (error) {
            showError("Could not rename the evaluation", toErrorMessage(error));
        } finally {
            setSavingNameId(null);
        }
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
                    const isEditing = editing?.id === item.id;
                    const open = () => {
                        if (!isShown) void loadRun(item.id);
                    };
                    return <div
                        key={item.id}
                        role="button"
                        tabIndex={0}
                        aria-current={isShown || undefined}
                        title={isShown ? "Shown below" : "Show the results of this run"}
                        onClick={open}
                        onKeyDown={(event) => {
                            if (event.target !== event.currentTarget) return;
                            if (event.key === "Enter" || event.key === " ") {
                                event.preventDefault();
                                open();
                            }
                        }}
                        className={`flex flex-row flex-wrap items-center gap-3 rounded-md border p-3 transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring ${isShown ? "border-primary bg-muted/50" : "cursor-pointer hover:bg-muted/30"}`}
                    >
                        <div className="flex min-w-0 flex-1 flex-col gap-1">
                            {isEditing ? (
                                <div className="flex flex-row items-center gap-2" onClick={stopPropagation}
                                     onKeyDown={stopPropagation}>
                                    <Input autoFocus maxLength={200} className="h-8 max-w-md"
                                           placeholder="Experiment name"
                                           aria-label="Experiment name"
                                           value={editing.name}
                                           onChange={(event) => setEditing({id: item.id, name: event.target.value})}
                                           onBlur={() => void saveName()}
                                           onKeyDown={(event) => {
                                               if (event.key === "Enter") void saveName();
                                               if (event.key === "Escape") cancelEdit();
                                           }}/>
                                    {/* preventDefault on mousedown keeps the focus in the input, so blur does not save first. */}
                                    <Button variant="ghost" size="icon" className="h-8 w-8" title="Save"
                                            onMouseDown={(event) => event.preventDefault()}
                                            onClick={() => void saveName()}>
                                        <Check className="h-4 w-4"/>
                                    </Button>
                                    <Button variant="ghost" size="icon" className="h-8 w-8" title="Cancel"
                                            onMouseDown={(event) => event.preventDefault()}
                                            onClick={cancelEdit}>
                                        <X className="h-4 w-4"/>
                                    </Button>
                                </div>
                            ) : (
                                <div className="flex min-w-0 flex-row items-center gap-1">
                                    <span className={`truncate font-semibold ${item.name ? "" : "text-muted-foreground italic"}`}>
                                        {item.name ?? "Unnamed experiment"}
                                    </span>
                                    <Button variant="ghost" size="icon" className="h-7 w-7 shrink-0"
                                            title="Rename" aria-label="Rename"
                                            disabled={savingNameId === item.id}
                                            onClick={(event) => {
                                                event.stopPropagation();
                                                editDoneRef.current = false;
                                                setEditing({id: item.id, name: item.name ?? ""});
                                            }}
                                            onKeyDown={stopPropagation}>
                                        {savingNameId === item.id
                                            ? <Spinner size="small" className="h-3.5 w-3.5"/>
                                            : <Pencil className="h-3.5 w-3.5"/>}
                                    </Button>
                                </div>
                            )}
                            <div className="flex flex-row flex-wrap items-center gap-2">
                                {statusBadge(item.status)}
                                <span className="text-sm">{formatDate(item.createdAt)}</span>
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
                        {/* Events from these buttons and the dropdown (a portal, but React events still bubble) must not open the run. */}
                        <div className="flex flex-row gap-2" onClick={stopPropagation} onKeyDown={stopPropagation}>
                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button variant="outline" size="sm"
                                            disabled={item.completedTestCases === 0 || downloadingId === item.id}
                                            title={item.completedTestCases === 0 ? "No finished test cases yet" : "Download the report of this run"}>
                                        {downloadingId === item.id
                                            ? <Spinner size="small" className="h-4 w-4"/>
                                            : <Download className="h-4 w-4"/>} Report
                                    </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end">
                                    <DropdownMenuItem onSelect={() => void downloadReport(item, "markdown")}>
                                        Markdown (.md)
                                    </DropdownMenuItem>
                                    <DropdownMenuItem onSelect={() => void downloadReport(item, "json")}>
                                        JSON (.json)
                                    </DropdownMenuItem>
                                </DropdownMenuContent>
                            </DropdownMenu>
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
