"use client"

import type React from "react"
import {useRef} from "react"
import {Button} from "@/components/ui/button"
import {Upload} from "lucide-react"
import {useToast} from "@/components/ui/toast"

interface FileUploadProps {
  onFileLoaded: (content: string) => void
}

export default function BpmnUploadButton({ onFileLoaded }: FileUploadProps) {
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const {showToast, showError} = useToast()

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      // @ts-ignore
      const file = e.target.files[0]
      processFile(file)
    }
  }

  const processFile = (file: File) => {
    if (!file.name.endsWith(".bpmn") && !file.name.endsWith(".xml")) {
      showToast({
        title: "Unsupported file type",
        description: `'${file.name}' is not a BPMN file. Please select a .bpmn or .xml file.`,
        variant: "info",
      })
      return
    }

    const reader = new FileReader()
    reader.onload = (e) => {
      const content = e.target?.result as string
      if (content) {
        onFileLoaded(content)
      } else {
        console.error("Error while reading file: content is empty")
        showError("Could not read the file", `'${file.name}' appears to be empty.`)
      }
    }
    reader.onerror = () => {
      console.error("Error while reading file:", reader.error)
      showError("Could not read the file", reader.error?.message ?? `Failed to read '${file.name}'.`)
    }
    reader.readAsText(file)
  }

  const handleButtonClick = () => {
    fileInputRef.current?.click()
  }

  return <div>
    <input type="file" accept=".bpmn,.xml" className="hidden" onChange={handleFileChange} ref={fileInputRef}/>
    <Button
        variant="outline"
        size="sm"
        onClick={handleButtonClick}
        title="Import"
    >
      <Upload className="h-4 w-4 mr-1"/>
        Import
    </Button>
  </div>
}

