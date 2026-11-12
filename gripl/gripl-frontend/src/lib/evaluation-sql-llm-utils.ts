import { ModelConfig, LLMComponentKey, LLM_COMPONENTS } from "@/components/evaluation/config/sql-llms-config";
import { MultiEvaluationRequest } from "@/models/dto/MultiEvaluationRequest";


export function flattenSqlLlmConfigs(
    configs: Record<LLMComponentKey, ModelConfig[]>
): { modelsString: string; baseUrlsString: string } {
    const modelParts: string[] = [];
    const baseUrlParts: string[] = [];

    LLM_COMPONENTS.forEach(component => {
        const compConfigs = configs[component.key] || [];
        const compModels: string[] = [];
        const compBaseUrls: string[] = [];

        compConfigs.forEach(cfg => {
            const modelName = cfg.model.trim();
            if (!modelName) return;
            compModels.push(modelName);
            compBaseUrls.push(cfg.baseUrl.trim());
        });

        if (compModels.length > 0) {
            modelParts.push(`${component.key}:${compModels.join(',')}`);
            baseUrlParts.push(`${component.key}:${compBaseUrls.join(',')}`);
        }
    });

    return {
        modelsString: modelParts.join(';'),
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

    const { modelsString, baseUrlsString } = flattenSqlLlmConfigs(sqlLlmConfigs);

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
            baseUrl: baseUrlsString,
        },
    };

    return {
        ...request,
        models: [patchedFirstModel, ...restModels],
    };
}