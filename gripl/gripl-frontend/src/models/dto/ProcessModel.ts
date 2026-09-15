import {AnalysisResponse} from "@/models/dto/AnalysisDto";
import {LlmPropsOverride} from "@/models/dto/MultiEvaluationRequest";

export type ProcessModelStatus = "PENDING" | "QUEUED" | "RUNNING" | "DONE" | "ERROR";

export interface ProcessModelListItem {
    id: number;
    name: string;
    status: ProcessModelStatus;
    analysisEndpoint?: string | null;
    totalElements?: number | null;
    criticalElementCount?: number | null;
    errorMessage?: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface ProcessModelDetail {
    id: number;
    name: string;
    bpmnXml: string;
    status: ProcessModelStatus;
    analysisEndpoint?: string | null;
    analysisResult?: AnalysisResponse | null;
    amountOfRetries?: number | null;
    totalElements?: number | null;
    criticalElementCount?: number | null;
    errorMessage?: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface EnqueueAnalysisRequest {
    ids: number[];
    endpoint: string;
    llmProps?: LlmPropsOverride | null;
    useRag?: boolean | null;
    ragMode?: string | null;
    activitiesOnly?: boolean | null;
}

export interface EnqueueAnalysisResponse {
    enqueuedIds: number[];
    skippedIds: number[];
}

export const IN_FLIGHT_STATUSES: ProcessModelStatus[] = ["QUEUED", "RUNNING"];
