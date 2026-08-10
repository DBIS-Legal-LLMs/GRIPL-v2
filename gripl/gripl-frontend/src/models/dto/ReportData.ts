import {GdprCategory} from "@/models/GdprCategory";

export interface PerClassMetricValues {
    totalTruePositives?: number;
    totalFalsePositives?: number;
    totalFalseNegatives?: number;
    totalTrueNegatives?: number;
    truePositives?: number;
    falsePositives?: number;
    falseNegatives?: number;
    trueNegatives?: number;
    tp?: number;
    fp?: number;
    fn?: number;
    tn?: number;
    precision?: number;
    recall?: number;
    f1Score?: number;
    f1?: number;
}

export interface EvaluationMetadataReport {
    type: "metadata";
    modelLabels: string[],
    modelTemperatures: (number | undefined)[];
    modelTopPs: (number | undefined)[];
    datasets: { id: number; name: string }[];
    timestamp: string;
    totalTestCases: number;
    defaultEvaluationEndpoint: string;
    seed: number;
    totalRepetitions?: number;
    markdown: string;
}

export interface TestCaseReport {
    type: "testCase";
    testCaseId: number;
    datasetId: number;
    testCaseName?: string;
    imageSrc: string;
    correctActivityIds: string[];
    falsePositiveIds: string[];
    falseNegativeIds: string[];
    expectedNamesWithIds: string[],
    actualNamesWithIds: string[];
    isSuccessful: boolean;
    result: { value: string; reason?: string; classification?: GdprCategory[] }[];
    amountOfRetries: number | null;
    markdown: string;
}

export interface EvaluationReportSummary {
    type: "summary";
    total: number;
    passed: number;
    failed: number;
    error: number;
    amountOfRetries: number | null;
    precision: number;
    recall: number;
    f1Score: number;
    accuracy: number;
    exactMatchAccuracy: number;
    totalTruePositives: number;
    totalFalsePositives: number;
    totalFalseNegatives: number;
    totalTrueNegatives: number;
    perClassMetrics?: Partial<Record<string, PerClassMetricValues>>;
    markdown: string;
}

export interface EvaluationReportStepInfo {
    type: "stepInfo";
    currentTestCaseName: string;
    currentTestCaseId: number;
    currentTestCaseNumber: number;
    totalTestCases: number;
    markdown: string;
}

export interface EvaluationReportError {
    type: "error";
    testCaseId: number;
    datasetId?: number;
    testCaseName?: string;
    errorMessage: string;
    markdown: string;
}

export type EvaluationReport = EvaluationMetadataReport | TestCaseReport | EvaluationReportSummary | EvaluationReportStepInfo | EvaluationReportError;