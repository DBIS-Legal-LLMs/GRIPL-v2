"use client"

import {AggregatedEvaluationResults} from "@/models/evaluation/AggregatedEvaluationResult";
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card";
import ChartMenu from "@/components/evaluation/charts/common/chart-menu";
import html2canvas from "html2canvas"

interface MetricsTableProps {
    aggregatedEvaluationResults: AggregatedEvaluationResults
    showAccuracy?: boolean
    showExactMatchAccuracy?: boolean
    classSpecificView?: boolean
}

export default function MetricsTable({
    aggregatedEvaluationResults,
    showAccuracy = false,
    showExactMatchAccuracy = false,
    classSpecificView = false
}: MetricsTableProps) {
    const chartId = `metrics-table-chart`

    const formatMetric = (mean: number, std: number) => {
        return `${mean.toFixed(3)} ± ${std.toFixed(3)}`
    }

    const handleDownload = async () => {
        const tableElement = document.getElementById("metrics-table-container")
        if (!tableElement) {
            console.error("Table element not found")
            return
        }

        try {
            const canvas = await html2canvas(tableElement, {
                backgroundColor: "#ffffff",
                scale: 2,
            })
            const dataUrl = canvas.toDataURL("image/png")
            const link = document.createElement("a")
            link.download = "metrics-table.png"
            link.href = dataUrl
            link.click()
        } catch (error) {
            console.error("Error downloading table:", error)
        }
    }

    return <Card>
        <CardHeader>
            <div className="flex items-start justify-between">
                <div>
                    <CardTitle>Metrics Table</CardTitle>
                    <CardDescription>Model performance metrics with averages and standard deviations over all runs and test cases.</CardDescription>
                </div>
                <ChartMenu chartId={chartId} onDownload={handleDownload} />
            </div>
        </CardHeader>
        <CardContent>
            <div id="metrics-table-container" className="w-full overflow-x-auto">
                <table className="w-full border-collapse text-foreground">
                    <thead>
                    <tr className="border-b-2 border-border">
                        <th className="text-left py-3 px-4 font-semibold text-foreground">Model</th>
                        <th className="text-right py-3 px-4 font-semibold text-foreground">Precision</th>
                        <th className="text-right py-3 px-4 font-semibold text-foreground">Recall</th>
                        <th className="text-right py-3 px-4 font-semibold text-foreground">F1-Score</th>
                        {showAccuracy && <th className="text-right py-3 px-4 font-semibold text-foreground">Accuracy</th>}
                        {showExactMatchAccuracy && <th className="text-right py-3 px-4 font-semibold text-foreground">Exact Match Accuracy</th>}
                        <th className="text-right py-3 px-4 font-semibold text-foreground">TP</th>
                        <th className="text-right py-3 px-4 font-semibold text-foreground">TN</th>
                        <th className="text-right py-3 px-4 font-semibold text-foreground">FP</th>
                        <th className="text-right py-3 px-4 font-semibold text-foreground">FN</th>
                        {!classSpecificView && <th className="text-right py-3 px-4 font-semibold text-foreground">Expected</th>}
                        {!classSpecificView && <th className="text-right py-3 px-4 font-semibold text-foreground">Predicted</th>}
                        {!classSpecificView && <th className="text-right py-3 px-4 font-semibold text-foreground">Passed</th>}
                        {!classSpecificView && <th className="text-right py-3 px-4 font-semibold text-foreground">Failed</th>}
                        {!classSpecificView && <th className="text-right py-3 px-4 font-semibold text-foreground">Errors</th>}
                        {!classSpecificView && <th className="text-right py-3 px-4 font-semibold text-foreground">Retries</th>}
                    </tr>
                    </thead>
                    <tbody>
                    {Object.entries(aggregatedEvaluationResults).map(([modelName, metrics]) => (
                        <tr key={modelName} className="border-b border-border hover:bg-muted/40">
                            <td className="py-2 px-4 text-foreground">{modelName}</td>
                            <td className="text-right py-2 px-4 font-mono text-sm text-foreground">
                                {formatMetric(metrics.avgPrecision, metrics.stdPrecision)}
                            </td>
                            <td className="text-right py-2 px-4 font-mono text-sm text-foreground">
                                {formatMetric(metrics.avgRecall, metrics.stdRecall)}
                            </td>
                            <td className="text-right py-2 px-4 font-mono text-sm text-foreground">
                                {formatMetric(metrics.avgF1Score, metrics.stdF1Score)}
                            </td>
                            {showAccuracy && (
                                <td className="text-right py-2 px-4 font-mono text-sm text-foreground">
                                    {formatMetric(metrics.avgAccuracy, metrics.stdAccuracy)}
                                </td>
                            )}
                            {showExactMatchAccuracy && (
                                <td className="text-right py-2 px-4 font-mono text-sm text-foreground">
                                    {formatMetric(metrics.avgExactMatchAccuracy, metrics.stdExactMatchAccuracy)}
                                </td>
                            )}
                            <td className="text-right py-2 px-4 font-mono text-sm text-foreground">
                                {formatMetric(metrics.avgTruePositives, metrics.stdTruePositives)}
                            </td>
                            <td className="text-right py-2 px-4 font-mono text-sm text-foreground">
                                {formatMetric(metrics.avgTrueNegatives, metrics.stdTrueNegatives)}
                            </td>
                            <td className="text-right py-2 px-4 font-mono text-sm text-foreground">
                                {formatMetric(metrics.avgFalsePositives, metrics.stdFalsePositives)}
                            </td>
                            <td className="text-right py-2 px-4 font-mono text-sm text-foreground">
                                {formatMetric(metrics.avgFalseNegatives, metrics.stdFalseNegatives)}
                            </td>
                            {!classSpecificView && (
                                <td className="text-right py-2 px-4 font-mono text-sm text-foreground">
                                    {formatMetric(metrics.avgExpectedClassifications, metrics.stdExpectedClassifications)}
                                </td>
                            )}
                            {!classSpecificView && (
                                <td className="text-right py-2 px-4 font-mono text-sm text-foreground">
                                    {formatMetric(metrics.avgPredictedClassifications, metrics.stdPredictedClassifications)}
                                </td>
                            )}
                            {!classSpecificView && (
                                <td className="text-right py-2 px-4 font-mono text-sm text-foreground">
                                    {formatMetric(metrics.avgPassed, metrics.stdPassed)}
                                </td>
                            )}
                            {!classSpecificView && (
                                <td className="text-right py-2 px-4 font-mono text-sm text-foreground">
                                    {formatMetric(metrics.avgFailed, metrics.stdFailed)}
                                </td>
                            )}
                            {!classSpecificView && (
                                <td className="text-right py-2 px-4 font-mono text-sm text-foreground">
                                    {formatMetric(metrics.avgErrors, metrics.stdErrors)}
                                </td>
                            )}
                            {!classSpecificView && metrics.avgAmountOfRetries !== undefined && metrics.stdAmountOfRetries !== undefined &&
                                <td className="text-right py-2 px-4 font-mono text-sm text-foreground">
                                    {formatMetric(metrics.avgAmountOfRetries, metrics.stdAmountOfRetries)}
                                </td>
                            }
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </CardContent>
    </Card>
}
