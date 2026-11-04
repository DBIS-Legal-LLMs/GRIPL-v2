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

/** Mirrors the backend's LabelExportSummary */
interface LabelExportSummary {
    exportedModelCount: number
    labelCount: number
    notAnalyzedModels: string[]
    unreadableModels: string[]
    droppedLabelCount: number
}

interface CreatedLabelingDataset {
    dataset: ImportedDataset
    summary: LabelExportSummary
}

const EXPORT_SUMMARY_HEADER = "X-Gripl-Export-Summary"

function readSummaryHeader(response: Response): LabelExportSummary | null {
    const raw = response.headers.get(EXPORT_SUMMARY_HEADER)
    if (!raw) return null
    try {
        return JSON.parse(decodeURIComponent(raw)) as LabelExportSummary
    } catch {
        return null
    }
}

function listNames(names: string[]): string {
    const shown = names.slice(0, 3).map(name => `'${name}'`).join(", ")
    return names.length > 3 ? `${shown} and ${names.length - 3} more` : shown
}

/** What the export did with the labels, so models that came out without them don't go unnoticed. */
function describeSummary(summary: LabelExportSummary): {notes: string[], variant: "success" | "info" | "error"} {
    const notes: string[] = []
    if (summary.unreadableModels.length > 0) {
        notes.push(`Analysis result could not be read, exported without labels: ${listNames(summary.unreadableModels)}.`)
    }
    if (summary.notAnalyzedModels.length > 0) {
        notes.push(`Not analyzed yet, exported without labels: ${listNames(summary.notAnalyzedModels)}.`)
    }
    if (summary.droppedLabelCount > 0) {
        notes.push(`${summary.droppedLabelCount} label(s) left out for elements removed after the analysis.`)
    }
    const variant = summary.unreadableModels.length > 0 ? "error" : notes.length > 0 ? "info" : "success"
    return {notes, variant}
}

/**
 * Turns the process models into one labeling dataset with the LLM analysis results as
 * pre-filled labels — either downloaded as ZIP in the labeling import format, or created
 * directly in the labeling tool.
 */
export default function ExportForLabelingButton({selectedIds}: ExportForLabelingButtonProps) {
    const [isLoading, setIsLoading] = useState(false)
    const {showError, showToast} = useToast()

    function showSummary(title: string, lead: string, summary: LabelExportSummary) {
        const {notes, variant} = describeSummary(summary)
        showToast({
            title,
            description: [
                lead,
                `${summary.exportedModelCount} model(s) with ${summary.labelCount} LLM label(s).`,
                ...notes,
            ].join("\n"),
            variant,
            duration: notes.length > 0 ? 12000 : undefined,
        })
    }

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
            const summary = readSummaryHeader(response)
            if (summary) {
                showSummary("Export downloaded", `Import '${fileName}' in Labeling to use it.`, summary)
            }
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
            const {dataset, summary} = await response.json() as CreatedLabelingDataset
            showSummary("Labeling dataset created", `'${dataset.name}' is now available in Labeling.`, summary)
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
