"use client"

import React, {useEffect, useState} from "react";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle
} from "@/components/ui/dialog";
import {Button} from "@/components/ui/button";
import {Label} from "@/components/ui/label";
import {Input} from "@/components/ui/input";
import {stripBpmnExtension} from "@/lib/download-bpmn";

interface ExportFileNameDialogProps {
    isOpen: boolean
    defaultName?: string
    onClose: () => void
    onConfirm: (fileName: string) => void
}

export default function ExportFileNameDialog({ isOpen, defaultName, onClose, onConfirm }: ExportFileNameDialogProps) {

    const [fileName, setFileName] = useState("")

    // Prefill with the default name every time the dialog opens
    useEffect(() => {
        if (isOpen) {
            setFileName(stripBpmnExtension(defaultName || "diagram"))
        }
    }, [isOpen, defaultName])

    function handleConfirm() {
        onConfirm(fileName.trim() || stripBpmnExtension(defaultName || "diagram"))
    }

    return <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
        <DialogContent>
            <DialogHeader>
                <DialogTitle>Export BPMN</DialogTitle>
                <DialogDescription>Choose a file name for the exported diagram.</DialogDescription>
            </DialogHeader>
            <div className="space-y-2 py-2 pb-4">
                <Label htmlFor="export-file-name">File name</Label>
                <div className="flex items-center space-x-2">
                    <Input
                        id="export-file-name"
                        value={fileName}
                        onChange={(e) => setFileName(e.target.value)}
                        onKeyDown={(e) => e.key === "Enter" && handleConfirm()}
                        autoFocus
                    />
                    <span className="text-sm text-muted-foreground">.bpmn</span>
                </div>
            </div>
            <DialogFooter>
                <Button variant="outline" onClick={onClose}>
                    Cancel
                </Button>
                <Button type="submit" onClick={handleConfirm}>Export</Button>
            </DialogFooter>
        </DialogContent>
    </Dialog>
}
