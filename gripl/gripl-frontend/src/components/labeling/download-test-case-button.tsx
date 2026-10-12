"use client"

import {Button} from "@/components/ui/button";
import React, {useState} from "react";
import {Download} from "lucide-react";
import {useToast} from "@/components/ui/toast";
import {extractErrorDetails, toErrorMessage} from "@/lib/http-error";
import {EvaluationData} from "@/models/dto/EvaluationData";
import {downloadBpmnXml} from "@/lib/download-bpmn";
import {Spinner} from "@/components/ui/spinner";

export interface DownloadTestCaseButtonProps {
    testCaseId: number
    testCaseName?: string
}

/**
 * Downloads the saved BPMN XML of a test case (without labels).
 */
export default function DownloadTestCaseButton({ testCaseId, testCaseName }: DownloadTestCaseButtonProps) {

    const [isLoading, setIsLoading] = useState(false);
    const {showError} = useToast();

    const handleDownloadClick = async (e: React.MouseEvent) => {
        e.stopPropagation()
        e.preventDefault()
        setIsLoading(true);
        try {
            const response = await fetch(`/api/dataset/testcase/${testCaseId}`);
            if (!response.ok) {
                throw new Error(await extractErrorDetails(response));
            }
            const data: EvaluationData = await response.json();
            downloadBpmnXml(data.bpmnXml, data.name || testCaseName || `testcase-${testCaseId}`);
        } catch (error) {
            console.error("There was an error downloading the test case:", error);
            showError("Failed to download the test case", toErrorMessage(error));
        } finally {
            setIsLoading(false);
        }
    };

    return <Button variant="outline" className="z-10" size="icon" disabled={isLoading}
                   onClick={handleDownloadClick} title="Download BPMN">
        {isLoading ? <Spinner className="h-4 w-4 text-foreground"/> : <Download className="h-4 w-4"/>}
    </Button>
}
