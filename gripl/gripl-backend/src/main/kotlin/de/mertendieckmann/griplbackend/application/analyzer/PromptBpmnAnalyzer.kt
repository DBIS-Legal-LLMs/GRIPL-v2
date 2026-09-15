package de.mertendieckmann.griplbackend.application.analyzer

import de.mertendieckmann.griplbackend.ai.JsonFixAiServiceFactory
import de.mertendieckmann.griplbackend.ai.PromptBpmnAnalysisAiServiceFactory
import de.mertendieckmann.griplbackend.ai.SharedChatMemoryProvider
import de.mertendieckmann.griplbackend.application.BpmnExtractor
import de.mertendieckmann.griplbackend.application.SafetyNet
import de.mertendieckmann.griplbackend.adapter.rag.RagApiClient
import de.mertendieckmann.griplbackend.application.rag.RagContextAugmenter
import de.mertendieckmann.griplbackend.config.RagApiProperties
import de.mertendieckmann.griplbackend.model.dto.AnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.RagMode
import dev.langchain4j.model.chat.ChatModel
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

/**
 * Binary (critical/not-critical) BPMN analyzer. [systemPrompt] resolves the base system prompt for
 * a given `activitiesOnly` flag — the built-in "prompt-engineering" endpoint resolves it from the
 * bundled resource files (see [PromptBpmnAnalysisAiServiceFactory]'s defaults), while a custom
 * uploaded endpoint just returns its stored prompt text regardless of `activitiesOnly`, since a
 * custom prompt has no separate activities-only variant.
 */
class PromptBpmnAnalyzer(
    private val llm: ChatModel,
    private val ragApiClient: RagApiClient,
    private val ragApiProperties: RagApiProperties,
    private val systemPrompt: (activitiesOnly: Boolean) -> String = { activitiesOnly ->
        if (activitiesOnly) PromptBpmnAnalysisAiServiceFactory.defaultActivitiesOnlyBasePrompt
        else PromptBpmnAnalysisAiServiceFactory.defaultBasePrompt
    }
) : BpmnAnalyzer {

    private val log = KotlinLogging.logger { }
    private val memoryProvider = SharedChatMemoryProvider(50)
    private val jsonFixAiService = JsonFixAiServiceFactory.create(llm, memoryProvider)
    private val safetyNet = SafetyNet { sessionId, error -> jsonFixAiService.fixAnalysisResultJson(sessionId, error) }
    private val ragAugmenter = RagContextAugmenter(ragApiClient, ragApiProperties)

    override fun analyzeBpmnForGdpr(bpmnXml: String, useRag: Boolean, ragMode: RagMode, activitiesOnly: Boolean): AnalysisResponse {
        val sessionId = UUID.randomUUID().toString()

        val bpmnElements = BpmnExtractor().extractBpmnElements(bpmnXml)
        val basePrompt = systemPrompt(activitiesOnly)

        if (useRag) {
            // RAG-augmented path
            val bpmnAnalysisAiServiceWithRag = PromptBpmnAnalysisAiServiceFactory.create(llm, memoryProvider, basePrompt)
            val ragContextMap = ragAugmenter.fetchRagContext(
                bpmnElements, ragMode, maxConcurrency = ragApiProperties.maxConcurrency, activitiesOnly = activitiesOnly
            )

            val pool = ragAugmenter.buildDedupedPool(ragContextMap)

            log.debug {
                "Deduped RAG pool: ${pool.entityLines.size} entities | " +
                    "${pool.relationshipLines.size} relationships | ${pool.documentLines.size} documents"
            }

            val result = safetyNet.safeGuardResultParsing(sessionId, maxRetries = 3) {
                val formattedPrompt = ragAugmenter.renderCombinedPrompt(bpmnElements, pool)
                bpmnAnalysisAiServiceWithRag.analyzeWithRagContext(sessionId, formattedPrompt)
            }

            val analysisResult = result.first.resolveActivities(bpmnElements)
            val amountOfRetries = result.second
            val ragContext = ragAugmenter.parseRagContextForResponse(ragContextMap, bpmnElements)

            log.info { "BPMN Analysis Result (with RAG): $analysisResult" }

            return AnalysisResponse.fromBpmnAnalysisResult(
                analysisResult, bpmnElements, amountOfRetries, ragContext, pool.flatten()
            )
        } else {
            // Original path — unchanged from evaluation baseline
            val bpmnAnalysisAiServiceNoRag = PromptBpmnAnalysisAiServiceFactory.createWithoutRag(llm, memoryProvider, basePrompt)
            val result = safetyNet.safeGuardResultParsing(sessionId, maxRetries = 3) {
                bpmnAnalysisAiServiceNoRag.analyze(sessionId, bpmnElements)
            }

            val analysisResult = result.first.resolveActivities(bpmnElements)
            val amountOfRetries = result.second

            log.info { "BPMN Analysis Result: $analysisResult" }

            return AnalysisResponse.fromBpmnAnalysisResult(analysisResult, bpmnElements, amountOfRetries)
        }
    }
}
