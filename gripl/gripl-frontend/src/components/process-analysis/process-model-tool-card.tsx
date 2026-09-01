"use client"

import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card";
import {Button} from "@/components/ui/button";
import {Download, FileText} from "lucide-react";
import {useState} from "react";
import {Spinner} from "@/components/ui/spinner";
import {Separator} from "@/components/ui/separator";
import {useAnalysisEndpoint} from "@/components/providers/analysis-endpoint-provider";
import {useToast} from "@/components/ui/toast";
import {useAnalysisSettings} from "@/hooks/use-analysis-settings";
import AnalysisSettingsFields from "@/components/process-analysis/analysis-settings-fields";
import analyzeProcessModels from "@/actions/analyze-process-models";
import {ProcessModelDetail} from "@/models/dto/ProcessModel";

interface ProcessModelToolCardProps {
    model: ProcessModelDetail;
    onAnalysisStarted: () => void;
}

export default function ProcessModelToolCard({model, onAnalysisStarted}: ProcessModelToolCardProps) {

    const settings = useAnalysisSettings()
    const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
    const {backendEndpoint, isMulticlass} = useAnalysisEndpoint()
    const {showError} = useToast()

    const isAnalyzing = model.status === "QUEUED" || model.status === "RUNNING"
    const analysisResult = model.analysisResult

    async function handleAnalyzeClick() {
        setIsSubmitting(true)
        try {
            await analyzeProcessModels({
                ids: [model.id],
                endpoint: backendEndpoint,
                ...settings.buildEnqueueParams(isMulticlass),
            })
            onAnalysisStarted()
        } catch (error) {
            console.error("Error starting analysis:", error)
            showError("Failed to start analysis", error instanceof Error ? error.message : undefined)
        } finally {
            setIsSubmitting(false)
        }
    }

    function handleDownloadResultClick() {
        if (!analysisResult) return;
        const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(analysisResult, null, 2));
        const downloadAnchorNode = document.createElement('a');
        downloadAnchorNode.setAttribute("href", dataStr);
        downloadAnchorNode.setAttribute("download", `gripl-analysis-result-${model.name}.json`);
        document.body.appendChild(downloadAnchorNode);
        downloadAnchorNode.click();
        downloadAnchorNode.remove();
    }

    return <Card className="max-w-80">
        <CardHeader>
            <CardTitle className="text-lg font-semibold">GRIPL Analysis Tool</CardTitle>
            <CardDescription>Analyze this BPMN diagram for GDPR compliance using the specified LLM. Critical elements will be highlighted in the diagram.</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col space-y-2">
            <AnalysisSettingsFields settings={settings} isMulticlass={isMulticlass}/>
            <div className="py-2">
                <Separator/>
            </div>
            <div className="text-xs text-muted-foreground break-all">
                Selected endpoint: {backendEndpoint}
            </div>
            <Button
                onClick={handleAnalyzeClick}
                variant="default"
                disabled={isAnalyzing || isSubmitting}
            >
                <>{isAnalyzing || isSubmitting ? <>
                    <Spinner className="text-white mr-2 h-4 w-4"/>
                    <span>Analyzing...</span>
                </> : <>
                    <FileText className="mr-2 h-4 w-4"/>
                    Analyze for GDPR
                </>}</>
            </Button>
            <Button
                onClick={handleDownloadResultClick}
                variant="outline"
                disabled={!analysisResult || isAnalyzing}
            >
                <Download className="mr-2 h-4 w-4"/>
                Download Report (Json)
            </Button>
        </CardContent>
    </Card>
}
