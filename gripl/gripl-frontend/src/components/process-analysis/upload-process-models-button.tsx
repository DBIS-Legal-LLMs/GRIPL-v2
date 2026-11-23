"use client"

import type React from "react"
import {useRef, useState} from "react"
import {Button} from "@/components/ui/button"
import {Upload} from "lucide-react"
import {useToast} from "@/components/ui/toast"
import {Spinner} from "@/components/ui/spinner"
import {ProcessModelListItem} from "@/models/dto/ProcessModel"
import {authenticatedFetch} from "@/lib/authenticated-fetch"
import {extractErrorDetails, toErrorMessage} from "@/lib/http-error"

interface UploadProcessModelsButtonProps {
    onUploaded: (models: ProcessModelListItem[]) => void
}

export default function UploadProcessModelsButton({onUploaded}: UploadProcessModelsButtonProps) {
    const fileInputRef = useRef<HTMLInputElement | null>(null)
    const [isUploading, setIsUploading] = useState(false)
    const [progress, setProgress] = useState<{ current: number; total: number } | null>(null)
    const {showToast, showError} = useToast()

    async function uploadFile(file: File): Promise<ProcessModelListItem> {
        const formData = new FormData()
        formData.append("bpmnFile", file, file.name)
        formData.append("name", file.name)

        const response = await authenticatedFetch("/api/process-models", {
            method: "POST",
            body: formData,
        })

        if (!response.ok) {
            throw new Error(await extractErrorDetails(response))
        }

        return await response.json() as ProcessModelListItem
    }

    // Reports skipped, uploaded and failed files the same way as the labeling bulk upload.
    const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const selectedFiles = Array.from(e.target.files ?? [])
        e.target.value = ""
        if (selectedFiles.length === 0) return

        const validFiles = selectedFiles.filter(file =>
            file.name.toLowerCase().endsWith(".bpmn") ||
            file.name.toLowerCase().endsWith(".xml")
        )
        const invalidFiles = selectedFiles.filter(file => !validFiles.includes(file))

        if (invalidFiles.length > 0) {
            showToast({
                title: `${invalidFiles.length} file${invalidFiles.length === 1 ? "" : "s"} skipped`,
                description: "Only .bpmn and .xml files can be uploaded.",
                variant: "info",
            })
        }

        if (validFiles.length === 0) return

        setIsUploading(true)
        setProgress({current: 0, total: validFiles.length})

        const uploaded: ProcessModelListItem[] = []
        const failures: string[] = []

        // A failed file does not prevent the remaining files from being uploaded.
        for (const [index, file] of validFiles.entries()) {
            setProgress({current: index + 1, total: validFiles.length})
            try {
                uploaded.push(await uploadFile(file))
            } catch (error) {
                console.error("Error uploading process model:", error)
                failures.push(`${file.name}: ${toErrorMessage(error)}`)
            }
        }

        setIsUploading(false)
        setProgress(null)

        if (uploaded.length > 0) {
            onUploaded(uploaded)
            showToast({
                title: `${uploaded.length} BPMN file${uploaded.length === 1 ? "" : "s"} uploaded`,
                description: "Added to Process Analysis.",
                variant: "success",
            })
        }

        if (failures.length > 0) {
            showError("Some BPMN files could not be uploaded", failures.join(" "))
        }
    }

    return <div>
        <input
            type="file"
            accept=".bpmn,.xml"
            multiple
            className="hidden"
            onChange={handleFileChange}
            ref={fileInputRef}
        />
        <Button
            variant="default"
            onClick={() => fileInputRef.current?.click()}
            disabled={isUploading}
        >
            {isUploading ? <Spinner size="small" className="mr-2 h-4 w-4"/> : <Upload className="mr-2 h-4 w-4"/>}
            {progress ? `Uploading ${progress.current}/${progress.total}` : "Upload Process Models"}
        </Button>
    </div>
}
