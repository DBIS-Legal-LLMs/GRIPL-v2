"use client"

import {useState} from "react"
import {Button} from "@/components/ui/button"
import {ChevronDown, DatabaseZap, Download, Tags} from "lucide-react"
import {Spinner} from "@/components/ui/spinner"
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {useToast} from "@/components/ui/toast"
import {extractErrorDetails, toErrorMessage} from "@/lib/http-error"
import {authenticatedFetch} from "@/lib/authenticated-fetch"

interface ExportForLabelingButtonProps {
    /** Exports only these models; empty exports all of the user's models */
    selectedIds: number[]
}

interface ImportedDataset {
    id: number
    name: string
    testCaseCount: number
}

/**
 * Turns the process models into one labeling dataset with the LLM analysis results as
 * pre-filled labels — either downloaded as ZIP in the labeling import format, or created
 * directly in the labeling tool.
 */
export default function ExportForLabelingButton({selectedIds}: ExportForLabelingButtonProps) {
    const [isLoading, setIsLoading] = useState(false)
    const {showError, showToast} = useToast()

    async function handleDownload() {
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

    async function handleCreateDataset() {
        setIsLoading(true)
        try {
            const response = await authenticatedFetch("/api/process-models/labeling-dataset", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Accept: "application/json",
                },
                body: JSON.stringify({ids: selectedIds.length > 0 ? selectedIds : null}),
            })
            if (!response.ok) {
                throw new Error(await extractErrorDetails(response))
            }
            const dataset = await response.json() as ImportedDataset
            showToast({
                title: "Labeling dataset created",
                description: `'${dataset.name}' with ${dataset.testCaseCount} test case(s) is now available in Labeling.`,
                variant: "success",
            })
        } catch (error) {
            console.error("There was an error creating the labeling dataset:", error)
            showError("Failed to create the labeling dataset", toErrorMessage(error))
        } finally {
            setIsLoading(false)
        }
    }

    return <DropdownMenu>
        <DropdownMenuTrigger asChild>
            <Button
                variant="outline"
                disabled={isLoading}
                title="Use the LLM results as pre-filled labels in the labeling tool"
            >
                {isLoading ? <Spinner size="small" className="mr-2 h-4 w-4"/> : <Tags className="mr-2 h-4 w-4"/>}
                {selectedIds.length > 0 ? `Export for Labeling (${selectedIds.length})` : "Export All for Labeling"}
                <ChevronDown className="ml-2 h-4 w-4"/>
            </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-64">
            <DropdownMenuItem onClick={handleCreateDataset}>
                <DatabaseZap className="mr-2 h-4 w-4"/>
                Create labeling dataset
            </DropdownMenuItem>
            <DropdownMenuItem onClick={handleDownload}>
                <Download className="mr-2 h-4 w-4"/>
                Download as ZIP
            </DropdownMenuItem>
        </DropdownMenuContent>
    </DropdownMenu>
}
