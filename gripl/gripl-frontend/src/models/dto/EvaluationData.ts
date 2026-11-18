export interface EvaluationData {
    id: number,
    name: string,
    bpmnXml: string,
    expectedValues: ExpectedValues[],
    datasetId: number
}

import {GdprCategory} from "@/models/GdprCategory";
import {Label} from "@/models/dto/Label";

export interface ExpectedValues {
    value: string
    classification: GdprCategory[]
    /** Free-text gold-standard explanation authored by a researcher during labeling. */
    explanation?: string
    /** Response-only assignments; archived assignments are retained until explicitly removed. */
    labels?: Label[]
    /** Write-only assignment IDs sent when saving a test case. */
    labelIds?: number[]
}

export interface EvaluationDataMeta {
    id: number,
    name?: string,
    datasetId?: number,
    labelCount?: number,
    /** Labels without a GDPR class (binary / legacy labels) */
    unclassifiedLabelCount?: number
}