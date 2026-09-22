"use client"

import type React from "react"
import {useRef, useState} from "react"
import {Button} from "@/components/ui/button"
import {Upload} from "lucide-react"
import {useToast} from "@/components/ui/toast"
import {Spinner} from "@/components/ui/spinner"
import {ProcessModelListItem} from "@/models/dto/ProcessModel"

interface UploadProcessModelsButtonProps {
    onUploaded: (models: ProcessModelListItem[]) => void
}

export default function UploadProcessModelsButton({onUploaded}: UploadProcessModelsButtonProps) {
    const fileInputRef = useRef<HTMLInputElement | null>(null)
    const [isUploading, setIsUploading] = useState(false)
    const {showToast, showError} = useToast()

    async function uploadFile(file: File): Promise<ProcessModelListItem | null> {
        const formData = new FormData()
        formData.append("bpmnFile", file, file.name)
        formData.append("name", file.name)

        const response = await fetch("/api/process-models", {
            method: "POST",
            body: formData,
        })

        if (!response.ok) {
            throw new Error(`Failed to upload '${file.name}': ${response.statusText}`)
        }

        return await response.json() as ProcessModelListItem
    }

    const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files
        if (!files || files.length === 0) return

        const validFiles = Array.from(files).filter(file => {
            const isValid = file.name.endsWith(".bpmn") || file.name.endsWith(".xml")
            if (!isValid) {
                showToast({
                    title: "Unsupported file type",
                    description: `'${file.name}' is not a BPMN file. Please select a .bpmn or .xml file.`,
                    variant: "info",
                })
            }
            return isValid
        })

        if (validFiles.length === 0) {
            e.target.value = ""
            return
        }

        setIsUploading(true)
        const uploaded: ProcessModelListItem[] = []
        for (const file of validFiles) {
            try {
                const model = await uploadFile(file)
                if (model) uploaded.push(model)
            } catch (error) {
                console.error("Error uploading process model:", error)
                showError("Upload failed", error instanceof Error ? error.message : `Failed to upload '${file.name}'.`)
            }
        }
        setIsUploading(false)
        e.target.value = ""
        if (uploaded.length > 0) onUploaded(uploaded)
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
            Upload Process Models
        </Button>
    </div>
}
