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
import React, {useState} from "react";
import {Label} from "@/components/ui/label";
import {Input} from "@/components/ui/input";
import {useRouter} from "next/navigation";
import {Upload} from "lucide-react";
import {useToast} from "@/components/ui/toast";
import {extractErrorDetails, toErrorMessage} from "@/lib/http-error";
import {Spinner} from "@/components/ui/spinner";

interface ImportedDataset {
    originalId: string;
    id: number;
    name: string;
    testCaseCount: number;
}

/**
 * Imports datasets incl. labels, either as ZIP (from the export) or as dataset.csv + evaluation_data.csv.
 * Always creates new datasets. Files from before multiclass labels are imported with labels without class.
 */
export default function ImportDatasetsButton() {

    const router = useRouter()
    const [isOpen, setIsOpen] = useState(false)
    const [isLoading, setIsLoading] = useState(false)
    const [files, setFiles] = useState<File[]>([])
    const {showToast, showError} = useToast()

    function handleOpenChange(open: boolean) {
        setIsOpen(open)
        if (!open) setFiles([])
    }

    function buildFormData(): FormData | string {
        const zip = files.find(file => file.name.toLowerCase().endsWith(".zip"))
        const datasetCsv = files.find(file => file.name.toLowerCase().includes("dataset") && file.name.toLowerCase().endsWith(".csv")
            && !file.name.toLowerCase().includes("evaluation"))
        const evaluationDataCsv = files.find(file => file.name.toLowerCase().includes("evaluation") && file.name.toLowerCase().endsWith(".csv"))

        const formData = new FormData()
        if (zip && files.length === 1) {
            formData.append("file", zip, zip.name)
            return formData
        }
        if (datasetCsv && evaluationDataCsv && files.length === 2) {
            formData.append("datasetCsv", datasetCsv, datasetCsv.name)
            formData.append("evaluationDataCsv", evaluationDataCsv, evaluationDataCsv.name)
            return formData
        }
        return "Please select either one ZIP file or the two files dataset.csv and evaluation_data.csv."
    }

    async function handleImport() {
        const formData = buildFormData()
        if (typeof formData === "string") {
            showToast({title: formData, variant: "info"})
            return
        }

        setIsLoading(true)
        try {
            const response = await fetch("/api/dataset/import", {method: "POST", body: formData})
            if (!response.ok) {
                throw new Error(await extractErrorDetails(response))
            }
            const imported: ImportedDataset[] = await response.json()
            const testCaseCount = imported.reduce((sum, dataset) => sum + dataset.testCaseCount, 0)
            showToast({
                title: `Imported ${imported.length} dataset${imported.length === 1 ? "" : "s"} with ${testCaseCount} test cases`,
                description: imported.map(dataset => dataset.name).join(", "),
                variant: "success"
            })
            handleOpenChange(false)
            router.refresh()
        } catch (error) {
            console.error("There was an error importing the datasets:", error)
            showError("Failed to import datasets", toErrorMessage(error))
        } finally {
            setIsLoading(false)
        }
    }

    return <Dialog open={isOpen} onOpenChange={handleOpenChange}>
        <Button variant="outline" className="h-full" onClick={() => setIsOpen(true)}>
            <Upload />
            <span className="pl-2 text-center">Import</span>
        </Button>
        <DialogContent>
            <DialogHeader>
                <DialogTitle>Import Datasets</DialogTitle>
                <DialogDescription>
                    Select a ZIP from the export or the two files <code>dataset.csv</code> and <code>evaluation_data.csv</code>.
                    Every dataset is created as a new dataset. If a dataset with the same name already exists,
                    the name gets the suffix &quot;(imported)&quot;.
                    Labels from the version before multiclass are imported without class and can be upgraded in multiclass mode.
                </DialogDescription>
            </DialogHeader>
            <div className="space-y-2 py-2 pb-4">
                <Label htmlFor="import-files">Files</Label>
                <Input
                    id="import-files"
                    type="file"
                    multiple
                    accept=".zip,.csv"
                    onChange={(e) => setFiles(Array.from(e.target.files ?? []))}
                />
                {files.length > 0 && (
                    <p className="text-xs text-muted-foreground">{files.map(file => file.name).join(", ")}</p>
                )}
            </div>
            <DialogFooter>
                <Button variant="outline" onClick={() => handleOpenChange(false)}>
                    Cancel
                </Button>
                <Button type="submit" onClick={handleImport} disabled={isLoading || files.length === 0}>
                    {isLoading && <Spinner className="h-4 w-4 text-foreground"/>}
                    Import
                </Button>
            </DialogFooter>
        </DialogContent>
    </Dialog>
}
