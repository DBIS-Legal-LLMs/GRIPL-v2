import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { RagSummaryMetrics, TestCaseRagMetrics } from "@/models/dto/ReportData";

interface RagMetricsCardProps {
    title?: string;
    summary?: RagSummaryMetrics | null;
    testCase?: TestCaseRagMetrics | null;
}

/**
 * Displays Ragas retrieval/generation quality metrics:
 *  - Faithfulness (grounding of explanations in retrieved context)
 *  - Context Utilization (how well the LLM used the retrieved context)
 *
 * Accepts either a per-test-case object or an aggregate summary object.
 * Renders nothing if no RAG metrics are available.
 */
export default function RagMetricsCard({ title = "RAG Metrics (Ragas)", summary, testCase }: RagMetricsCardProps) {
    const faithfulness = summary?.faithfulnessMean ?? testCase?.faithfulness ?? null;
    const contextUtilization = summary?.contextUtilizationMean ?? testCase?.contextUtilization ?? null;

    const hasAny = faithfulness !== null || contextUtilization !== null;
    if (!hasAny) return null;

    const fmt = (v: number | null) => (v === null ? "n/a" : v.toFixed(3));
    const pct = (v: number | null) => (v === null ? 0 : Math.max(0, Math.min(1, v)) * 100);

    const samples = summary
        ? `${summary.totalSamples} sample(s) across ${summary.evaluatedTestCases} test case(s)` +
          (summary.failedSamples > 0 ? ` — ${summary.failedSamples} failed` : "")
        : testCase
            ? `${testCase.sampleCount} sample(s)` + (testCase.failedCount > 0 ? ` — ${testCase.failedCount} failed` : "")
            : "";

    const cols = [faithfulness, contextUtilization].filter(v => v !== null).length;

    return (
        <Card>
            <CardHeader>
                <CardTitle>{title}</CardTitle>
            </CardHeader>
            <CardContent>
                <div className={`grid grid-cols-1 md:grid-cols-${cols} gap-6`}>
                    {faithfulness !== null && (
                        <div className="text-center">
                            <div className="text-3xl font-bold text-chart-metric-4">{fmt(faithfulness)}</div>
                            <div className="text-sm text-muted-foreground">Faithfulness</div>
                            <Progress value={pct(faithfulness)} className="h-2 mt-2" />
                        </div>
                    )}
                    {contextUtilization !== null && (
                        <div className="text-center">
                            <div className="text-3xl font-bold text-chart-metric-3">{fmt(contextUtilization)}</div>
                            <div className="text-sm text-muted-foreground">Context Utilization</div>
                            <Progress value={pct(contextUtilization)} className="h-2 mt-2" />
                        </div>
                    )}
                </div>
                {samples && (
                    <div className="mt-4 text-xs text-muted-foreground text-center">{samples}</div>
                )}
            </CardContent>
        </Card>
    );
}
