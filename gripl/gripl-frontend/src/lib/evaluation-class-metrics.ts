import { GDPR_CATEGORIES, GdprCategory } from "@/models/GdprCategory";
import { EvaluationReportSummary, PerClassMetricValues } from "@/models/dto/ReportData";

export const ALL_CLASSES_FILTER = "__ALL_CLASSES__";

export interface ClassMetricValues {
    tp: number;
    fp: number;
    fn: number;
    tn: number;
    precision: number;
    recall: number;
    f1Score: number;
}

export function classOptions(): GdprCategory[] {
    return GDPR_CATEGORIES.map((category) => category.value);
}

function asNumber(value: unknown): number {
    if (typeof value === "number" && Number.isFinite(value)) {
        return value;
    }
    return 0;
}

function normalizeClassMetrics(metric?: PerClassMetricValues): ClassMetricValues {
    return {
        tp: asNumber(metric?.totalTruePositives ?? metric?.truePositives ?? metric?.tp),
        fp: asNumber(metric?.totalFalsePositives ?? metric?.falsePositives ?? metric?.fp),
        fn: asNumber(metric?.totalFalseNegatives ?? metric?.falseNegatives ?? metric?.fn),
        tn: asNumber(metric?.totalTrueNegatives ?? metric?.trueNegatives ?? metric?.tn),
        precision: asNumber(metric?.precision),
        recall: asNumber(metric?.recall),
        f1Score: asNumber(metric?.f1Score ?? metric?.f1),
    };
}

function keyCandidates(className: string): string[] {
    return [className, className.toUpperCase(), className.toLowerCase()];
}

function getPerClassMetric(summary: EvaluationReportSummary, className: string): ClassMetricValues | null {
    const metrics = summary.perClassMetrics;
    if (!metrics) {
        return null;
    }

    for (const key of keyCandidates(className)) {
        const value = metrics[key];
        if (value) {
            return normalizeClassMetrics(value);
        }
    }

    return null;
}

export function hasPerClassMetrics(summary: EvaluationReportSummary): boolean {
    return !!summary.perClassMetrics && Object.keys(summary.perClassMetrics).length > 0;
}

export function isAllClassesSelected(selectedClasses: string[]): boolean {
    return selectedClasses.length === 0 || selectedClasses.includes(ALL_CLASSES_FILTER);
}

export function applyClassSelectionToSummary(
    summary: EvaluationReportSummary,
    selectedClasses: string[]
): EvaluationReportSummary {
    if (isAllClassesSelected(selectedClasses)) {
        return summary;
    }

    const selectedMetrics = selectedClasses
        .map((selectedClass) => getPerClassMetric(summary, selectedClass))
        .filter((metric): metric is ClassMetricValues => metric !== null);

    if (selectedMetrics.length === 0) {
        return summary;
    }

    const tp = selectedMetrics.reduce((sum, metric) => sum + metric.tp, 0);
    const fp = selectedMetrics.reduce((sum, metric) => sum + metric.fp, 0);
    const fn = selectedMetrics.reduce((sum, metric) => sum + metric.fn, 0);
    const tn = selectedMetrics.reduce((sum, metric) => sum + metric.tn, 0);

    const precision = tp + fp > 0 ? tp / (tp + fp) : 0;
    const recall = tp + fn > 0 ? tp / (tp + fn) : 0;
    const f1Score = precision + recall > 0 ? (2 * precision * recall) / (precision + recall) : 0;

    return {
        ...summary,
        precision,
        recall,
        f1Score,
        totalTruePositives: tp,
        totalFalsePositives: fp,
        totalFalseNegatives: fn,
        totalTrueNegatives: tn,
    };
}
