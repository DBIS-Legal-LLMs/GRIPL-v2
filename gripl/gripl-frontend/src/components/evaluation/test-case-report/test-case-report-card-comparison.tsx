import {ListChecks, Target} from "lucide-react";
import {Card} from "@/components/ui/card";
import {TestCaseReport} from "@/models/dto/ReportData";
import {Badge} from "@/components/ui/badge";
import {GdprCategory, toFrontendGdprCategory} from "@/models/GdprCategory";

const categoryBadgeClass: Record<GdprCategory, string> = {
    Collection:   "bg-emerald-600 text-white border-transparent",
    Storage:      "bg-blue-600 text-white border-transparent",
    Usage:        "bg-amber-500 text-black border-transparent",
    Transferal:   "bg-violet-600 text-white border-transparent",
    Modification: "bg-sky-600 text-white border-transparent",
    Deletion:     "bg-red-600 text-white border-transparent",
    Access:       "bg-teal-600 text-white border-transparent",
};

function extractElementId(nameWithId: string): string | null {
    const match = nameWithId.match(/\(([^)]+)\)\s*$/);
    return match?.[1] ?? null;
}

/** Strips the trailing "(id)" from "Name (id)" for display. */
function extractDisplayName(nameWithId: string): string {
    return nameWithId.replace(/\s*\([^)]+\)\s*$/, "").trim();
}

function normalizeBadgeClass(raw: string): string {
    const cat = toFrontendGdprCategory(raw);
    return cat ? (categoryBadgeClass[cat] ?? "") : "";
}

function normalizeLabel(raw: string): string {
    return toFrontendGdprCategory(raw) ?? raw;
}

function addClassification(target: Record<string, string[]>, value: string) {
    const separatorIndex = value.indexOf(":");
    if (separatorIndex === -1) {
        return;
    }

    const elementId = value.slice(0, separatorIndex);
    const classification = value.slice(separatorIndex + 1);
    if (!classification) {
        return;
    }

    target[elementId] = Array.from(new Set([...(target[elementId] ?? []), classification]));
}

interface TestCaseReportCardComparisonProps {
    report: TestCaseReport
}

export default function TestCaseReportCardComparison({ report }: TestCaseReportCardComparisonProps) {
    const correctlyDetectedIds = new Set(report.correctActivityIds.map((value) => value.split(":")[0]));
    const expectedClassifications = [...report.correctActivityIds, ...report.falseNegativeIds].reduce((acc, value) => {
        addClassification(acc, value);
        return acc;
    }, {} as Record<string, string[]>);
    const actualClassifications = report.result.reduce((acc, value) => {
        if ((value.classification ?? []).length > 0) {
            acc[value.value] = Array.from(new Set(value.classification ?? []));
        }
        return acc;
    }, {} as Record<string, string[]>);

    return <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
            <h3 className="font-semibold text-sm mb-3 flex items-center gap-2">
                <Target className="h-4 w-4" />
                Expected Activity-Classifications
            </h3>
            <Card className="p-3">
                <div className="space-y-1">
                    {report.expectedNamesWithIds.map((item, index) => {
                        const elementId = extractElementId(item)
                        const classifications = elementId ? (expectedClassifications[elementId] ?? []) : []
                        const isCorrect = elementId ? correctlyDetectedIds.has(elementId) : false
                        return <div key={index} className={`text-sm ${isCorrect ? 'text-chart-success' : 'text-warning'}`}>
                            <div>• {extractDisplayName(item)}</div>
                            {classifications.length > 0 && (
                                <div className="mt-1 ml-3 flex flex-wrap gap-1">
                                    {classifications.map((classification) => (
                                        <Badge key={`${elementId}-${classification}`} variant="outline" className={`text-[10px] ${normalizeBadgeClass(classification)}`}>
                                            {normalizeLabel(classification)}
                                        </Badge>
                                    ))}
                                </div>
                            )}
                        </div>
                    })}
                </div>
            </Card>
        </div>

        <div>
            <h3 className="font-semibold text-sm mb-3 flex items-center gap-2">
                <ListChecks className="h-4 w-4" />
                Predicted Activity-Classifications
            </h3>
            <Card className="p-3">
                <div className="space-y-1">
                    {report.actualNamesWithIds.map((item, index) => {
                        const elementId = extractElementId(item)
                        const classifications = elementId ? (actualClassifications[elementId] ?? []) : []
                        const isCorrect = elementId ? correctlyDetectedIds.has(elementId) : false
                        return <div key={index} className={`text-sm ${isCorrect ? 'text-chart-success' : 'text-destructive'}`}>
                            <div>• {extractDisplayName(item)}</div>
                            {classifications.length > 0 && (
                                <div className="mt-1 ml-3 flex flex-wrap gap-1">
                                    {classifications.map((classification) => (
                                        <Badge key={`${elementId}-${classification}`} variant="outline" className={`text-[10px] ${normalizeBadgeClass(classification)}`}>
                                            {normalizeLabel(classification)}
                                        </Badge>
                                    ))}
                                </div>
                            )}
                        </div>
                    })}
                </div>
            </Card>
        </div>
    </div>
}