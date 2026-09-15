"use client"

import {Card, CardContent} from "@/components/ui/card";
import {Button} from "@/components/ui/button";
import {Download, FileText} from "lucide-react";
import {useState} from "react";
import {Spinner} from "@/components/ui/spinner";
import {useAnalysisEndpoint} from "@/components/providers/analysis-endpoint-provider";
import {useToast} from "@/components/ui/toast";
import {useAnalysisSettings} from "@/hooks/use-analysis-settings";
import analyzeProcessModels from "@/actions/analyze-process-models";
import {ProcessModelDetail} from "@/models/dto/ProcessModel";

interface ProcessModelActionsProps {
    model: ProcessModelDetail;
    onAnalysisStarted: () => void;
}

/**
 * Slim analyze/download actions for the detail view's top-right corner.
 * The LLM configuration itself lives in Analysis Settings on the dashboard —
 * it applies globally, so it isn't re-shown or re-editable per model here.
 */
export default function ProcessModelActions({model, onAnalysisStarted}: ProcessModelActionsProps) {

    const settings = useAnalysisSettings()
    const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
    const {backendEndpoint} = useAnalysisEndpoint()
    const {showError} = useToast()

    const isAnalyzing = model.status === "QUEUED" || model.status === "RUNNING"
    const analysisResult = model.analysisResult

    async function handleAnalyzeClick() {
        setIsSubmitting(true)
        try {
            await analyzeProcessModels({
                ids: [model.id],
                endpoint: backendEndpoint,
                ...settings.buildEnqueueParams(),
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

    return <Card className="w-fit">
        <CardContent className="flex flex-col gap-2 p-3">
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
