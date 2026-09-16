"use client"

import {useState} from "react"
import {Button} from "@/components/ui/button"
import {Download} from "lucide-react"
import {Spinner} from "@/components/ui/spinner"
import {useToast} from "@/components/ui/toast"
import {ProcessModelDetail, ProcessModelListItem} from "@/models/dto/ProcessModel"

interface DownloadListReportButtonProps {
    models: ProcessModelListItem[]
}

export default function DownloadListReportButton({models}: DownloadListReportButtonProps) {
    const [isDownloading, setIsDownloading] = useState(false)
    const {showError} = useToast()

    async function handleDownloadClick() {
        setIsDownloading(true)
        try {
            const details = await Promise.all(models.map(async (model) => {
                const response = await fetch(`/api/process-models/${model.id}`)
                if (!response.ok) throw new Error(`Failed to fetch result for '${model.name}'`)
                return await response.json() as ProcessModelDetail
            }))

            const bundle = details.map(d => d.status === "DONE" ? {
                id: d.id,
                name: d.name,
                totalElements: d.totalElements,
                criticalElementCount: d.criticalElementCount,
                analysisResult: d.analysisResult,
            } : {
                id: d.id,
                name: d.name,
                totalElements: null,
                criticalElementCount: null,
                analysisResult: null,
                note: "No analysis has been performed for this process model yet.",
            })

            const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(bundle, null, 2))
            const downloadAnchorNode = document.createElement('a')
            downloadAnchorNode.setAttribute("href", dataStr)
            downloadAnchorNode.setAttribute("download", "gripl-process-analysis-report.json")
            document.body.appendChild(downloadAnchorNode)
            downloadAnchorNode.click()
            downloadAnchorNode.remove()
        } catch (error) {
            console.error("Error downloading list report:", error)
            showError("Failed to download report", error instanceof Error ? error.message : undefined)
        } finally {
            setIsDownloading(false)
        }
    }

    return <Button
        variant="outline"
        onClick={handleDownloadClick}
        disabled={models.length === 0 || isDownloading}
    >
        {isDownloading ? <Spinner size="small" className="mr-2 h-4 w-4"/> : <Download className="mr-2 h-4 w-4"/>}
        Download Report ({models.length})
    </Button>
}
