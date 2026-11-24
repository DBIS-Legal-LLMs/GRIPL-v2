import {
    EvaluationMetadataReport,
    EvaluationReportError,
    EvaluationReportSummary,
    TestCaseReport
} from "@/models/dto/ReportData";
import {AggregatedEvaluationResults} from "@/models/evaluation/AggregatedEvaluationResult";
import {EvaluationRunDetail, EvaluationRunEvent} from "@/models/dto/EvaluationRun";
import {applyClassSelectionToSummary, isAllClassesSelected} from "@/lib/evaluation-class-metrics";
import {authenticatedFetch} from "@/lib/authenticated-fetch";
import {extractErrorDetails} from "@/lib/http-error";

export type TestCasesByRun = Map<number, (TestCaseReport & { modelLabel: string })[]>;
export type SummaryByRun = Map<number, Map<string, EvaluationReportSummary>>;
export type ErrorsByRun = Map<number, (EvaluationReportError & { modelLabel: string })[]>;

/** Everything the result views and the report downloads are built from. */
export interface EvaluationResultData {
    metadata: EvaluationMetadataReport | null;
    testCasesByRun: TestCasesByRun;
    summaryByRun: SummaryByRun;
    errorsByRun: ErrorsByRun;
}

const RUNS_URL = "/api/gdpr/evaluation/runs";
const EVENT_PAGE_SIZE = 500;

export function isMulticlassMetadata(metadata: EvaluationMetadataReport | null | undefined, endpoint?: string) {
    return (endpoint ?? metadata?.defaultEvaluationEndpoint)?.includes("multiclass") === true;
}

/** Sorts stored testCase, summary and error events into the per-repetition maps. */
export function groupEvents(events: EvaluationRunEvent[]) {
    const testCasesByRun: TestCasesByRun = new Map();
    const summaryByRun: SummaryByRun = new Map();
    const errorsByRun: ErrorsByRun = new Map();
    for (const {modelLabel, report, runNumber} of events) {
        if (report.type === "testCase") {
            testCasesByRun.set(runNumber, [...(testCasesByRun.get(runNumber) ?? []), {...(report as TestCaseReport), modelLabel}]);
        } else if (report.type === "summary") {
            const runSummaries = summaryByRun.get(runNumber) ?? new Map<string, EvaluationReportSummary>();
            runSummaries.set(modelLabel, report as EvaluationReportSummary);
            summaryByRun.set(runNumber, runSummaries);
        } else if (report.type === "error") {
            errorsByRun.set(runNumber, [...(errorsByRun.get(runNumber) ?? []), {...(report as EvaluationReportError), modelLabel}]);
        }
    }
    return {testCasesByRun, summaryByRun, errorsByRun};
}

async function fetchJson<T>(url: string): Promise<T> {
    const response = await authenticatedFetch(url);
    if (!response.ok) throw new Error(await extractErrorDetails(response));
    return await response.json() as T;
}

/** Loads all stored results of a server-side run, without touching what the page shows. */
export async function fetchRunResults(id: number): Promise<EvaluationResultData> {
    const detail = await fetchJson<EvaluationRunDetail>(`${RUNS_URL}/${id}`);
    const events: EvaluationRunEvent[] = [];
    let after = 0;
    while (true) {
        const page = await fetchJson<EvaluationRunEvent[]>(`${RUNS_URL}/${id}/events?after=${after}&limit=${EVENT_PAGE_SIZE}`);
        events.push(...page);
        if (page.length > 0) after = page[page.length - 1].id;
        if (page.length < EVENT_PAGE_SIZE) break;
    }
    return {metadata: detail.metadata, ...groupEvents(events)};
}

const mean = (xs: number[]) => xs.reduce((a, b) => a + b, 0) / xs.length;
const std = (xs: number[]) => {
    const m = mean(xs);
    return Math.sqrt(xs.reduce((sum, val) => sum + Math.pow(val - m, 2), 0) / xs.length);
};
const meanOrUndefined = (xs: number[]) => xs.length > 0 ? mean(xs) : undefined;
const stdOrUndefined = (xs: number[]) => xs.length > 0 ? std(xs) : undefined;
const meanOrZero = (xs: number[]) => xs.length > 0 ? mean(xs) : 0;
const stdOrZero = (xs: number[]) => xs.length > 0 ? std(xs) : 0;

/** Mean and standard deviation of every metric across the repetitions, per model. */
export function computeAggregateStats(
    data: EvaluationResultData,
    isMulticlass: boolean,
    selectedClasses: string[],
): AggregatedEvaluationResults | null {
    const {summaryByRun, testCasesByRun, metadata} = data;
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
        const contextUtilizations: number[] = [];
        const faithfulnesses: number[] = [];
        const perTypeValues = new Map<string, {
            displayName: string;
            precisions: number[];
            recalls: number[];
            f1Scores: number[];
            tps: number[];
            fps: number[];
            fns: number[];
            tns: number[];
        }>();
        const expectedClassifications: number[] = [];
        const predictedClassifications: number[] = [];

        for (const [runNum, runSummaries] of summaryByRun.entries()) {
            const rawSummary = runSummaries.get(modelLabel);
            const modelSummary = rawSummary && isMulticlass
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
                if (modelSummary.ragMetrics?.contextUtilizationMean !== null && modelSummary.ragMetrics?.contextUtilizationMean !== undefined) {
                    contextUtilizations.push(modelSummary.ragMetrics.contextUtilizationMean);
                }
                if (modelSummary.ragMetrics?.faithfulnessMean !== null && modelSummary.ragMetrics?.faithfulnessMean !== undefined) {
                    faithfulnesses.push(modelSummary.ragMetrics.faithfulnessMean);
                }
                if (modelSummary.perElementType) {
                    for (const [key, typeSummary] of Object.entries(modelSummary.perElementType)) {
                        let bucket = perTypeValues.get(key);
                        if (!bucket) {
                            bucket = {displayName: typeSummary.displayName, precisions: [], recalls: [], f1Scores: [], tps: [], fps: [], fns: [], tns: []};
                            perTypeValues.set(key, bucket);
                        }
                        bucket.precisions.push(typeSummary.precision);
                        bucket.recalls.push(typeSummary.recall);
                        bucket.f1Scores.push(typeSummary.f1Score);
                        bucket.tps.push(typeSummary.truePositives);
                        bucket.fps.push(typeSummary.falsePositives);
                        bucket.fns.push(typeSummary.falseNegatives);
                        bucket.tns.push(typeSummary.trueNegatives);
                    }
                }
            }

            const runTestCases = (testCasesByRun.get(runNum) || []).filter((testCase) => testCase.modelLabel === modelLabel);
            if (runTestCases.length > 0) {
                expectedClassifications.push(runTestCases.reduce(
                    (sum, testCase) => sum + testCase.correctActivityIds.length + testCase.falseNegativeIds.length, 0));
                predictedClassifications.push(runTestCases.reduce(
                    (sum, testCase) => sum + testCase.correctActivityIds.length + testCase.falsePositiveIds.length, 0));
            }
        }

        if (precisions.length === 0) continue;

        const perElementType = perTypeValues.size > 0
            ? Object.fromEntries([...perTypeValues.entries()].map(([key, v]) => [key, {
                displayName: v.displayName,
                avgPrecision: mean(v.precisions),
                stdPrecision: std(v.precisions),
                avgRecall: mean(v.recalls),
                stdRecall: std(v.recalls),
                avgF1Score: mean(v.f1Scores),
                stdF1Score: std(v.f1Scores),
                avgTruePositives: mean(v.tps),
                avgFalsePositives: mean(v.fps),
                avgFalseNegatives: mean(v.fns),
                avgTrueNegatives: mean(v.tns),
                runsCounted: v.precisions.length
            }]))
            : undefined;

        stats[modelLabel] = {
            avgPrecision: mean(precisions),
            stdPrecision: std(precisions),
            avgRecall: mean(recalls),
            stdRecall: std(recalls),
            avgF1Score: mean(f1Scores),
            stdF1Score: std(f1Scores),
            avgAccuracy: mean(accuracies),
            stdAccuracy: std(accuracies),
            avgExactMatchAccuracy: mean(exactMatchAccuracies),
            stdExactMatchAccuracy: std(exactMatchAccuracies),
            avgPassed: mean(passedCounts),
            stdPassed: std(passedCounts),
            avgFailed: mean(failedCounts),
            stdFailed: std(failedCounts),
            avgErrors: mean(errorCounts),
            stdErrors: std(errorCounts),
            avgAmountOfRetries: meanOrUndefined(amountOfRetries),
            stdAmountOfRetries: stdOrUndefined(amountOfRetries),
            avgContextUtilization: meanOrUndefined(contextUtilizations),
            stdContextUtilization: stdOrUndefined(contextUtilizations),
            avgTruePositives: mean(truePositives),
            stdTruePositives: std(truePositives),
            avgFalsePositives: mean(falsePositives),
            stdFalsePositives: std(falsePositives),
            avgFalseNegatives: mean(falseNegatives),
            stdFalseNegatives: std(falseNegatives),
            avgTrueNegatives: mean(trueNegatives),
            stdTrueNegatives: std(trueNegatives),
            avgFaithfulness: meanOrUndefined(faithfulnesses),
            stdFaithfulness: stdOrUndefined(faithfulnesses),
            ragRunsCounted: Math.max(contextUtilizations.length, faithfulnesses.length),
            perElementType,
            avgExpectedClassifications: meanOrZero(expectedClassifications),
            stdExpectedClassifications: stdOrZero(expectedClassifications),
            avgPredictedClassifications: meanOrZero(predictedClassifications),
            stdPredictedClassifications: stdOrZero(predictedClassifications),
        };
    }

    return stats;
}

export function hasResults(data: EvaluationResultData) {
    return data.testCasesByRun.size > 0 || data.summaryByRun.size > 0;
}

export function buildMarkdownReport(
    data: EvaluationResultData,
    aggregateStats: AggregatedEvaluationResults | null,
    isMulticlass: boolean,
    selectedClasses: string[],
): string {
    const {metadata, summaryByRun, testCasesByRun} = data;
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
            if (isMulticlass && isAllClassesSelected(selectedClasses)) {
                sections.push(`- Exact Match Accuracy: ${stats.avgExactMatchAccuracy.toFixed(3)} ± ${stats.stdExactMatchAccuracy.toFixed(3)}`);
            } else if (!isMulticlass) {
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
            if (stats.ragRunsCounted > 0) {
                sections.push(`\n### RAG Metrics (averaged across ${stats.ragRunsCounted} run(s))`);
                if (stats.avgContextUtilization !== undefined && stats.stdContextUtilization !== undefined) {
                    sections.push(`- Context Utilization: ${stats.avgContextUtilization.toFixed(3)} ± ${stats.stdContextUtilization.toFixed(3)}`);
                }
                if (stats.avgFaithfulness !== undefined && stats.stdFaithfulness !== undefined) {
                    sections.push(`- Faithfulness: ${stats.avgFaithfulness.toFixed(3)} ± ${stats.stdFaithfulness.toFixed(3)}`);
                }
            }
            if (stats.perElementType && Object.keys(stats.perElementType).length > 0) {
                const rows = Object.values(stats.perElementType).map((t) =>
                    `| ${t.displayName} | ${t.avgPrecision.toFixed(3)} ± ${t.stdPrecision.toFixed(3)} | ${t.avgRecall.toFixed(3)} ± ${t.stdRecall.toFixed(3)} | ${t.avgF1Score.toFixed(3)} ± ${t.stdF1Score.toFixed(3)} | ${t.avgTruePositives.toFixed(1)} | ${t.avgFalsePositives.toFixed(1)} | ${t.avgFalseNegatives.toFixed(1)} | ${t.avgTrueNegatives.toFixed(1)} |`
                );
                sections.push([
                    `### Metrics by Element Type (averaged across runs)`,
                    `| Type | Precision | Recall | F1-Score | TP | FP | FN | TN |`,
                    `|------|-----------|--------|----------|----|----|----|----|`,
                    ...rows
                ].join("\n"));
            }
        }
    }

    const runNumbers = [...new Set([...summaryByRun.keys(), ...testCasesByRun.keys()])].sort((a, b) => a - b);
    for (const runNum of runNumbers) {
        sections.push(`# Run ${runNum}`);
        for (const [modelLabel, modelSummary] of (summaryByRun.get(runNum) ?? new Map()).entries()) {
            sections.push(`## Summary (${modelLabel})`);
            sections.push(modelSummary.markdown);
        }

        const byModel = new Map<string, string[]>();
        for (const testCase of testCasesByRun.get(runNum) ?? []) {
            byModel.set(testCase.modelLabel, [...(byModel.get(testCase.modelLabel) ?? []), testCase.markdown]);
        }
        byModel.forEach((cases, label) => {
            sections.push(`## Model: ${label}`);
            sections.push(...cases);
        });
    }

    return sections.join("\n\n");
}

/** The JSON report; "Upload JSON Report" reads the same format back. */
export function buildJsonReport(
    data: EvaluationResultData,
    aggregateStats: AggregatedEvaluationResults | null,
    colors: Record<string, string>,
): string {
    const testCasesByRun: Record<number, unknown[]> = {};
    data.testCasesByRun.forEach((cases, runNum) => { testCasesByRun[runNum] = cases; });

    const summariesByRun: Record<number, unknown[]> = {};
    data.summaryByRun.forEach((runSummaries, runNum) => {
        summariesByRun[runNum] = Array.from(runSummaries.entries()).map(([label, summary]) => ({label, summary}));
    });

    const errorsByRun: Record<number, unknown[]> = {};
    data.errorsByRun.forEach((errs, runNum) => { errorsByRun[runNum] = errs; });

    return JSON.stringify({
        metadata: data.metadata,
        testCasesByRun,
        summariesByRun,
        errorsByRun,
        aggregateStats: aggregateStats || null,
        colors,
    }, null, 2);
}

export function downloadTextFile(fileName: string, content: string, type: string) {
    const blob = new Blob([content], {type});
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}

/** The name with characters that are not allowed in file names replaced by "_"; "" for no name. */
export function safeFileName(name: string | null | undefined) {
    return (name ?? "").trim().replace(/[\\/:*?"<>|\u0000-\u001f]+/g, "_").replace(/^\.+/, "");
}

/**
 * e.g. "GPT vs Llama_2026-11-24" for a named run, evaluation_report_run-12_2026-11-24 otherwise.
 * Characters that are not allowed in file names are replaced with "_".
 */
export function reportFileBaseName(runId: number | null, timestamp: string | null | undefined, name?: string | null) {
    const date = timestamp ? new Date(timestamp).toISOString().slice(0, 10) : null;
    const safeName = safeFileName(name);
    if (safeName) return [safeName, date].filter(Boolean).join("_");
    return ["evaluation_report", runId !== null ? `run-${runId}` : null, date].filter(Boolean).join("_");
}
