"use client"

import {ChangeEvent, useRef, useState} from "react";
import {useRouter} from "next/navigation";
import {Upload} from "lucide-react";
import {Button} from "@/components/ui/button";
import {Dataset} from "@/models/dto/Dataset";
import {useToast} from "@/components/ui/toast";
import {authenticatedFetch} from "@/lib/authenticated-fetch";
import {extractErrorDetails, toErrorMessage} from "@/lib/http-error";

interface BulkBpmnUploadButtonProps {
    dataset: Dataset;
}

export default function BulkBpmnUploadButton({dataset}: BulkBpmnUploadButtonProps) {
    // Based on BpmnUploadButton: a hidden file input is opened by a styled button.
    // This input additionally allows selecting multiple files.
    const inputRef = useRef<HTMLInputElement | null>(null);

    const router = useRouter();
    const {showToast, showError} = useToast();

    // New for bulk upload: track whether uploads are running and show progress.
    const [isUploading, setIsUploading] = useState(false);
    const [progress, setProgress] = useState<{ current: number; total: number } | null>(null);

    function openFileSelection() {
        inputRef.current?.click();
    }

    async function uploadFile(file: File) {
        const name = file.name.replace(/\.(bpmn|xml)$/i, "") || file.name;
        const formData = new FormData();

        // Reuses the request format from CreateTestCaseButton so every file
        // becomes a normal, initially unlabeled test case in this dataset.
        formData.append("bpmnFile", file, file.name);
        formData.append("name", name);
        formData.append(
            "expectedValues",
            new Blob([JSON.stringify([])], {type: "application/json"}),
            "expectedValues.json"
        );
        formData.append("datasetId", dataset.id.toString());

        const response = await authenticatedFetch("/api/dataset/testcase", {
            method: "POST",
            body: formData,
        });

        if (!response.ok) {
            throw new Error(await extractErrorDetails(response));
        }
    }

    async function handleFileSelection(event: ChangeEvent<HTMLInputElement>) {
        const selectedFiles = Array.from(event.target.files ?? []);
        event.target.value = "";

        // Uses the same accepted file extensions as the Sandbox upload button.
        const validFiles = selectedFiles.filter((file) =>
            file.name.toLowerCase().endsWith(".bpmn") ||
            file.name.toLowerCase().endsWith(".xml")
        );

        const invalidFiles = selectedFiles.filter((file) => !validFiles.includes(file));

        if (invalidFiles.length > 0) {
            showToast({
                title: `${invalidFiles.length} file${invalidFiles.length === 1 ? "" : "s"} skipped`,
                description: "Only .bpmn and .xml files can be uploaded.",
                variant: "info",
            });
        }

        if (validFiles.length === 0) {
            return;
        }

        setIsUploading(true);
        setProgress({current: 0, total: validFiles.length});

        const failures: string[] = [];

        // New bulk-upload behavior: send one existing test-case request per file.
        // A failed file does not prevent the remaining files from being uploaded.
        for (const [index, file] of validFiles.entries()) {
            setProgress({current: index + 1, total: validFiles.length});

            try {
                await uploadFile(file);
            } catch (error) {
                failures.push(`${file.name}: ${toErrorMessage(error)}`);
            }
        }

        setIsUploading(false);
        setProgress(null);

        // Based on CreateTestCaseButton: reload the dataset list after creation.
        router.refresh();

        const successfulUploads = validFiles.length - failures.length;

        if (successfulUploads > 0) {
            showToast({
                title: `${successfulUploads} BPMN file${successfulUploads === 1 ? "" : "s"} uploaded`,
                description: `Added to dataset "${dataset.name}".`,
                variant: "success",
            });
        }

        if (failures.length > 0) {
            showError("Some BPMN files could not be uploaded", failures.join(" "));
        }
    }

    return <>
        <input
            ref={inputRef}
            type="file"
            accept=".bpmn,.xml"
            multiple
            className="hidden"
            onChange={handleFileSelection}
        />
        <Button
            className="h-full"
            variant="outline"
            onClick={openFileSelection}
            disabled={isUploading}
            title="Upload BPMN files"
        >
            <Upload />
            <span className="pl-2 text-center">
                {progress
                    ? `Uploading ${progress.current}/${progress.total}`
                    : "Upload BPMN files"}
            </span>
        </Button>
    </>;
}