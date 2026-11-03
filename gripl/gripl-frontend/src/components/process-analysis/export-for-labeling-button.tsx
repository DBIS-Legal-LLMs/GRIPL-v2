"use client"

import {useState} from "react"
import {Button} from "@/components/ui/button"
import {Tags} from "lucide-react"
import {Spinner} from "@/components/ui/spinner"
import {useToast} from "@/components/ui/toast"
import {extractErrorDetails, toErrorMessage} from "@/lib/http-error"
import {authenticatedFetch} from "@/lib/authenticated-fetch"

interface ExportForLabelingButtonProps {
    /** Exports only these models; empty exports all of the user's models */
    selectedIds: number[]
}

/**
 * Downloads the process models as one dataset ZIP in the labeling import format,
 * with the LLM analysis results as pre-filled labels.
 */
export default function ExportForLabelingButton({selectedIds}: ExportForLabelingButtonProps) {
    const [isLoading, setIsLoading] = useState(false)
    const {showError} = useToast()

    async function handleExport() {
        setIsLoading(true)
        try {
            const query = selectedIds.length > 0 ? `?ids=${selectedIds.join(",")}` : ""
            const response = await authenticatedFetch(`/api/process-models/export-labels${query}`)
            if (!response.ok) {
                throw new Error(await extractErrorDetails(response))
            }
            const fileName = response.headers.get("Content-Disposition")?.match(/filename="?([^";]+)"?/)?.[1]
                ?? "gripl-llm-prelabels.zip"
            const objectUrl = URL.createObjectURL(await response.blob())
            const a = document.createElement("a")
            a.href = objectUrl
            a.download = fileName
            document.body.appendChild(a)
            a.click()
            document.body.removeChild(a)
            URL.revokeObjectURL(objectUrl)
        } catch (error) {
            console.error("There was an error exporting the process models for labeling:", error)
            showError("Failed to export for labeling", toErrorMessage(error))
        } finally {
            setIsLoading(false)
        }
    }

    return <Button
        variant="outline"
        onClick={handleExport}
        disabled={isLoading}
        title="Export as a dataset for the labeling tool, with the LLM results as pre-filled labels"
    >
        {isLoading ? <Spinner size="small" className="mr-2 h-4 w-4"/> : <Tags className="mr-2 h-4 w-4"/>}
        {selectedIds.length > 0 ? `Export for Labeling (${selectedIds.length})` : "Export All for Labeling"}
    </Button>
}
