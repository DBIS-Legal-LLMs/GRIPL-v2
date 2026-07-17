import { EvaluationReportSummary } from "@/models/dto/ReportData";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import RagMetricsCard from "./rag-metrics-card";

interface SummaryReportCardSingleProps {
    reportSummary: EvaluationReportSummary;
}

export default function EvaluationReportSummaryCardSingle({ reportSummary }: SummaryReportCardSingleProps) {
    const formatPercentage = (value: number) => `${(value * 100).toFixed(1)}%`;
    const formatDecimal = (value: number) => value.toFixed(3);
    const successRate = reportSummary.total > 0 ? reportSummary.passed / reportSummary.total : 0;

    return (
        <div className="space-y-6">
            <Card>
                <CardHeader>
                    <CardTitle>Evaluation Summary</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <div className="text-center">
                            <div className="text-2xl font-bold text-chart-success">{reportSummary.passed}</div>
                            <div className="text-sm text-muted-foreground">Passed</div>
                        </div>
                        <div className="text-center">
                            <div className="text-2xl font-bold text-chart-error">{reportSummary.failed}</div>
                            <div className="text-sm text-muted-foreground">Failed</div>
                        </div>
                        <div className="text-center">
                            <div className="text-2xl font-bold text-chart-warning">{reportSummary.error}</div>
                            <div className="text-sm text-muted-foreground">Errors</div>
                        </div>
                        <div className="text-center">
                            <div className="text-2xl font-bold">{reportSummary.total}</div>
                            <div className="text-sm text-muted-foreground">Total</div>
                        </div>
                    </div>
                    <div className="mt-4">
                        <div className="flex justify-between mb-2">
                            <span>Success Rate</span>
                            <span>{formatPercentage(successRate)}</span>
                        </div>
                        <Progress value={successRate * 100} className="h-2" />
                    </div>
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle>Performance Metrics</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
                        <div className="text-center">
                            <div className="text-3xl font-bold text-chart-metric-4">{formatDecimal(reportSummary.accuracy)}</div>
                            <div className="text-sm text-muted-foreground">Accuracy</div>
                            <Progress value={reportSummary.accuracy * 100} className="h-2 mt-2" />
                        </div>
                        <div className="text-center">
                            <div className="text-3xl font-bold text-chart-metric-1">{formatDecimal(reportSummary.precision)}</div>
                            <div className="text-sm text-muted-foreground">Precision</div>
                            <Progress value={reportSummary.precision * 100} className="h-2 mt-2" />
                        </div>
                        <div className="text-center">
                            <div className="text-3xl font-bold text-chart-metric-2">{formatDecimal(reportSummary.recall)}</div>
                            <div className="text-sm text-muted-foreground">Recall</div>
                            <Progress value={reportSummary.recall * 100} className="h-2 mt-2" />
                        </div>
                        <div className="text-center">
                            <div className="text-3xl font-bold text-chart-metric-3">{formatDecimal(reportSummary.f1Score)}</div>
                            <div className="text-sm text-muted-foreground">F1-Score</div>
                            <Progress value={reportSummary.f1Score * 100} className="h-2 mt-2" />
                        </div>
                    </div>
                </CardContent>
            </Card>

            {reportSummary.perElementType && Object.keys(reportSummary.perElementType).length > 0 && (
                <Card>
                    <CardHeader>
                        <CardTitle>Metrics by Element Type</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead>Type</TableHead>
                                    <TableHead className="text-right">TP</TableHead>
                                    <TableHead className="text-right">FP</TableHead>
                                    <TableHead className="text-right">FN</TableHead>
                                    <TableHead className="text-right">TN</TableHead>
                                    <TableHead className="text-right">Precision</TableHead>
                                    <TableHead className="text-right">Recall</TableHead>
                                    <TableHead className="text-right">F1-Score</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {Object.entries(reportSummary.perElementType).map(([key, typeSummary]) => (
                                    <TableRow key={key}>
                                        <TableCell className="font-medium">{typeSummary.displayName}</TableCell>
                                        <TableCell className="text-right">{typeSummary.truePositives}</TableCell>
                                        <TableCell className="text-right">{typeSummary.falsePositives}</TableCell>
                                        <TableCell className="text-right">{typeSummary.falseNegatives}</TableCell>
                                        <TableCell className="text-right">{typeSummary.trueNegatives}</TableCell>
                                        <TableCell className="text-right">{formatDecimal(typeSummary.precision)}</TableCell>
                                        <TableCell className="text-right">{formatDecimal(typeSummary.recall)}</TableCell>
                                        <TableCell className="text-right">{formatDecimal(typeSummary.f1Score)}</TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </CardContent>
                </Card>
            )}

            {reportSummary.ragMetrics && (
                <RagMetricsCard summary={reportSummary.ragMetrics} />
            )}
        </div>
    );
}
