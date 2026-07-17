"use client"

import {Button} from "@/components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle
} from "@/components/ui/dialog";
import React from "react";
import {Label} from "@/components/ui/label";
import {Input} from "@/components/ui/input";
import {useRouter} from "next/navigation";
import {Plus} from "lucide-react";
import emptyDiagram from "@/data/empty-diagram.bpmn";
import createDataset from "@/actions/create-dataset";
import {useToast} from "@/components/ui/toast";
import {toErrorMessage} from "@/lib/http-error";

export default function CreateDatasetButton() {

    const router = useRouter()
    const [showCreateDatasetDialog, setShowCreateDatasetDialog] = React.useState(false)
    const {showToast, showError} = useToast()

    function handleTestCaseCreation() {
        const datasetName = (document.getElementById("dataset-name") as HTMLInputElement).value
        if (!datasetName) {
            showToast({title: "Please enter a name for the dataset.", variant: "info"})
            return
        }
        const datasetDescription = (document.getElementById("dataset-description") as HTMLInputElement).value
        createDataset(datasetName, datasetDescription).then().then(id => {
            router.refresh()
            setShowCreateDatasetDialog(false)
        }).catch(error => {
            console.error("There was an error creating the dataset:", error)
            showError("Failed to create the dataset", toErrorMessage(error))
        })
    }

    return <Dialog open={showCreateDatasetDialog} onOpenChange={setShowCreateDatasetDialog}>
        <Button className="h-full" onClick={() => setShowCreateDatasetDialog(true)}>
            <Plus />
            <span className="pl-2 text-center">Create Dataset</span>
        </Button>
        <DialogContent>
            <DialogHeader>
                <DialogTitle>Create Dataset</DialogTitle>
                <DialogDescription>Create a new dataset to organize your test cases for different evaluation runs.</DialogDescription>
            </DialogHeader>
            <div>
                <div className="space-y-4 py-2 pb-4">
                    <div className="space-y-2">
                        <Label htmlFor="dataset-name">Name</Label>
                        <Input id="dataset-name" placeholder={"name"}/>
                    </div>
                </div>
                <div className="space-y-4 py-2 pb-4">
                    <div className="space-y-2">
                        <Label htmlFor="dataset-description">Description</Label>
                        <Input id="dataset-description" placeholder={"description"}/>
                    </div>

                </div>
            </div>
            <DialogFooter>
                <Button variant="outline" onClick={() => setShowCreateDatasetDialog(false)}>
                    Cancel
                </Button>
                <Button type="submit" onClick={handleTestCaseCreation}>Create</Button>
            </DialogFooter>
        </DialogContent>
    </Dialog>
}