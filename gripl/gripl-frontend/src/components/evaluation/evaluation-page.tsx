"use client";

import {Button} from "@/components/ui/button";
import React, {useEffect, useMemo, useState} from "react";
import {
    EvaluationReportError,
    EvaluationReportSummary,
    TestCaseReport
} from "@/models/dto/ReportData";
import TestCaseReportCard from "@/components/evaluation/test-case-report/test-case-report-card";
import EvaluationReportSummaryCard from "@/components/evaluation/evaluation-report-summary-card";
import MetricsCharts from "@/components/evaluation/metrics-charts";
import {Spinner} from "@/components/ui/spinner";
import {MultiEvaluationRequest} from "@/models/dto/MultiEvaluationRequest";
import TestCaseErrorCard from "@/components/evaluation/test-case-report/test-case-error-card";
import {Tabs, TabsContent, TabsList, TabsTrigger} from "@/components/ui/tabs";
import {Card, CardContent, CardDescription, CardHeader} from "@/components/ui/card";
import EvaluationConfig from "@/components/evaluation/config/evaluation-config";
import {Dataset} from "@/models/dto/Dataset";
import {ChevronDown, FileText, Play} from "lucide-react";
import EvaluationRunList from "@/components/evaluation/evaluation-run-list";
import {isResumableRun} from "@/models/dto/EvaluationRun";
import TestcaseResultsStacked from "@/components/evaluation/charts/aggregated/testcase-results-stacked";
import {AggregatedEvaluationResults} from "@/models/evaluation/AggregatedEvaluationResult";
import MetricChart from "@/components/evaluation/charts/aggregated/metric-chart";
import {useColors} from "@/components/evaluation/charts/common/color-context";
import MetricsTable from "@/components/evaluation/charts/aggregated/metrics-table";
import {useToast} from "@/components/ui/toast";
import {useEvaluationJob} from "@/components/providers/evaluation-job-provider";
import {toErrorMessage} from "@/lib/http-error";
import {computeAggregateStats, isMulticlassMetadata} from "@/lib/evaluation-report";
import {
    applyClassSelectionToSummary,
    ALL_CLASSES_FILTER,
    classOptions,
    isAllClassesSelected
} from "@/lib/evaluation-class-metrics";
import {
    DropdownMenu,
    DropdownMenuCheckboxItem,
    DropdownMenuContent,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger
} from "@/components/ui/dropdown-menu";

interface EvaluationPageProps {
    datasets: Dataset[];
}

export default function EvaluationPage({ datasets }: EvaluationPageProps) {
    const {
        evaluationRequest, setEvaluationRequest,
        metadata, setMetadata,
        testCasesByRun, setTestCasesByRun,
        summaryByRun, setSummaryByRun,
        currentStepInfos,
        errorsByRun, setErrorsByRun,
        isLoading,
        setIsFinished,
        startEvaluation,
        run, resumeEvaluation, detachRun, attachToActiveRun,
    } = useEvaluationJob();

    const [selectedRun, setSelectedRun] = useState<number>(1);
    const [selectedModel, setSelectedModel] = useState<string | undefined>(undefined);
    const [selectedDataset, setSelectedDataset] = useState<string | undefined>(undefined);
    const [selectedClasses, setSelectedClasses] = useState<string[]>([ALL_CLASSES_FILTER]);

    // The provider attaches on its own mount, which can be before the user logged in.
    useEffect(() => {
        void attachToActiveRun();
    }, [attachToActiveRun]);

    const [isMetricsSummaryOpen, setIsMetricsSummaryOpen] = useState<boolean>(false);

    const { colors, setColors } = useColors()
    const { showError } = useToast()

    // Resetting the run/job state itself lives in the provider (so it survives
    // navigation); resetting which run tab is selected is page-local view state.
    const handleEvaluationStart = async () => {
        setSelectedRun(1);
        await startEvaluation();
    };

    const isMulticlassEvaluation = useMemo(
        () => isMulticlassMetadata(metadata, evaluationRequest?.defaultEvaluationEndpoint),
        [evaluationRequest, metadata]
    );

    const showExactMatchMetric = isMulticlassEvaluation && isAllClassesSelected(selectedClasses);
    const showClassSpecificMetrics = isMulticlassEvaluation && !isAllClassesSelected(selectedClasses);

    const testCases = testCasesByRun.get(selectedRun) || [];
    const summary = useMemo(() => {
        const rawSummary = summaryByRun.get(selectedRun) || new Map();

        if (!isMulticlassEvaluation) {
            return rawSummary;
        }

        return new Map(
            Array.from(rawSummary.entries()).map(([label, modelSummary]) => [
                label,
                applyClassSelectionToSummary(modelSummary, selectedClasses)
            ])
        );
    }, [summaryByRun, selectedRun, isMulticlassEvaluation, selectedClasses]);
    const errors = errorsByRun.get(selectedRun) || [];


    const involvedDatasets = useMemo(() => {
        const ids = new Set<number>();
        for (const tc of testCases) if (tc.datasetId != null) ids.add(tc.datasetId);
        for (const e of errors) if (e.datasetId != null) ids.add(e.datasetId);

        const nameById = new Map<number, string>();
        datasets.forEach((d) => nameById.set(d.id, d.name));
        metadata?.datasets.forEach((d) => nameById.set(d.id, d.name));

        const order = metadata?.datasets.map((d) => d.id) ?? [];
        return [...ids]
            .sort((a, b) => {
                const ia = order.indexOf(a);
                const ib = order.indexOf(b);
                if (ia !== -1 && ib !== -1) return ia - ib;
                if (ia !== -1) return -1;
                if (ib !== -1) return 1;
                return a - b;
            })
            .map((id) => ({ id, name: nameById.get(id) ?? `Dataset ${id}` }));
    }, [testCases, errors, metadata, datasets]);

    const aggregateStats = useMemo<AggregatedEvaluationResults | null>(
        () => computeAggregateStats({metadata, testCasesByRun, summaryByRun, errorsByRun}, isMulticlassEvaluation, selectedClasses),
        [summaryByRun, metadata, isMulticlassEvaluation, selectedClasses, testCasesByRun, errorsByRun]
    );

    const toggleClassSelection = (className: string, checked: boolean) => {
        setSelectedClasses((previous) => {
            const withoutAll = previous.filter((value) => value !== ALL_CLASSES_FILTER);

            if (checked) {
                return Array.from(new Set([...withoutAll, className]));
            }

            const next = withoutAll.filter((value) => value !== className);
            return next.length > 0 ? next : [ALL_CLASSES_FILTER];
        });
    };


    const handleUploadJsonReport = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (!file) return;
        // The uploaded report replaces the shown server-side run.
        detachRun();

        const reader = new FileReader();
        reader.onload = (e) => {
            try {
                const text = e.target?.result;
                if (typeof text !== "string") throw new Error("File content is not a string");

                const parsed = JSON.parse(text);

                setMetadata(parsed.metadata || null);

                const tcMap = new Map<number, (TestCaseReport & { modelLabel: string })[]>();
                for (const [runNum, cases] of Object.entries(parsed.testCasesByRun)) {
                    tcMap.set(parseInt(runNum), cases as any);
                }
                setTestCasesByRun(tcMap);

                const summaryMap = new Map<number, Map<string, EvaluationReportSummary>>();
                for (const [runNum, summaries] of Object.entries(parsed.summariesByRun)) {
                    const runSummaryMap = new Map<string, EvaluationReportSummary>();
                    (summaries as any[]).forEach((s: { label: string; summary: EvaluationReportSummary }) => {
                        runSummaryMap.set(s.label, s.summary);
                    });
                    summaryMap.set(parseInt(runNum), runSummaryMap);
                }
                setSummaryByRun(summaryMap);

                const errorsMap = new Map<number, (EvaluationReportError & { modelLabel: string })[]>();
                for (const [runNum, errs] of Object.entries(parsed.errorsByRun || {})) {
                    errorsMap.set(parseInt(runNum), errs as any);
                }
                setErrorsByRun(errorsMap);

                if (parsed.colors) {
                    setColors(parsed.colors);
                }
            } catch (err) {
                console.error("Failed to load report:", err);
                showError("Failed to load report", toErrorMessage(err));
            }
        };
        reader.readAsText(file);
        setIsFinished(true);
    };

    const completedCount =
        Array.from(testCasesByRun.values()).reduce((sum, cases) => sum + cases.length, 0) +
        Array.from(errorsByRun.values()).reduce((sum, errs) => sum + errs.length, 0);

    const onUploadJsonReportClick = () => {
        document.getElementById("upload-json-report")?.click()
    };

    const summariesByModel = useMemo(
        () => Array.from(summary.entries()).map(([label, s]) => ({ label, summary: s })),
        [summary]
    );

    return (
        <div className="w-full">
            <EvaluationConfig onMultiConfigChanged={setEvaluationRequest} datasets={datasets} className="mb-6">
                <>
                <div className="flex flex-row justify-between items-start flex-wrap mb-4 gap-4">
                    <div className="flex flex-row gap-4 flex-wrap">
                        <input id="upload-json-report" type="file" accept=".json" className="hidden" onChange={handleUploadJsonReport} />
                        <label htmlFor="upload-json-report">
                            <Button variant="secondary" onClick={onUploadJsonReportClick} disabled={isLoading} className="hover:cursor-pointer">
                                <span className="flex flex-row items-center gap-2">
                                    <FileText className="h-4 w-4" />
                                    Upload JSON Report
                                </span>
                            </Button>
                        </label>
                    </div>
                    <Button variant="default" disabled={isLoading || !evaluationRequest}
                            onClick={handleEvaluationStart} className="h-auto">
                        <>
                            {!isLoading && <Play className="h-4 w-4" /> }
                            {!isLoading && "Start Evaluation"}
                            {isLoading && (
                                <div className="flex flex-row space-x-4">
                                    <Spinner className="h-4 w-4 text-foreground"/>
                                    {currentStepInfos.length > 0 && (
                                        <div className="flex flex-row items-center space-x-4">
                                            <div className="flex flex-col justify-start overflow-y-auto">
                                                {currentStepInfos.map((info, idx) => (
                                                    <span key={`${info.modelLabel}-${info.runNumber}-stepinfo-${idx}`}
                                                          className="whitespace-nowrap">
                                                    [Run {info.runNumber}] [{info.modelLabel}] Evaluating {info.currentTestCaseName}...
                                                </span>
                                                ))}
                                            </div>
                                            <p>({completedCount} / {run?.totalTestCases ?? "?"})</p>
                                        </div>
                                    )}
                                </div>
                            )}
                        </>
                    </Button>
                </div>
                {run && isResumableRun(run.status) && (
                    <div className="mb-4 flex flex-row flex-wrap items-center gap-3 rounded-md border border-amber-500 p-3 text-sm">
                        <span>
                            {run.status === "CANCELLED" && "This evaluation was cancelled. "}
                            {run.status === "INTERRUPTED" && "This evaluation was interrupted by a server restart. "}
                            {run.status === "ERROR" && `This evaluation failed${run.errorMessage ? `: ${run.errorMessage}` : ""}. `}
                            {completedCount} of {run.totalTestCases ?? "?"} test cases are finished and shown below.
                        </span>
                        <Button variant="outline" size="sm" disabled={isLoading} onClick={() => resumeEvaluation(run.id)}>
                            <Play className="h-4 w-4"/> Resume
                        </Button>
                    </div>
                )}
                </>
            </EvaluationConfig>

            <section className="px-6 container mx-auto">
                <EvaluationRunList/>
            </section>

            <section className="px-6 container mx-auto">
                <h2 className="text-2xl font-semibold mb-2">Complete Result Overview</h2>
                {isMulticlassEvaluation && (
                    <div className="mb-4 flex flex-wrap items-center gap-3">
                        <span className="text-sm font-medium">Classification Filter:</span>
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button variant="outline" size="sm" className="gap-2">
                                    {isAllClassesSelected(selectedClasses)
                                        ? "All"
                                        : `${selectedClasses.length} selected`}
                                    <ChevronDown className="h-4 w-4" />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="start">
                                <DropdownMenuLabel>Per-class Metrics</DropdownMenuLabel>
                                <DropdownMenuSeparator />
                                <DropdownMenuCheckboxItem
                                    checked={isAllClassesSelected(selectedClasses)}
                                    onCheckedChange={(checked) => {
                                        if (checked === true) {
                                            setSelectedClasses([ALL_CLASSES_FILTER]);
                                        }
                                    }}
                                >
                                    All
                                </DropdownMenuCheckboxItem>
                                {classOptions().map((className) => (
                                    <DropdownMenuCheckboxItem
                                        key={className}
                                        checked={!isAllClassesSelected(selectedClasses) && selectedClasses.includes(className)}
                                        onCheckedChange={(checked) => toggleClassSelection(className, checked === true)}
                                    >
                                        {className}
                                    </DropdownMenuCheckboxItem>
                                ))}
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                )}
                {summariesByModel.length > 0 || metadata ? <>
                    <Card className="mb-6">
                        <CardHeader>
                            <h3 className="text-xl font-semibold">Evaluation Metadata</h3>
                        </CardHeader>
                        <CardContent>
                            <>{ metadata && <CardDescription>
                                <table>
                                    <tbody>
                                    <tr>
                                        <td className="align-top">Models:</td>
                                        <td className="pl-4 pb-2">
                                            <div className="flex flex-wrap gap-2">
                                                {metadata.modelLabels.map((label, index) => (
                                                    <div
                                                        key={label}
                                                        className={`inline-flex items-center gap-1.5 px-2 py-1 rounded-full bg-card border-2 text-secondary-foreground text-sm`}
                                                        style={{
                                                            borderColor: colors[label]
                                                        }}
                                                    >
                                                        <span className="font-medium">{label}</span>
                                                        <span className="text-muted-foreground">•</span>
                                                        <span>temp: {metadata.modelTemperatures[index] ?? "default"}</span>
                                                        <span className="text-muted-foreground">•</span>
                                                        <span>topP: {metadata.modelTopPs?.[index] ?? "default"}</span>
                                                    </div>
                                                ))}
                                            </div>
                                        </td>
                                    </tr>
                                    <tr><td>Datasets:</td><td className="pl-4">{metadata.datasets.map(d => d.name).join(", ")}</td></tr>
                                    <tr><td>Total Test Cases:</td><td className="pl-4">{metadata.totalTestCases}</td></tr>
                                    <tr><td>Default Evaluation Endpoint:</td><td className="pl-4">{metadata.defaultEvaluationEndpoint}</td></tr>
                                    {metadata.totalRepetitions && metadata.totalRepetitions >= 1 && <tr><td>Total Runs:</td><td className="pl-4">{metadata.totalRepetitions}</td></tr>}
                                    <tr><td>Seed:</td><td className="pl-4">{metadata.seed}</td></tr>
                                    <tr><td>Timestamp:</td><td className="pl-4">{new Date(metadata.timestamp).toLocaleString()}</td></tr>
                                    </tbody>
                                </table>
                            </CardDescription> }</>
                        </CardContent>
                    </Card>
                    {aggregateStats && metadata && metadata.totalRepetitions && <h2 className="text-2xl font-semibold mb-2">Aggregated Results Across  {metadata.totalRepetitions} Runs</h2> }
                    <div className="space-y-6 mb-8">
                        {/* Aggregate Statistics across all runs */}
                        {aggregateStats && metadata && metadata.totalRepetitions && metadata.totalRepetitions >= 1 && (<>
                            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                                <MetricChart
                                    title="Precision"
                                    description="Precision (mean ± SD)"
                                    metricKey="avgPrecision"
                                    stdKey="stdPrecision"
                                    aggregatedEvaluationResults={aggregateStats}
                                />
                                <MetricChart
                                    title="Recall"
                                    description="Recall (mean ± SD)"
                                    metricKey="avgRecall"
                                    stdKey="stdRecall"
                                    aggregatedEvaluationResults={aggregateStats}
                                />
                                <MetricChart
                                    title="F1-Score"
                                    description="F1-Score (mean ± SD)"
                                    metricKey="avgF1Score"
                                    stdKey="stdF1Score"
                                    aggregatedEvaluationResults={aggregateStats}
                                />
                                {(!isMulticlassEvaluation || showExactMatchMetric) && (
                                    <MetricChart
                                        title={isMulticlassEvaluation ? "Exact Match Accuracy" : "Accuracy"}
                                        description={isMulticlassEvaluation ? "Exact Match Accuracy (mean ± SD)" : "Accuracy (mean ± SD)"}
                                        metricKey={isMulticlassEvaluation ? "avgExactMatchAccuracy" : "avgAccuracy"}
                                        stdKey={isMulticlassEvaluation ? "stdExactMatchAccuracy" : "stdAccuracy"}
                                        aggregatedEvaluationResults={aggregateStats}
                                    />
                                )}
                            </div>
                            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                                <TestcaseResultsStacked aggregatedEvaluationResults={aggregateStats} repetisions={metadata.totalRepetitions} />
                                <MetricChart
                                    title="Retries"
                                    description="Number of retries accross 25 test cases (mean ± SD)"
                                    metricKey="avgAmountOfRetries"
                                    stdKey="stdAmountOfRetries"
                                    aggregatedEvaluationResults={aggregateStats}
                                    xAxisMaxOffset={2}
                                />
                            </div>
                            <MetricsTable
                                aggregatedEvaluationResults={aggregateStats}
                                showAccuracy={!isMulticlassEvaluation}
                                showExactMatchAccuracy={isMulticlassEvaluation && showExactMatchMetric}
                                classSpecificView={showClassSpecificMetrics}
                            />
                            {Object.values(aggregateStats).some(s => s.ragRunsCounted > 0) && (
                                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                                    <MetricChart
                                        title="Context Utilization"
                                        description="Context Utilization (mean ± SD)"
                                        metricKey="avgContextUtilization"
                                        stdKey="stdContextUtilization"
                                        aggregatedEvaluationResults={aggregateStats}
                                    />
                                    <MetricChart
                                        title="Faithfulness"
                                        description="Faithfulness (mean ± SD)"
                                        metricKey="avgFaithfulness"
                                        stdKey="stdFaithfulness"
                                        aggregatedEvaluationResults={aggregateStats}
                                    />
                                </div>
                            )}
                        </>)}
                    </div>
                </> : <Card className="p-4 mb-4">
                    <p className="text-muted-foreground">No results yet. Start an evaluation to see results here.</p>
                </Card>}

                {metadata && metadata.totalRepetitions && metadata.totalRepetitions >= 1 && (
                    <>
                        <h2 className="text-2xl font-semibold mb-2">Results by Run</h2>
                        <Tabs value={selectedRun.toString()} onValueChange={(v) => setSelectedRun(parseInt(v))} className="w-full mb-6">
                            <TabsList className="w-full h-12 sticky top-0 z-20 mb-4">
                                {Array.from({ length: metadata.totalRepetitions }, (_, i) => i + 1).map((runNum) => (
                                    <TabsTrigger value={runNum.toString()} key={`run-${runNum}-trigger`}>
                                        Run {runNum}
                                    </TabsTrigger>
                                ))}
                            </TabsList>

                            {Array.from({ length: metadata.totalRepetitions }, (_, i) => i + 1).map((runNum) => (
                                <TabsContent value={runNum.toString()} key={`run-${runNum}-content`}>
                                    {!summaryByRun.get(runNum) && !testCasesByRun.get(runNum) && !errorsByRun.get(runNum) && (
                                        <Card className="p-4 text-muted-foreground mb-4">
                                            No results yet for Run {runNum}. Start an evaluation to see results here.
                                        </Card>
                                    )}

                                    {summaryByRun.get(runNum) && (
                                        <div className="space-y-6 mb-6">
                                            <MetricsCharts
                                                isMulticlass={isMulticlassEvaluation}
                                                showExactMatchMetric={showExactMatchMetric}
                                                reportSummaries={Array.from((summaryByRun.get(runNum) || new Map()).entries()).map(([label, s]) => ({
                                                    label,
                                                    summary: isMulticlassEvaluation ? applyClassSelectionToSummary(s, selectedClasses) : s
                                                }))}/>
                                        </div>
                                    )}

                                    <h2 className="text-2xl font-semibold mb-2">Results by
                                        Model{metadata && metadata.totalRepetitions && metadata.totalRepetitions > 1 ? ` (Run ${selectedRun})` : ""}</h2>
                                    <Tabs className="w-full" value={selectedModel || metadata?.modelLabels?.[0]} onValueChange={setSelectedModel}>
                                        <TabsList className="w-full h-12 sticky top-12 z-10 mb-4">
                                            {metadata?.modelLabels.map?.((label) => (
                                                <TabsTrigger value={label} key={`${label}-trigger`}>
                                                    {label}
                                                </TabsTrigger>
                                            ))}
                                        </TabsList>

                                        {metadata?.modelLabels.map?.((label) => {
                                            const modelSummary = summary?.get(label);

                                            return (
                                                <TabsContent value={label} key={`${label}-content`}>
                                                    {!modelSummary && !testCases.some((tc) => tc.modelLabel === label) && !errors.some((e) => e.modelLabel === label) && (
                                                        <Card className="p-4 text-muted-foreground mb-4">
                                                            No results yet for model <strong>{label}</strong>. Start an
                                                            evaluation to see results here.
                                                        </Card>
                                                    )}

                                                    {modelSummary && (
                                                        <div className="space-y-6 mb-6">
                                                            <EvaluationReportSummaryCard
                                                                reportSummary={modelSummary}
                                                                isMulticlass={isMulticlassEvaluation}
                                                                showExactMatchMetric={showExactMatchMetric}
                                                            />
                                                            <MetricsCharts
                                                                reportSummary={modelSummary}
                                                                isMulticlass={isMulticlassEvaluation}
                                                                showExactMatchMetric={showExactMatchMetric}
                                                            />
                                                        </div>
                                                    )}

                                                    <h2 className="text-2xl font-semibold mb-2">Test Case Results for {label}</h2>
                                                    <Tabs className="w-full" value={selectedDataset || (involvedDatasets[0] ? `dataset-${involvedDatasets[0].id}` : undefined)}
                                                          onValueChange={setSelectedDataset}>
                                                        <TabsList className="w-full h-12 sticky top-24 z-30 mb-4">
                                                            {involvedDatasets.map((dataset) => (
                                                                <TabsTrigger value={`dataset-${dataset.id}`}
                                                                             key={`dataset-${dataset.id}-trigger`}>
                                                                    {dataset.name}
                                                                </TabsTrigger>
                                                            ))}
                                                        </TabsList>

                                                        {involvedDatasets.map((dataset) => (
                                                            <TabsContent value={`dataset-${dataset.id}`}
                                                                         key={`dataset-${dataset.id}-content`}>
                                                                <div className="flex flex-col space-y-4 pb-6">
                                                                    {testCases
                                                                        .filter((testCase) => testCase.modelLabel === label && testCase.datasetId === dataset.id)
                                                                        .sort((a, b) => a.testCaseId - b.testCaseId)
                                                                        .map((report) => (
                                                                            <div
                                                                                key={`${report.modelLabel}-${report.testCaseId}`}>
                                                                                <TestCaseReportCard report={report}/>
                                                                            </div>
                                                                        ))}
                                                                </div>

                                                                {errors.filter((error) => error.modelLabel === label && error.datasetId === dataset.id).length > 0 && (
                                                                    <div className="flex flex-col space-y-4 pb-4">
                                                                        {errors
                                                                            .filter((error) => error.modelLabel === label && error.datasetId === dataset.id)
                                                                            .map((e, idx) => (
                                                                                <div
                                                                                    key={`${e.modelLabel}-${e.testCaseId}-${idx}`}>
                                                                                    <TestCaseErrorCard error={e}/>
                                                                                </div>
                                                                            ))}
                                                                    </div>
                                                                )}
                                                            </TabsContent>
                                                        ))}
                                                    </Tabs>
                                                </TabsContent>
                                            );
                                        })}
                                    </Tabs>
                                </TabsContent>
                            ))}
                        </Tabs>
                    </>
                )}
            </section>
        </div>
    );
}
