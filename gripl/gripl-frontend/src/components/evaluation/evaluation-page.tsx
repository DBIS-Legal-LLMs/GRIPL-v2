"use client";

import {Button} from "@/components/ui/button";
import React, {useEffect, useMemo, useState} from "react";
import {
    EvaluationMetadataReport,
    EvaluationReport,
    EvaluationReportError,
    EvaluationReportStepInfo,
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
import TestcaseResultsStacked from "@/components/evaluation/charts/aggregated/testcase-results-stacked";
import {AggregatedEvaluationResults} from "@/models/evaluation/AggregatedEvaluationResult";
import MetricChart from "@/components/evaluation/charts/aggregated/metric-chart";
import {ColorProvider, useColors} from "@/components/evaluation/charts/common/color-context";
import MetricsTable from "@/components/evaluation/charts/aggregated/metrics-table";
import {
    applyClassSelectionToSummary,
    ALL_CLASSES_FILTER,
    classOptions,
    hasPerClassMetrics,
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

type ModelReportEnvelope = {
    modelLabel: string;
    report: EvaluationReport;
    runNumber: number;
};

interface EvaluationPageProps {
    datasets: Dataset[];
}

export default function EvaluationPage({ datasets }: EvaluationPageProps) {
    const [evaluationRequest, setEvaluationRequest] = useState<MultiEvaluationRequest | null>(null);

    const [metadata, setMetadata] = useState<EvaluationMetadataReport | null>(null);
    const [testCasesByRun, setTestCasesByRun] = useState<Map<number, (TestCaseReport & { modelLabel: string })[]>>(new Map());
    const [summaryByRun, setSummaryByRun] = useState<Map<number, Map<string, EvaluationReportSummary>>>(new Map());
    const [currentStepInfos, setCurrentStepInfos] = useState<(EvaluationReportStepInfo & { modelLabel: string, runNumber: number })[]>([]);
    const [errorsByRun, setErrorsByRun] = useState<Map<number, (EvaluationReportError & { modelLabel: string })[]>>(new Map());
    const [isLoading, setIsLoading] = useState(false);
    const [isFinished, setIsFinished] = useState(false);

    const [selectedRun, setSelectedRun] = useState<number>(1);
    const [selectedModel, setSelectedModel] = useState<string | undefined>(undefined);
    const [selectedDataset, setSelectedDataset] = useState<string | undefined>(undefined);
    const [selectedClasses, setSelectedClasses] = useState<string[]>([ALL_CLASSES_FILTER]);

    const [isMetricsSummaryOpen, setIsMetricsSummaryOpen] = useState<boolean>(false);

    const { colors, setColors } = useColors()

    const handleEvaluationStart = async () => {
        if (!evaluationRequest) return;

        setMetadata(null);
        setTestCasesByRun(new Map());
        setSummaryByRun(new Map());
        setCurrentStepInfos([]);
        setErrorsByRun(new Map());
        setIsLoading(true);
        setIsFinished(false);
        setSelectedRun(1);

        console.log("Sending request", evaluationRequest)

        const res = await fetch(`/api/gdpr/evaluation/stream`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(evaluationRequest)
        });

        if (!res.ok || !res.body) {
            console.error("Request failed:", res.status, res.statusText);
            setIsLoading(false);
            return;
        }

        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split("\n");

            for (let i = 0; i < lines.length - 1; i++) {
                const line = lines[i].trim();
                if (!line) continue;

                try {
                    const env = JSON.parse(line) as ModelReportEnvelope;
                    const { modelLabel, report, runNumber } = env;

                    console.log(`Received report for model: ${modelLabel}, run: ${runNumber}`, report);

                    if (report.type === "metadata") {
                        setMetadata(report);
                    } else if (report.type === "testCase") {
                        setTestCasesByRun((prev) => {
                            const next = new Map(prev);
                            const runCases = next.get(runNumber) || [];
                            next.set(runNumber, [...runCases, { ...(report as TestCaseReport), modelLabel }]);
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
                        setCurrentStepInfos((prev) => [...prev, { ...(report as EvaluationReportStepInfo), modelLabel, runNumber }]);
                    } else if (report.type === "error") {
                        setErrorsByRun((prev) => {
                            const next = new Map(prev);
                            const runErrors = next.get(runNumber) || [];
                            next.set(runNumber, [...runErrors, { ...(report as EvaluationReportError), modelLabel }]);
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
    };

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

    const isMulticlassEvaluation = useMemo(() => {
        const endpoint = evaluationRequest?.defaultEvaluationEndpoint
            ?? metadata?.defaultEvaluationEndpoint;

        if (endpoint?.includes("multiclass") === true) {
            return true;
        }

        for (const runSummaries of summaryByRun.values()) {
            for (const summary of runSummaries.values()) {
                if (hasPerClassMetrics(summary)) {
                    return true;
                }
            }
        }

        return false;
    }, [evaluationRequest, metadata, summaryByRun]);

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

    const aggregateStats = useMemo<AggregatedEvaluationResults | null>(() => {
        if (summaryByRun.size === 0 || !metadata?.modelLabels) return null;

        const stats = {} as AggregatedEvaluationResults;

        for (const modelLabel of metadata.modelLabels) {
            const precisions: number[] = [];
            const recalls: number[] = [];
            const f1Scores: number[] = [];
            const accuracies: number[] = [];
            const exactMatchAccuracies: number[] = [];
            const passedCounts: number[] = [];
            const failedCounts: number[] = [];
            const errorCounts: number[] = [];
            const amountOfRetries: number[] = [];
            const truePositives: number[] = [];
            const falsePositives: number[] = [];
            const falseNegatives: number[] = [];
            const trueNegatives: number[] = [];
            const expectedClassifications: number[] = [];
            const predictedClassifications: number[] = [];

            for (const [runNum, runSummaries] of summaryByRun.entries()) {
                const rawSummary = runSummaries.get(modelLabel);
                const modelSummary = rawSummary && isMulticlassEvaluation
                    ? applyClassSelectionToSummary(rawSummary, selectedClasses)
                    : rawSummary;
                if (modelSummary) {
                    precisions.push(modelSummary.precision);
                    recalls.push(modelSummary.recall);
                    f1Scores.push(modelSummary.f1Score);
                    accuracies.push(modelSummary.accuracy);
                    exactMatchAccuracies.push(modelSummary.exactMatchAccuracy);
                    passedCounts.push(modelSummary.passed);
                    failedCounts.push(modelSummary.failed);
                    errorCounts.push(modelSummary.error);
                    if (modelSummary.amountOfRetries !== null && modelSummary.amountOfRetries !== undefined) {
                        amountOfRetries.push(modelSummary.amountOfRetries);
                    }
                    truePositives.push(modelSummary.totalTruePositives);
                    falsePositives.push(modelSummary.totalFalsePositives);
                    falseNegatives.push(modelSummary.totalFalseNegatives);
                    trueNegatives.push(modelSummary.totalTrueNegatives);
                }

                const runTestCases = (testCasesByRun.get(runNum) || []).filter((testCase) => testCase.modelLabel === modelLabel);
                if (runTestCases.length > 0) {
                    const expectedCount = runTestCases.reduce(
                        (sum, testCase) => sum + testCase.correctActivityIds.length + testCase.falseNegativeIds.length,
                        0
                    );
                    const predictedCount = runTestCases.reduce(
                        (sum, testCase) => sum + testCase.correctActivityIds.length + testCase.falsePositiveIds.length,
                        0
                    );

                    expectedClassifications.push(expectedCount);
                    predictedClassifications.push(predictedCount);
                }
            }

            if (precisions.length > 0) {
                const avgPrecision = precisions.reduce((a, b) => a + b, 0) / precisions.length;
                const avgRecall = recalls.reduce((a, b) => a + b, 0) / recalls.length;
                const avgF1Score = f1Scores.reduce((a, b) => a + b, 0) / f1Scores.length;
                const avgAccuracy = accuracies.reduce((a, b) => a + b, 0) / accuracies.length;
                const avgExactMatchAccuracy = exactMatchAccuracies.reduce((a, b) => a + b, 0) / exactMatchAccuracies.length;

                const stdPrecision = Math.sqrt(precisions.reduce((sum, val) => sum + Math.pow(val - avgPrecision, 2), 0) / precisions.length);
                const stdRecall = Math.sqrt(recalls.reduce((sum, val) => sum + Math.pow(val - avgRecall, 2), 0) / recalls.length);
                const stdF1Score = Math.sqrt(f1Scores.reduce((sum, val) => sum + Math.pow(val - avgF1Score, 2), 0) / f1Scores.length);
                const stdAccuracy = Math.sqrt(accuracies.reduce((sum, val) => sum + Math.pow(val - avgAccuracy, 2), 0) / accuracies.length);
                const stdExactMatchAccuracy = Math.sqrt(exactMatchAccuracies.reduce((sum, val) => sum + Math.pow(val - avgExactMatchAccuracy, 2), 0) / exactMatchAccuracies.length);

                const avgPassed = passedCounts.reduce((a, b) => a + b, 0) / passedCounts.length;
                const stdPassed = Math.sqrt(passedCounts.reduce((sum, val) => sum + Math.pow(val - avgPassed, 2), 0) / passedCounts.length);
                const avgFailed = failedCounts.reduce((a, b) => a + b, 0) / failedCounts.length;
                const stdFailed = Math.sqrt(failedCounts.reduce((sum, val) => sum + Math.pow(val - avgFailed, 2), 0) / failedCounts.length);
                const avgErrors = errorCounts.reduce((a, b) => a + b, 0) / errorCounts.length;
                const stdErrors = Math.sqrt(errorCounts.reduce((sum, val) => sum + Math.pow(val - avgErrors, 2), 0) / errorCounts.length);

                const avgAmountOfRetries = amountOfRetries.length > 0 ? amountOfRetries.reduce((a, b) => a + b, 0) / amountOfRetries.length : undefined;
                const stdAmountOfRetries = amountOfRetries.length > 0 ? Math.sqrt(amountOfRetries.reduce((sum, val) => sum + Math.pow(val - (avgAmountOfRetries || 0), 2), 0) / amountOfRetries.length) : undefined;

                const avgTruePositives = truePositives.reduce((a, b) => a + b, 0) / truePositives.length;
                const avgFalsePositives = falsePositives.reduce((a, b) => a + b, 0) / falsePositives.length;
                const avgFalseNegatives = falseNegatives.reduce((a, b) => a + b, 0) / falseNegatives.length;
                const avgTrueNegatives = trueNegatives.reduce((a, b) => a + b, 0) / trueNegatives.length;
                const stdTruePositives = Math.sqrt(truePositives.reduce((sum, val) => sum + Math.pow(val - avgTruePositives, 2), 0) / truePositives.length);
                const stdFalsePositives = Math.sqrt(falsePositives.reduce((sum, val) => sum + Math.pow(val - avgFalsePositives, 2), 0) / falsePositives.length);
                const stdFalseNegatives = Math.sqrt(falseNegatives.reduce((sum, val) => sum + Math.pow(val - avgFalseNegatives, 2), 0) / falseNegatives.length);
                const stdTrueNegatives = Math.sqrt(trueNegatives.reduce((sum, val) => sum + Math.pow(val - avgTrueNegatives, 2), 0) / trueNegatives.length);
                const avgExpectedClassifications = expectedClassifications.length > 0
                    ? expectedClassifications.reduce((a, b) => a + b, 0) / expectedClassifications.length
                    : 0;
                const avgPredictedClassifications = predictedClassifications.length > 0
                    ? predictedClassifications.reduce((a, b) => a + b, 0) / predictedClassifications.length
                    : 0;
                const stdExpectedClassifications = expectedClassifications.length > 0
                    ? Math.sqrt(expectedClassifications.reduce((sum, val) => sum + Math.pow(val - avgExpectedClassifications, 2), 0) / expectedClassifications.length)
                    : 0;
                const stdPredictedClassifications = predictedClassifications.length > 0
                    ? Math.sqrt(predictedClassifications.reduce((sum, val) => sum + Math.pow(val - avgPredictedClassifications, 2), 0) / predictedClassifications.length)
                    : 0;

                stats[modelLabel] = {
                    avgPrecision,
                    stdPrecision,
                    avgRecall,
                    stdRecall,
                    avgF1Score,
                    stdF1Score,
                    avgAccuracy,
                    stdAccuracy,
                    avgExactMatchAccuracy,
                    stdExactMatchAccuracy,
                    avgPassed,
                    stdPassed,
                    avgFailed,
                    stdFailed,
                    avgErrors,
                    stdErrors,
                    avgAmountOfRetries,
                    stdAmountOfRetries,
                    avgTruePositives,
                    stdTruePositives,
                    avgFalsePositives,
                    stdFalsePositives,
                    avgFalseNegatives,
                    stdFalseNegatives,
                    avgTrueNegatives,
                    stdTrueNegatives,
                    avgExpectedClassifications,
                    stdExpectedClassifications,
                    avgPredictedClassifications,
                    stdPredictedClassifications
                }
            }
        }

        return stats;
    }, [summaryByRun, metadata, isMulticlassEvaluation, selectedClasses, testCasesByRun]);

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

    const handleDownloadMarkdownReport = () => {
        const hasSummaries = summaryByRun.size > 0;
        if (testCasesByRun.size === 0 && !hasSummaries) {
            alert("No results yet.");
            return;
        }
        const sections: string[] = [];

        if (metadata) {
            sections.push("# Evaluation Report");
            sections.push(metadata.markdown);
        }

        if (aggregateStats && summaryByRun.size >= 1) {
            sections.push("# Aggregate Statistics Across All Runs");
            for (const [modelLabel, stats] of Object.entries(aggregateStats)) {
                sections.push(`## Model: ${modelLabel}`);
                sections.push(`- Precision: ${stats.avgPrecision.toFixed(3)} ± ${stats.stdPrecision.toFixed(3)}`);
                sections.push(`- Recall: ${stats.avgRecall.toFixed(3)} ± ${stats.stdRecall.toFixed(3)}`);
                sections.push(`- F1-Score: ${stats.avgF1Score.toFixed(3)} ± ${stats.stdF1Score.toFixed(3)}`);
                if (isMulticlassEvaluation && isAllClassesSelected(selectedClasses)) {
                    sections.push(`- Exact Match Accuracy: ${stats.avgExactMatchAccuracy.toFixed(3)} ± ${stats.stdExactMatchAccuracy.toFixed(3)}`);
                } else if (!isMulticlassEvaluation) {
                    sections.push(`- Accuracy: ${stats.avgAccuracy.toFixed(3)} ± ${stats.stdAccuracy.toFixed(3)}`);
                }
                sections.push(`- True Positives: ${stats.avgTruePositives.toFixed(3)} ± ${stats.stdTruePositives.toFixed(3)}`);
                sections.push(`- False Positives: ${stats.avgFalsePositives.toFixed(3)} ± ${stats.stdFalsePositives.toFixed(3)}`);
                sections.push(`- False Negatives: ${stats.avgFalseNegatives.toFixed(3)} ± ${stats.stdFalseNegatives.toFixed(3)}`);
                sections.push(`- True Negatives: ${stats.avgTrueNegatives.toFixed(3)} ± ${stats.stdTrueNegatives.toFixed(3)}`);
                sections.push(`- Expected Classifications: ${stats.avgExpectedClassifications.toFixed(3)} ± ${stats.stdExpectedClassifications.toFixed(3)}`);
                sections.push(`- Predicted Classifications: ${stats.avgPredictedClassifications.toFixed(3)} ± ${stats.stdPredictedClassifications.toFixed(3)}`);
                sections.push(`- Passed: ${stats.avgPassed.toFixed(3)} ± ${stats.stdPassed.toFixed(3)} / ${metadata?.totalTestCases}`);
                sections.push(`- Failed: ${stats.avgFailed.toFixed(3)} ± ${stats.stdFailed.toFixed(3)} / ${metadata?.totalTestCases}`);
                sections.push(`- Errors: ${stats.avgErrors.toFixed(3)} ± ${stats.stdErrors.toFixed(3)} / ${metadata?.totalTestCases}`);
                if (stats.avgAmountOfRetries !== undefined && stats.stdAmountOfRetries !== undefined) {
                    sections.push(`- Amount of Retries: ${stats.avgAmountOfRetries.toFixed(3)} ± ${stats.stdAmountOfRetries.toFixed(3)}`);
                }
            }
        }

        for (const [runNum, runSummaries] of summaryByRun.entries()) {
            sections.push(`# Run ${runNum}`);
            for (const [modelLabel, modelSummary] of runSummaries.entries()) {
                sections.push(`## Summary (${modelLabel})`);
                sections.push(modelSummary.markdown);
            }

            const runTestCases = testCasesByRun.get(runNum) || [];
            const byModel = groupBy(runTestCases, (x) => x.modelLabel);

            Object.keys(byModel).forEach((label) => {
                sections.push(`## Model: ${label}`);
                const cases = byModel[label];
                sections.push(...cases.map((c) => c.markdown));
            });
        }

        const blob = new Blob([sections.join("\n\n")], { type: "text/markdown" });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = "evaluation_report_multi.md";
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    };

    const handleDownloadJsonReport = () => {
        const hasSummaries = summaryByRun.size > 0;
        if (testCasesByRun.size === 0 && !hasSummaries) {
            alert("No results yet.");
            return;
        }

        // Convert Maps to serializable format
        const testCasesByRunObj: Record<number, any[]> = {};
        for (const [runNum, cases] of testCasesByRun.entries()) {
            testCasesByRunObj[runNum] = cases;
        }

        const summariesByRunObj: Record<number, any[]> = {};
        for (const [runNum, runSummaries] of summaryByRun.entries()) {
            summariesByRunObj[runNum] = Array.from(runSummaries.entries()).map(([label, s]) => ({ label, summary: s }));
        }

        const errorsByRunObj: Record<number, any[]> = {};
        for (const [runNum, errs] of errorsByRun.entries()) {
            errorsByRunObj[runNum] = errs;
        }

        const report = {
            metadata,
            testCasesByRun: testCasesByRunObj,
            summariesByRun: summariesByRunObj,
            errorsByRun: errorsByRunObj,
            aggregateStats: aggregateStats || null,
            colors: colors
        };

        const blob = new Blob([JSON.stringify(report, null, 2)], { type: "application/json" });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = "evaluation_report_multi.json";
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }

    const handleUploadJsonReport = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (!file) return;

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
                alert("Failed to load report: " + (err as Error).message);
            }
        };
        reader.readAsText(file);
        setIsFinished(true);
    };

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
                <div className="flex flex-row justify-between items-start flex-wrap mb-4 gap-4">
                    <div className="flex flex-row gap-4 flex-wrap">
                        <Button variant="secondary" disabled={!isFinished} onClick={handleDownloadMarkdownReport}>
                            <FileText className="h-4 w-4" />
                            Download Markdown Report
                        </Button>
                        <Button variant="secondary" disabled={!isFinished} onClick={handleDownloadJsonReport}>
                            <FileText className="h-4 w-4" />
                            Download JSON Report
                        </Button>
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
                                            <p>({Array.from(testCasesByRun.values()).reduce((sum, cases) => sum + cases.length, 0) + Array.from(errorsByRun.values()).reduce((sum, errs) => sum + errs.length, 0)} / {currentStepInfos[0]?.totalTestCases * (evaluationRequest?.models.length || 1) * (metadata?.totalRepetitions || 1)})</p>
                                        </div>
                                    )}
                                </div>
                            )}
                        </>
                    </Button>
                </div>
            </EvaluationConfig>

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
                                                    <Tabs className="w-full" value={selectedDataset || (metadata?.datasets?.[0] ? `dataset-${metadata.datasets[0].id}` : undefined)}
                                                          onValueChange={setSelectedDataset}>
                                                        <TabsList className="w-full h-12 sticky top-24 z-30 mb-4">
                                                            {metadata?.datasets.map?.((dataset) => (
                                                                <TabsTrigger value={`dataset-${dataset.id}`}
                                                                             key={`dataset-${dataset.id}-trigger`}>
                                                                    {dataset.name}
                                                                </TabsTrigger>
                                                            ))}
                                                        </TabsList>

                                                        {metadata?.datasets.map?.((dataset) => (
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

function groupBy<T>(arr: T[], key: (t: T) => string) {
    return arr.reduce<Record<string, T[]>>((acc, item) => {
        const k = key(item);
        // @ts-ignore
        (acc[k] ||= []).push(item);
        return acc;
    }, {});
}
