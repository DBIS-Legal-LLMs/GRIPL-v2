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
import {Dataset} from "@/models/dto/Dataset";
import updateDataset from "@/actions/update-dataset";
import {useToast} from "@/components/ui/toast";
import {toErrorMessage} from "@/lib/http-error";

interface EditDatasetButtonProps {
    dataset: Dataset;
}

export default function EditDatasetButton({dataset}: EditDatasetButtonProps) {

    const router = useRouter()
    const [showEditDatasetDialog, setShowEditDatasetDialog] = useState(false)
    const [name, setName] = useState(dataset.name)
    const [description, setDescription] = useState(dataset.description ?? "")
    const {showError, showToast} = useToast()

    function handleDatasetUpdate() {
        if (!name) {
            showToast({title: "Please enter a name for the dataset.", variant: "info"})
            return
        }
        updateDataset(dataset.id, name, description).then(() => {
            router.refresh()
            setShowEditDatasetDialog(false)
        }).catch(error => {
            console.error("There was an error updating the dataset:", error)
            showError("Failed to update the dataset", toErrorMessage(error))
        })
    }

    return <Dialog open={showEditDatasetDialog} onOpenChange={setShowEditDatasetDialog}>
        <Button variant="outline" className="h-full w-20 aspect-square p-0" onClick={() => setShowEditDatasetDialog(true)}>
            <Pencil/>
        </Button>
        <DialogContent>
            <DialogHeader>
                <DialogTitle>Edit Dataset {dataset.name}</DialogTitle>
                <DialogDescription>Update the metadata of this dataset.</DialogDescription>
            </DialogHeader>
            <div>
                <div className="space-y-4 py-2 pb-4">
                    <div className="space-y-2">
                        <Label htmlFor="dataset-name">Name</Label>
                        <Input id="dataset-name" value={name} onChange={event => setName(event.target.value)}
                               placeholder="name"/>
                    </div>
                </div>
                <div className="space-y-4 py-2 pb-4">
                    <div className="space-y-2">
                        <Label htmlFor="dataset-description">Description</Label>
                        <Input id="dataset-description" value={description}
                               onChange={event => setDescription(event.target.value)} placeholder="description"/>
                    </div>
                </div>
            </div>
            <DialogFooter>
                <Button variant="outline" onClick={() => setShowEditDatasetDialog(false)}>
                    Cancel
                </Button>
                <Button type="submit" onClick={handleDatasetUpdate}>Save</Button>
            </DialogFooter>
        </DialogContent>
    </Dialog>
}
