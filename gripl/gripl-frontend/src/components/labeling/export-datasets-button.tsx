"use client"

import {Button} from "@/components/ui/button";
import {Download} from "lucide-react";
import React, {useState} from "react";
import {Dataset} from "@/models/dto/Dataset";
import {useToast} from "@/components/ui/toast";
import {extractErrorDetails, toErrorMessage} from "@/lib/http-error";
import {Spinner} from "@/components/ui/spinner";

interface ExportDatasetsButtonProps {
    /** Exports only this dataset; without a dataset all datasets are exported */
    dataset?: Dataset;
}

/**
 * Downloads a ZIP with dataset.csv and evaluation_data.csv (incl. labels) in the format of the original Postgres export.
 */
export default function ExportDatasetsButton({ dataset }: ExportDatasetsButtonProps) {

    const [isLoading, setIsLoading] = useState(false);
    const {showError} = useToast();

    async function handleExport() {
        setIsLoading(true);
        try {
            const url = dataset ? `/api/dataset/${dataset.id}/export` : "/api/dataset/export";
            const response = await fetch(url);
            if (!response.ok) {
                throw new Error(await extractErrorDetails(response));
            }
            const fileName = response.headers.get("Content-Disposition")?.match(/filename="?([^";]+)"?/)?.[1]
                ?? "gripl-datasets.zip";
            const objectUrl = URL.createObjectURL(await response.blob());
            const a = document.createElement("a");
            a.href = objectUrl;
            a.download = fileName;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(objectUrl);
        } catch (error) {
            console.error("There was an error exporting the dataset(s):", error);
            showError("Failed to export", toErrorMessage(error));
        } finally {
            setIsLoading(false);
        }
    }

    return <Button
        variant="outline"
        className="h-full"
        onClick={handleExport}
        disabled={isLoading}
        title={dataset ? `Export dataset '${dataset.name}' incl. labels` : "Export all datasets incl. labels"}
    >
        {isLoading ? <Spinner className="h-4 w-4 text-foreground"/> : <Download />}
        <span className="pl-2 text-center">{dataset ? "Export" : "Export All"}</span>
    </Button>
}
