"use client"

import Link from "next/link"
import {Card, CardHeader, CardTitle} from "@/components/ui/card"
import {Button} from "@/components/ui/button"
import {Spinner} from "@/components/ui/spinner"
import {CheckCircle2, Download, Trash2, XCircle} from "lucide-react"
import {ProcessModelListItem as ProcessModelListItemDto} from "@/models/dto/ProcessModel"

interface ProcessModelListItemProps {
    model: ProcessModelListItemDto
    selected: boolean
    onSelectChange: (id: number, selected: boolean) => void
    onDelete: (id: number) => void
    onDownload: (id: number) => void
}

export default function ProcessModelListItem({model, selected, onSelectChange, onDelete, onDownload}: ProcessModelListItemProps) {

    function renderStatus() {
        switch (model.status) {
            case "QUEUED":
                return <span className="text-xs text-muted-foreground">Queued</span>
            case "RUNNING":
                return <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <Spinner size="small" className="h-3.5 w-3.5"/> Analyzing...
                </span>
            case "DONE":
                return <span className="flex items-center gap-1.5 text-xs text-emerald-600">
                    <CheckCircle2 className="h-4 w-4"/>
                    {model.criticalElementCount ?? 0}/{model.totalElements ?? 0} Elements critical
                </span>
            case "ERROR":
                return <span className="flex items-center gap-1.5 text-xs text-destructive" title={model.errorMessage ?? undefined}>
                    <XCircle className="h-4 w-4"/> Analysis failed
                </span>
            default:
                return <span className="text-xs text-muted-foreground">Not analyzed</span>
        }
    }

    return <div className="w-full flex flex-row items-center gap-2 mb-3">
        <input
            type="checkbox"
            className="h-4 w-4 shrink-0"
            checked={selected}
            onChange={(e) => onSelectChange(model.id, e.target.checked)}
            disabled={model.status === "QUEUED" || model.status === "RUNNING"}
        />
        <Link href={`/process-analysis/${model.id}`} className="flex-1 min-w-0">
            <Card className="hover:bg-muted/50 transition-colors">
                <CardHeader className="flex-row justify-between items-center gap-4">
                    <CardTitle className="text-base font-medium truncate">{model.name}</CardTitle>
                    {renderStatus()}
                </CardHeader>
            </Card>
        </Link>
        <Button
            variant="outline"
            size="icon"
            title="Download report"
            disabled={model.status !== "DONE"}
            onClick={() => onDownload(model.id)}
        >
            <Download className="h-4 w-4"/>
        </Button>
        <Button
            variant="outline"
            size="icon"
            title="Delete"
            disabled={model.status === "RUNNING"}
            onClick={() => onDelete(model.id)}
        >
            <Trash2 className="h-4 w-4"/>
        </Button>
    </div>
}
