"use client"

import {useState} from "react"
import {useRouter} from "next/navigation"
import {useProcessModelListPolling} from "@/hooks/use-process-model-list-polling"
import ProcessModelListItemRow from "@/components/process-analysis/process-model-list-item"
import UploadProcessModelsButton from "@/components/process-analysis/upload-process-models-button"
import AnalyzeSelectedButton from "@/components/process-analysis/analyze-selected-button"
import DownloadListReportButton from "@/components/process-analysis/download-list-report-button"
import ClearAllButton from "@/components/process-analysis/clear-all-button"
import deleteProcessModel from "@/actions/delete-process-model"
import {useToast} from "@/components/ui/toast"
import {ProcessModelDetail, ProcessModelListItem, ProcessModelStatus} from "@/models/dto/ProcessModel"

interface ProcessModelListProps {
    initialModels: ProcessModelListItem[]
}

export default function ProcessModelList({initialModels}: ProcessModelListProps) {
    const {models, setModels} = useProcessModelListPolling(initialModels)
    const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())
    const router = useRouter()
    const {showError} = useToast()

    function handleSelectChange(id: number, selected: boolean) {
        setSelectedIds(prev => {
            const next = new Set(prev)
            if (selected) next.add(id); else next.delete(id)
            return next
        })
    }

    function handleUploaded(uploaded: ProcessModelListItem[]) {
        setModels(prev => [...uploaded, ...prev])
    }

    function handleEnqueued(ids: number[]) {
        const enqueued = new Set(ids)
        setModels(prev => prev.map(m => enqueued.has(m.id) ? {...m, status: "QUEUED" as ProcessModelStatus} : m))
        setSelectedIds(new Set())
    }

    async function handleDelete(id: number) {
        try {
            await deleteProcessModel(id)
            setModels(prev => prev.filter(m => m.id !== id))
            setSelectedIds(prev => {
                const next = new Set(prev)
                next.delete(id)
                return next
            })
            router.refresh()
        } catch (error) {
            console.error("Error deleting process model:", error)
            showError("Failed to delete process model", error instanceof Error ? error.message : undefined)
        }
    }

    function handleCleared(deletedIds: number[]) {
        const deleted = new Set(deletedIds)
        setModels(prev => prev.filter(m => !deleted.has(m.id)))
        setSelectedIds(prev => {
            const next = new Set(prev)
            deleted.forEach(id => next.delete(id))
            return next
        })
        router.refresh()
    }

    async function handleDownload(id: number) {
        try {
            const response = await fetch(`/api/process-models/${id}`)
            if (!response.ok) throw new Error(`Failed to fetch result: ${response.statusText}`)
            const detail = await response.json() as ProcessModelDetail

            const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(detail.analysisResult, null, 2))
            const downloadAnchorNode = document.createElement('a')
            downloadAnchorNode.setAttribute("href", dataStr)
            downloadAnchorNode.setAttribute("download", `gripl-analysis-result-${detail.name}.json`)
            document.body.appendChild(downloadAnchorNode)
            downloadAnchorNode.click()
            downloadAnchorNode.remove()
        } catch (error) {
            console.error("Error downloading process model report:", error)
            showError("Failed to download report", error instanceof Error ? error.message : undefined)
        }
    }

    const selectableIds = models.filter(m => m.status !== "QUEUED" && m.status !== "RUNNING").map(m => m.id)

    return <div className="space-y-4">
        <div className="flex flex-row flex-wrap items-center justify-between gap-2">
            <UploadProcessModelsButton onUploaded={handleUploaded}/>
            <div className="flex flex-row gap-2">
                <AnalyzeSelectedButton selectedIds={Array.from(selectedIds)} onEnqueued={handleEnqueued}/>
                <DownloadListReportButton models={models}/>
                <ClearAllButton models={models} onCleared={handleCleared}/>
            </div>
        </div>

        {models.length > 0 && (
            <div className="flex items-center gap-2 pb-1">
                <input
                    type="checkbox"
                    className="h-4 w-4"
                    checked={selectableIds.length > 0 && selectableIds.every(id => selectedIds.has(id))}
                    onChange={(e) => setSelectedIds(e.target.checked ? new Set(selectableIds) : new Set())}
                />
                <span className="text-sm text-muted-foreground">Select all</span>
            </div>
        )}

        {models.length === 0 ? (
            <p className="text-sm text-muted-foreground">No process models uploaded yet.</p>
        ) : (
            <div>
                {models.map(model => (
                    <ProcessModelListItemRow
                        key={model.id}
                        model={model}
                        selected={selectedIds.has(model.id)}
                        onSelectChange={handleSelectChange}
                        onDelete={handleDelete}
                        onDownload={handleDownload}
                    />
                ))}
            </div>
        )}
    </div>
}
