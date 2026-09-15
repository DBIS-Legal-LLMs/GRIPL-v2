"use client"

import {useState} from "react"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle
} from "@/components/ui/dialog"
import {Button} from "@/components/ui/button"
import {Spinner} from "@/components/ui/spinner"
import {Trash2} from "lucide-react"
import {useToast} from "@/components/ui/toast"
import deleteProcessModel from "@/actions/delete-process-model"
import {ProcessModelListItem} from "@/models/dto/ProcessModel"

interface ClearAllButtonProps {
    models: ProcessModelListItem[]
    onCleared: (deletedIds: number[]) => void
}

export default function ClearAllButton({models, onCleared}: ClearAllButtonProps) {
    const [open, setOpen] = useState(false)
    const [isDeleting, setIsDeleting] = useState(false)
    const {showError} = useToast()

    const runningCount = models.filter(m => m.status === "RUNNING").length

    async function handleConfirm() {
        setIsDeleting(true)
        const results = await Promise.allSettled(models.map(m => deleteProcessModel(m.id).then(() => m.id)))
        const deletedIds = results
            .filter((r): r is PromiseFulfilledResult<number> => r.status === "fulfilled")
            .map(r => r.value)
        const failedCount = results.length - deletedIds.length

        setIsDeleting(false)
        setOpen(false)
        onCleared(deletedIds)

        if (failedCount > 0) {
            showError(
                `Could not delete ${failedCount} model${failedCount === 1 ? "" : "s"}`,
                "Models currently being analyzed can't be deleted."
            )
        }
    }

    return <Dialog open={open} onOpenChange={setOpen}>
        <Button
            variant="outline"
            onClick={() => setOpen(true)}
            disabled={models.length === 0}
        >
            <Trash2 className="mr-2 h-4 w-4"/>
            Clear All
        </Button>
        <DialogContent>
            <DialogHeader>
                <DialogTitle>Delete all {models.length} process models?</DialogTitle>
                <DialogDescription>
                    This permanently deletes every uploaded process model and its analysis results. This cannot be undone.
                    {runningCount > 0 && ` ${runningCount} model${runningCount === 1 ? " is" : "s are"} currently being analyzed and will be skipped.`}
                </DialogDescription>
            </DialogHeader>
            <DialogFooter>
                <Button variant="outline" onClick={() => setOpen(false)} disabled={isDeleting}>
                    Cancel
                </Button>
                <Button variant="destructive" onClick={handleConfirm} disabled={isDeleting}>
                    {isDeleting && <Spinner size="small" className="mr-2 h-4 w-4"/>}
                    Delete All
                </Button>
            </DialogFooter>
        </DialogContent>
    </Dialog>
}
