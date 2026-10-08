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
import {Trash2} from "lucide-react";
import React from "react";
import deleteDataset from "@/actions/delete-dataset";
import {useRouter} from "next/navigation";
import {Dataset} from "@/models/dto/Dataset";

interface DeleteDatasetButtonProps {
    dataset: Dataset;
    testCaseCount: number;
}

export default function DeleteDatasetButton({ dataset, testCaseCount }: DeleteDatasetButtonProps) {

    const router = useRouter()
    const [showDeleteDatasetDialog, setShowDeleteDatasetDialog] = React.useState(false)

    function handleDatasetDeletion() {
        deleteDataset(dataset.id).then(() => router.refresh());
    }

    return <Dialog open={showDeleteDatasetDialog} onOpenChange={setShowDeleteDatasetDialog}>
        <Button variant="destructive" className="h-full" onClick={() => setShowDeleteDatasetDialog(true)}>
            <Trash2 />
            <span className="pl-2 text-center">Delete Dataset</span>
        </Button>
        <DialogContent>
            <DialogHeader>
                <DialogTitle>Delete Dataset {dataset.name}</DialogTitle>
                <DialogDescription>
                    Are you sure you want to delete the dataset &apos;{dataset.name}&apos; and
                    all <strong>{testCaseCount}</strong> test case{testCaseCount === 1 ? "" : "s"} incl. their labels?
                    This action cannot be undone. Export the dataset first if you want to keep a copy.
                </DialogDescription>
            </DialogHeader>
            <DialogFooter>
                <Button variant="outline" onClick={() => setShowDeleteDatasetDialog(false)}>
                    Cancel
                </Button>
                <Button variant="destructive" type="submit" onClick={handleDatasetDeletion}>Delete</Button>
            </DialogFooter>
        </DialogContent>
    </Dialog>
}