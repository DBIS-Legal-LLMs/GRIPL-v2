package de.mertendieckmann.griplbackend.application.analyzer

import de.mertendieckmann.griplbackend.adapter.rag.RagApiClient
import de.mertendieckmann.griplbackend.ai.MulticlassBpmnAnalysisAiServiceFactory
import de.mertendieckmann.griplbackend.ai.MulticlassJsonFixAiServiceFactory
import de.mertendieckmann.griplbackend.ai.SharedChatMemoryProvider
import de.mertendieckmann.griplbackend.application.BpmnExtractor
import de.mertendieckmann.griplbackend.application.SafetyNet
import de.mertendieckmann.griplbackend.application.rag.RagContextAugmenter
import de.mertendieckmann.griplbackend.config.RagApiProperties
import de.mertendieckmann.griplbackend.model.dto.MulticlassAnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.RagMode
import dev.langchain4j.model.chat.ChatModel
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

/**
 * Multiclass (GDPR processing-class) BPMN analyzer. [basePrompt] defaults to the built-in
 * classification prompt; a custom uploaded multiclass endpoint passes its own stored prompt text
 * instead — the classification taxonomy itself ([de.mertendieckmann.griplbackend.model.analysis.GdprProcessingClass])
 * stays fixed either way, since it's baked into the structured-output schema.
 */
class MulticlassBpmnAnalyzer(
    private val llm: ChatModel,
    private val ragApiClient: RagApiClient,
    private val ragApiProperties: RagApiProperties,
    private val basePrompt: String = MulticlassBpmnAnalysisAiServiceFactory.defaultPrompt
) {
    private val log = KotlinLogging.logger { }
    private val memoryProvider = SharedChatMemoryProvider(50)
    private val bpmnAnalysisAiService = MulticlassBpmnAnalysisAiServiceFactory.create(llm, memoryProvider, basePrompt)
    private val jsonFixAiService = MulticlassJsonFixAiServiceFactory.create(llm, memoryProvider)
    private val safetyNet = SafetyNet { sessionId, error -> jsonFixAiService.fixAnalysisResultJson(sessionId, error) }
    private val ragAugmenter = RagContextAugmenter(ragApiClient, ragApiProperties)

    fun analyzeBpmnForGdpr(
        bpmnXml: String,
        useRag: Boolean = false,
        ragMode: RagMode = RagMode.HYBRID,
        activitiesOnly: Boolean = false
    ): MulticlassAnalysisResponse {
        val sessionId = UUID.randomUUID().toString()
        val bpmnElements = BpmnExtractor().extractBpmnElements(bpmnXml)

        if (useRag) {
            val ragContextMap = ragAugmenter.fetchRagContext(
                bpmnElements, ragMode, maxConcurrency = ragApiProperties.maxConcurrency, activitiesOnly = activitiesOnly
            )
            val pool = ragAugmenter.buildDedupedPool(ragContextMap)

            val result = safetyNet.safeGuardResultParsing(sessionId, maxRetries = 3) {
                val formattedPrompt = ragAugmenter.renderCombinedPrompt(bpmnElements, pool)
                bpmnAnalysisAiService.analyzeWithRagContext(sessionId, formattedPrompt)
            }

            val analysisResult = result.first.resolveActivities(bpmnElements)
            val ragContext = ragAugmenter.parseRagContextForResponse(ragContextMap, bpmnElements)

            log.info { "BPMN Multiclass Analysis Result (with RAG): $analysisResult" }

            return MulticlassAnalysisResponse.fromBpmnMulticlassAnalysisResult(
                analysisResult, bpmnElements, result.second, ragContext, pool.flatten()
            )
        } else {
            val result = safetyNet.safeGuardResultParsing(sessionId, maxRetries = 3) {
                bpmnAnalysisAiService.analyze(sessionId, bpmnElements)
            }

            val analysisResult = result.first.resolveActivities(bpmnElements)

            log.info { "BPMN Multiclass Analysis Result: $analysisResult" }

            return MulticlassAnalysisResponse.fromBpmnMulticlassAnalysisResult(
                analysisResult, bpmnElements, result.second
            )
        }
    }
}
