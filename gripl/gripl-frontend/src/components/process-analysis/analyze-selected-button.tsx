"use client"

import {useState} from "react"
import {Button} from "@/components/ui/button"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle
} from "@/components/ui/dialog"
import {FileText} from "lucide-react"
import {Spinner} from "@/components/ui/spinner"
import {useToast} from "@/components/ui/toast"
import {useAnalysisEndpoint} from "@/components/providers/analysis-endpoint-provider"
import {useAnalysisSettings} from "@/hooks/use-analysis-settings"
import AnalysisSettingsFields from "@/components/process-analysis/analysis-settings-fields"
import analyzeProcessModels from "@/actions/analyze-process-models"

interface AnalyzeSelectedButtonProps {
    selectedIds: number[]
    onEnqueued: (ids: number[]) => void
}

export default function AnalyzeSelectedButton({selectedIds, onEnqueued}: AnalyzeSelectedButtonProps) {
    const [open, setOpen] = useState(false)
    const [isSubmitting, setIsSubmitting] = useState(false)
    const settings = useAnalysisSettings()
    const {backendEndpoint, isMulticlass} = useAnalysisEndpoint()
    const {showError} = useToast()

    async function handleStartAnalysis() {
        setIsSubmitting(true)
        try {
            const response = await analyzeProcessModels({
                ids: selectedIds,
                endpoint: backendEndpoint,
                ...settings.buildEnqueueParams(isMulticlass),
            })
            onEnqueued(response.enqueuedIds)
            setOpen(false)
        } catch (error) {
            console.error("Error enqueueing analysis:", error)
            showError("Failed to start analysis", error instanceof Error ? error.message : undefined)
        } finally {
            setIsSubmitting(false)
        }
    }

    return <Dialog open={open} onOpenChange={setOpen}>
        <Button
            variant="default"
            onClick={() => setOpen(true)}
            disabled={selectedIds.length === 0}
        >
            <FileText className="mr-2 h-4 w-4"/>
            Analyze Selected ({selectedIds.length})
        </Button>
        <DialogContent className="max-h-[85vh] overflow-y-auto">
            <DialogHeader>
                <DialogTitle>Analyze {selectedIds.length} Process Model{selectedIds.length === 1 ? "" : "s"}</DialogTitle>
                <DialogDescription>
                    Configure the LLM used for this analysis run. The selected models will be analyzed sequentially, one at a time.
                </DialogDescription>
            </DialogHeader>
            <AnalysisSettingsFields settings={settings} isMulticlass={isMulticlass} idPrefix="batch-"/>
            <div className="text-xs text-muted-foreground break-all">
                Selected endpoint: {backendEndpoint}
            </div>
            <DialogFooter>
                <Button variant="outline" onClick={() => setOpen(false)} disabled={isSubmitting}>
                    Cancel
                </Button>
                <Button onClick={handleStartAnalysis} disabled={isSubmitting}>
                    {isSubmitting && <Spinner size="small" className="mr-2 h-4 w-4"/>}
                    Start Analysis
                </Button>
            </DialogFooter>
        </DialogContent>
    </Dialog>
}
