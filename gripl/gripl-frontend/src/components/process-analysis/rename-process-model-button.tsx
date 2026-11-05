"use client"

import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle
} from "@/components/ui/dialog";
import {Button} from "@/components/ui/button";
import {Pencil} from "lucide-react";
import React, {useState} from "react";
import {Label} from "@/components/ui/label";
import {Input} from "@/components/ui/input";
import {Spinner} from "@/components/ui/spinner";
import renameProcessModel from "@/actions/rename-process-model";
import {useToast} from "@/components/ui/toast";
import {toErrorMessage} from "@/lib/http-error";
import {PROCESS_MODEL_RENAMED_EVENT, ProcessModelRenamedDetail} from "@/models/dto/ProcessModel";

interface RenameProcessModelButtonProps {
    id: number;
    name: string;
    onRenamed: (name: string) => void;
}

export default function RenameProcessModelButton({id, name, onRenamed}: RenameProcessModelButtonProps) {

    const [isOpen, setIsOpen] = useState(false)
    const [newName, setNewName] = useState(name)
    const [isSaving, setIsSaving] = useState(false)
    const {showError, showToast} = useToast()

    function handleOpenChange(open: boolean) {
        if (open) setNewName(name)
        setIsOpen(open)
    }

    async function handleSubmit(event: React.FormEvent) {
        event.preventDefault()
        const trimmed = newName.trim()
        if (!trimmed) {
            showToast({title: "Please enter a name for the process model.", variant: "info"})
            return
        }
        setIsSaving(true)
        try {
            const updated = await renameProcessModel(id, trimmed)
            onRenamed(updated.name)
            // Lets the breadcrumbs pick up the new name without refetching.
            window.dispatchEvent(new CustomEvent<ProcessModelRenamedDetail>(PROCESS_MODEL_RENAMED_EVENT, {
                detail: {id, name: updated.name}
            }))
            setIsOpen(false)
        } catch (error) {
            console.error("There was an error renaming the process model:", error)
            showError("Failed to rename the process model", toErrorMessage(error))
        } finally {
            setIsSaving(false)
        }
    }

    return <Dialog open={isOpen} onOpenChange={handleOpenChange}>
        <Button variant="ghost" size="icon" className="h-8 w-8" title="Rename" onClick={() => handleOpenChange(true)}>
            <Pencil className="h-4 w-4"/>
        </Button>
        <DialogContent>
            <form onSubmit={handleSubmit}>
                <DialogHeader>
                    <DialogTitle>Rename Process Model</DialogTitle>
                    <DialogDescription>Update the name of this process model.</DialogDescription>
                </DialogHeader>
                <div className="space-y-2 py-4">
                    <Label htmlFor="process-model-name">Name</Label>
                    <Input id="process-model-name" value={newName} maxLength={255}
                           onChange={event => setNewName(event.target.value)} placeholder="name" autoFocus/>
                </div>
                <DialogFooter>
                    <Button type="button" variant="outline" onClick={() => setIsOpen(false)}>
                        Cancel
                    </Button>
                    <Button type="submit" disabled={isSaving}>
                        {isSaving && <Spinner className="mr-2 h-4 w-4 text-white"/>}
                        Save
                    </Button>
                </DialogFooter>
            </form>
        </DialogContent>
    </Dialog>
}
