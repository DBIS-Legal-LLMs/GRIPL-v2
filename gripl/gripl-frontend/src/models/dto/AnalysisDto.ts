import {GdprCategory} from "@/models/GdprCategory";

export interface AnalysisRequest {
    bpmnXml: String
}

export interface BinaryAnalysisResponse {
    criticalElements: CriticalElement[];
<<<<<<< HEAD
    ragContext?: Record<string, RagElementContext>;
=======
    amountOfRetries?: number | null;
>>>>>>> d96d174 (frontend with backend merge)
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
    type?: string;
    reason: string;
<<<<<<< HEAD
    references?: LlmReference[];
}

export interface LlmReference {
    exactText: string;
    sourceDocument?: string | null;
}

export interface RagElementContext {
    activityName: string | null;
    entities: RagEntity[];
    relationships: RagRelationship[];
    documents: RagDocument[];
}

export interface RagEntity {
    label: string;
    type: string;
    description: string;
}

export interface RagRelationship {
    source: string;
    target: string;
    label: string;
}

export interface RagDocument {
    content: string;
    preview: string;
    sourceDocument?: string | null;
}

export interface PdfHighlightRect {
    x0: number;
    y0: number;
    x1: number;
    y1: number;
}

export interface PdfLocateResponse {
    page: number | null;
    rects: PdfHighlightRect[];
    page_width: number;
    page_height: number;
=======
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
            classification: element.classification,
        }));
    }

    return response.criticalElements.map((element) => ({
        id: element.id,
        name: element.name || element.id,
        reason: element.reason,
        classification: [],
    }));
>>>>>>> d96d174 (frontend with backend merge)
}