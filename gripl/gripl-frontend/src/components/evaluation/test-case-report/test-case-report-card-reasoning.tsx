import {Brain} from "lucide-react";
import {TestCaseReport} from "@/models/dto/ReportData";
import {Badge} from "@/components/ui/badge";
import {GdprCategory} from "@/models/GdprCategory";

const categoryBadgeClass: Record<GdprCategory, string> = {
    Collection: "bg-emerald-600 text-white border-transparent",
    Storage: "bg-blue-600 text-white border-transparent",
    Usage: "bg-amber-500 text-black border-transparent",
    Transferal: "bg-violet-600 text-white border-transparent",
    Modification: "bg-sky-600 text-white border-transparent",
    Deletion: "bg-red-600 text-white border-transparent",
    Access: "bg-teal-600 text-white border-transparent",
};

interface TestCaseReportCardReasoningProps {
    report: TestCaseReport
}

export default function TestCaseReportCardReasoning({ report }: TestCaseReportCardReasoningProps) {

    return report.result && report.result.length > 0 && <div>
        <h3 className="font-semibold text-sm mb-3 flex items-center gap-2">
            <Brain className="h-4 w-4" />
            AI Model Reasoning
        </h3>
        <div className="rounded-lg border">
            <table className="w-full text-foreground">
                <thead>
                    <tr className="bg-muted">
                        <th className="text-left text-sm font-semibold p-2 text-foreground">Activity / Classification</th>
                        <th className="text-left text-sm font-semibold p-2 text-foreground">Reasoning</th>
                    </tr>
                </thead>
                <tbody>
                    {report.result.map((result, index) => {
                        const matchedName =
                            report.actualNamesWithIds.find((nameWithId) => nameWithId.includes(result.value)) ||
                            result.value

                        const isFalsePositive = report.falsePositiveIds?.includes(result.value) || false

                        return <tr key={index} className={`border-t border-border ${isFalsePositive && "bg-destructive/30"}`}>
                            <td className="p-2 align-top text-foreground">
                                <div className="mb-1 text-sm font-medium">{matchedName}</div>
                                {result.classification && result.classification.length > 0 && (
                                    <div className="flex flex-wrap gap-1">
                                        {result.classification.map((classification) => (
                                            <Badge key={classification} variant="outline" className={`text-xs ${categoryBadgeClass[classification]}`}>
                                                {classification}
                                            </Badge>
                                        ))}
                                    </div>
                                )}
                            </td>
                            <td className="text-sm p-2 text-foreground">{result.reason || "No reasoning provided"}</td>
                        </tr>
                    })}
                </tbody>
            </table>
        </div>
    </div>
}