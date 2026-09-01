package de.mertendieckmann.griplbackend.application.analyzer

import de.mertendieckmann.griplbackend.adapter.web.utils.ControllerUtils
import de.mertendieckmann.griplbackend.config.LlmConfig
import de.mertendieckmann.griplbackend.model.dto.AnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.MulticlassAnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.RagMode
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

/**
 * Shared "build LLM -> pick analyzer -> analyze" wiring used by both the
 * synchronous /gdpr/analysis endpoints (AnalysisController) and the sequential
 * process-model job runner (ProcessModelJobRunner), so the two don't drift.
 */
@Service
class AnalysisService(
    private val analyzerFactory: AnalyzerFactory,
    private val llmConfig: LlmConfig,
    private val env: Environment
) {

    fun analyzePromptEngineering(
        bpmnXml: String,
        llmPropsOverride: LlmConfig.Companion.LlmPropsOverride?,
        useRag: Boolean,
        ragMode: RagMode,
        activitiesOnly: Boolean
    ): AnalysisResponse {
        val resolvedOverride = ControllerUtils.resolveEnvironmentVariables(llmPropsOverride, env)
        val llm = llmConfig.buildStrictJsonModelWithOverride(resolvedOverride)
        val analyzer = analyzerFactory.createPromptEngineeringAnalyzer(llm)
        return analyzer.analyzeBpmnForGdpr(
            bpmnXml = bpmnXml,
            useRag = useRag,
            ragMode = ragMode,
            activitiesOnly = activitiesOnly
        )
    }

    fun analyzeBaseline(
        bpmnXml: String,
        llmPropsOverride: LlmConfig.Companion.LlmPropsOverride?,
        useRag: Boolean,
        ragMode: RagMode,
        activitiesOnly: Boolean
    ): AnalysisResponse {
        val resolvedOverride = ControllerUtils.resolveEnvironmentVariables(llmPropsOverride, env)
        val llm = llmConfig.buildStrictJsonModelWithOverride(resolvedOverride)
        val analyzer = analyzerFactory.createBaselineAnalyzer(llm)
        return analyzer.analyzeBpmnForGdpr(
            bpmnXml = bpmnXml,
            useRag = useRag,
            ragMode = ragMode,
            activitiesOnly = activitiesOnly
        )
    }

    fun analyzeMulticlass(
        bpmnXml: String,
        llmPropsOverride: LlmConfig.Companion.LlmPropsOverride?
    ): MulticlassAnalysisResponse {
        val resolvedOverride = ControllerUtils.resolveEnvironmentVariables(llmPropsOverride, env)
        val llm = llmConfig.buildStrictJsonModelWithOverride(resolvedOverride)
        val analyzer = analyzerFactory.createMulticlassAnalyzer(llm)
        return analyzer.analyzeBpmnForGdpr(bpmnXml)
    }
}
