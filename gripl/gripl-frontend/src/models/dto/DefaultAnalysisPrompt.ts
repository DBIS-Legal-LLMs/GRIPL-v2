import {CustomAnalysisResponseType} from "@/models/dto/CustomAnalysisEndpoint";

export interface DefaultAnalysisPrompt {
    name: string;
    responseType: CustomAnalysisResponseType;
    promptText: string;
}
