export type CustomAnalysisResponseType = "BINARY" | "MULTICLASS";

export interface CustomAnalysisEndpoint {
    id: number;
    name: string;
    promptText: string;
    responseType: CustomAnalysisResponseType;
    ragEnabled: boolean;
    ragMode?: string | null;
    createdAt: string;
    updatedAt: string;
}
