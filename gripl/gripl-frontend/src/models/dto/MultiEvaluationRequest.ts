
export interface MultiEvaluationRequest {
    models: ModelRunConfig[];
    datasets: number[];
    evaluationDataIds?: number[];
    defaultEvaluationEndpoint: string;
    seed?: number;
    maxConcurrent: number | null;
    repetitions?: number;
    useRag: boolean;
    ragMode: string;
    evaluateRag: boolean;
    activitiesOnly: boolean;
    /** Optional name of the run (experiment); used for the file names of the reports. */
    name?: string;
}

export interface ModelRunConfig {
    label: string;
    evaluationEndpoint?: string | null;
    llmProps?: LlmPropsOverride | null;
}

export interface LlmPropsOverride {
    baseUrl?: string | null;
    modelName?: string | null;
    apiKey?: string | null;
    timeoutSeconds?: number | null;
}