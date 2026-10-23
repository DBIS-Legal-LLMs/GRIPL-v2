import { ModelConfig, LLMComponentKey, LLM_COMPONENTS } from "@/components/evaluation/config/sql-llms-config";
import { MultiEvaluationRequest } from "@/models/dto/MultiEvaluationRequest";


export function flattenSqlLlmConfigs(
    configs: Record<LLMComponentKey, ModelConfig[]>
): { modelsString: string; apiKeyNamesString: string; baseUrlsString: string } {
    const modelParts: string[] = [];
    const apiKeyParts: string[] = [];
    const baseUrlParts: string[] = [];

    LLM_COMPONENTS.forEach(component => {
        const compConfigs = configs[component.key] || [];
        const compModels: string[] = [];
        const compApiKeys: string[] = [];
        const compBaseUrls: string[] = [];

        compConfigs.forEach(cfg => {
            const modelName = cfg.model.trim();
            if (!modelName) return;
            compModels.push(modelName);
            compApiKeys.push(cfg.apiKeyName.trim());
            compBaseUrls.push(cfg.baseUrl.trim());
        });

        if (compModels.length > 0) {
            modelParts.push(`${component.key}:${compModels.join(',')}`);
            apiKeyParts.push(`${component.key}:${compApiKeys.join(',')}`);
            baseUrlParts.push(`${component.key}:${compBaseUrls.join(',')}`);
        }
    });

    return {
        modelsString: modelParts.join(';'),
        apiKeyNamesString: apiKeyParts.join(';'),
        baseUrlsString: baseUrlParts.join(';'),
    };
}

export function buildEvaluationPayload(
    request: MultiEvaluationRequest,
    sqlLlmConfigs: Record<LLMComponentKey, ModelConfig[]>
): MultiEvaluationRequest {
    if (!request.useSQLLM) {
        return request;
    }

    const { modelsString, apiKeyNamesString, baseUrlsString } = flattenSqlLlmConfigs(sqlLlmConfigs);

    if (!request.models?.length) {
        console.warn("[SQL-LLM] useSQLLM aktiv, aber keine Models im Request.");
        return request;
    }

    const [firstModel, ...restModels] = request.models;

    if (!firstModel.llmProps) {
        console.warn("[SQL-LLM] useSQLLM aktiv, aber llmProps fehlt beim ersten Model.");
        return request;
    }

    const patchedFirstModel = {
        ...firstModel,
        llmProps: {
            ...firstModel.llmProps,
            modelName: modelsString,
            apiKey: apiKeyNamesString,
            baseUrl: baseUrlsString,
        },
    };

    return {
        ...request,
        models: [patchedFirstModel, ...restModels],
    };
}