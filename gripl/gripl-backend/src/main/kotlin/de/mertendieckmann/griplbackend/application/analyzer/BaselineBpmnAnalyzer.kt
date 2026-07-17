package de.mertendieckmann.griplbackend.application.analyzer

import de.mertendieckmann.griplbackend.ai.BaselineBpmnAnalysisAiServiceFactory
import de.mertendieckmann.griplbackend.ai.SharedChatMemoryProvider
import de.mertendieckmann.griplbackend.application.BpmnExtractor
import de.mertendieckmann.griplbackend.application.SafetyNet
import de.mertendieckmann.griplbackend.model.dto.AnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.RagMode
import dev.langchain4j.model.chat.ChatModel
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

class BaselineBpmnAnalyzer(
    private val llm: ChatModel
): BpmnAnalyzer {

    private val log = KotlinLogging.logger { }
    private val memoryProvider = SharedChatMemoryProvider(50)
    private val safetyNet = SafetyNet(llm, memoryProvider)

    override fun analyzeBpmnForGdpr(bpmnXml: String, useRag: Boolean, ragMode: RagMode, activitiesOnly: Boolean): AnalysisResponse {
        if (useRag) {
            log.warn { "Baseline analyzer does not support RAG — useRag=true is ignored; results are baseline-only." }
        }
        val sessionId = UUID.randomUUID().toString()

        val bpmnAnalysisAiService = BaselineBpmnAnalysisAiServiceFactory.create(llm, memoryProvider, activitiesOnly)
        val bpmnElements = BpmnExtractor().extractBpmnElements(bpmnXml)

        val result = safetyNet.safeGuardAnalysisResultParsing(
            sessionId = sessionId,
            maxRetries = 3
        ) {
            bpmnAnalysisAiService.analyze(sessionId, bpmnXml)
        }

        val analysisResult = result.first.resolveActivities(bpmnElements)
        val amountOfRetries = result.second

        log.info { "BPMN Analysis Result: $analysisResult" }

        return AnalysisResponse.fromBpmnAnalysisResult(analysisResult, bpmnElements, amountOfRetries)
    }
}