import {EvaluationMetadataReport, EvaluationReport} from "@/models/dto/ReportData";

export type EvaluationRunStatus = "QUEUED" | "RUNNING" | "DONE" | "CANCELLED" | "INTERRUPTED" | "ERROR";

export const ACTIVE_RUN_STATUSES: EvaluationRunStatus[] = ["QUEUED", "RUNNING"];
export const RESUMABLE_RUN_STATUSES: EvaluationRunStatus[] = ["CANCELLED", "INTERRUPTED", "ERROR"];

export const isActiveRun = (status: EvaluationRunStatus | undefined) =>
    status !== undefined && ACTIVE_RUN_STATUSES.includes(status);
export const isResumableRun = (status: EvaluationRunStatus | undefined) =>
    status !== undefined && RESUMABLE_RUN_STATUSES.includes(status);

/** One evaluation run as listed by GET /api/gdpr/evaluation/runs. */
export interface EvaluationRun {
    id: number;
    /** Name given by the user; used for the file names of the reports. */
    name: string | null;
    status: EvaluationRunStatus;
    modelLabels: string[];
    datasetNames: string[];
    /** Test cases with a result or an error so far, over all models and repetitions. */
    completedTestCases: number;
    /** Test cases × models × repetitions; null until the run has started. */
    totalTestCases: number | null;
    errorMessage: string | null;
    createdAt: string;
    startedAt: string | null;
    finishedAt: string | null;
}

export interface ModelReportEnvelope {
    modelLabel: string;
    report: EvaluationReport;
    runNumber: number;
}

export interface EvaluationRunDetail {
    run: EvaluationRun;
    metadata: EvaluationMetadataReport | null;
    /** stepInfo envelopes of the test cases being evaluated right now. */
    currentSteps: ModelReportEnvelope[];
}

/** A stored testCase, error or summary event, in the same form as the NDJSON stream. */
export interface EvaluationRunEvent extends ModelReportEnvelope {
    id: number;
}
