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
import {useRouter} from "next/navigation";
import {Label} from "@/components/ui/label";
import {Input} from "@/components/ui/input";
import {EvaluationDataMeta} from "@/models/dto/EvaluationData";
import updateTestcaseName from "@/actions/update-testcase-name";
import {useToast} from "@/components/ui/toast";
import {toErrorMessage} from "@/lib/http-error";

interface EditTestcaseButtonProps {
    testcase: EvaluationDataMeta;
}

export default function EditTestcaseButton({testcase}: EditTestcaseButtonProps) {

    const router = useRouter()
    const [showEditTestcaseDialog, setShowEditTestcaseDialog] = useState(false)
    const [name, setName] = useState(testcase.name ?? "")
    const {showError, showToast} = useToast()

    function handleTestcaseUpdate() {
        if (!name) {
            showToast({title: "Please enter a name for the test case.", variant: "info"})
            return
        }
        updateTestcaseName(Number(testcase.id), name).then(() => {
            router.refresh()
            setShowEditTestcaseDialog(false)
        }).catch(error => {
            console.error("There was an error updating the test case:", error)
            showError("Failed to update the test case", toErrorMessage(error))
        })
    }

    return <Dialog open={showEditTestcaseDialog} onOpenChange={setShowEditTestcaseDialog}>
        <Button variant="outline" className="z-10" onClick={() => setShowEditTestcaseDialog(true)}>
            <Pencil className="h-2 w-2" />
        </Button>
        <DialogContent>
            <DialogHeader>
                <DialogTitle>Edit Test Case</DialogTitle>
                <DialogDescription>Update the name of this test case.</DialogDescription>
            </DialogHeader>
            <div>
                <div className="space-y-4 py-2 pb-4">
                    <div className="space-y-2">
                        <Label htmlFor="testcase-name">Name</Label>
                        <Input id="testcase-name" value={name} onChange={event => setName(event.target.value)}
                               placeholder="name"/>
                    </div>
                </div>
            </div>
            <DialogFooter>
                <Button variant="outline" onClick={() => setShowEditTestcaseDialog(false)}>
                    Cancel
                </Button>
                <Button type="submit" onClick={handleTestcaseUpdate}>Save</Button>
            </DialogFooter>
        </DialogContent>
    </Dialog>
}
