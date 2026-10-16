"use client"

import {useState} from "react"
import {Button} from "@/components/ui/button"
import {FileText} from "lucide-react"
import {Spinner} from "@/components/ui/spinner"
import {useToast} from "@/components/ui/toast"
import {useAnalysisEndpoint} from "@/components/providers/analysis-endpoint-provider"
import {AnalysisSettings} from "@/hooks/use-analysis-settings"
import analyzeProcessModels from "@/actions/analyze-process-models"

interface AnalyzeSelectedButtonProps {
    selectedIds: number[]
    settings: AnalysisSettings
    onEnqueued: (ids: number[]) => void
}

export default function AnalyzeSelectedButton({selectedIds, settings, onEnqueued}: AnalyzeSelectedButtonProps) {
    const [isSubmitting, setIsSubmitting] = useState(false)
    const {backendEndpoint} = useAnalysisEndpoint()
    const {showError} = useToast()

    async function handleClick() {
        setIsSubmitting(true)
        try {
            const response = await analyzeProcessModels({
                ids: selectedIds,
                endpoint: backendEndpoint,
                ...settings.buildEnqueueParams(),
            })
            onEnqueued(response.enqueuedIds)
        } catch (error) {
            console.error("Error enqueueing analysis:", error)
            showError("Failed to start analysis", error instanceof Error ? error.message : undefined)
        } finally {
            setIsSubmitting(false)
        }
    }

    return <Button
        variant="default"
        onClick={handleClick}
        disabled={selectedIds.length === 0 || isSubmitting}
    >
        {isSubmitting ? <Spinner size="small" className="mr-2 h-4 w-4"/> : <FileText className="mr-2 h-4 w-4"/>}
        Analyze Selected ({selectedIds.length})
    </Button>
}
