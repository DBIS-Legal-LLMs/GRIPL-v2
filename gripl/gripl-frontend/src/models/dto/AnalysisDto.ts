import {GdprCategory, normalizeFrontendGdprCategories} from "@/models/GdprCategory";

export interface AnalysisRequest {
    bpmnXml: String
}

export interface BinaryAnalysisResponse {
    criticalElements: CriticalElement[];
    amountOfRetries?: number | null;
}

export interface CriticalElement {
    id: string;
    name?: string | null;
    reason: string;
}

export interface MulticlassAnalysisResponse {
    classifiedElements: ClassifiedElement[];
    amountOfRetries?: number | null;
}

export interface ClassifiedElement {
    id: string;
    name?: string | null;
    reason: string;
    classification: GdprCategory[];
}

export type AnalysisResponse = BinaryAnalysisResponse | MulticlassAnalysisResponse;

export interface NormalizedAnalysisElement {
    id: string;
    name: string;
    reason: string;
    classification: GdprCategory[];
}

export function getAnalysisElements(response: AnalysisResponse | null | undefined): NormalizedAnalysisElement[] {
    if (!response) {
        return [];
    }

    if ("classifiedElements" in response) {
        return response.classifiedElements.map((element) => ({
            id: element.id,
            name: element.name || element.id,
            reason: element.reason,
            classification: normalizeFrontendGdprCategories(element.classification as unknown as string[]),
        }));
    }

    return response.criticalElements.map((element) => ({
        id: element.id,
        name: element.name || element.id,
        reason: element.reason,
        classification: [],
    }));
}