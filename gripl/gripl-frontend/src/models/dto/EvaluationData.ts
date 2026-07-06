export interface EvaluationData {
    id: number,
    name: string,
    bpmnXml: string,
    expectedValues: ExpectedValues[],
    datasetId: number
}

import {GdprCategory} from "@/models/GdprCategory";

export interface ExpectedValues {
    value: string
    classification: GdprCategory[]
    reason?: string
}

export interface EvaluationDataMeta {
    id: number,
    name?: string,
    datasetId?: number
}