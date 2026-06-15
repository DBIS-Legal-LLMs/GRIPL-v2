package de.mertendieckmann.griplbackend.application.analyzer

import de.mertendieckmann.griplbackend.ai.MulticlassBpmnAnalysisAiServiceFactory
import de.mertendieckmann.griplbackend.ai.SharedChatMemoryProvider
import de.mertendieckmann.griplbackend.application.BpmnExtractor
import de.mertendieckmann.griplbackend.model.dto.MulticlassAnalysisResponse
import dev.langchain4j.model.chat.ChatModel
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

class MulticlassBpmnAnalyzer(
    llm: ChatModel
) {
    private val log = KotlinLogging.logger { }
    private val memoryProvider = SharedChatMemoryProvider(50)
    private val bpmnAnalysisAiService = MulticlassBpmnAnalysisAiServiceFactory.create(llm, memoryProvider)

    fun analyzeBpmnForGdpr(bpmnXml: String): MulticlassAnalysisResponse {
        val sessionId = UUID.randomUUID().toString()

        val bpmnElements = BpmnExtractor().extractBpmnElements(bpmnXml)

        val result = bpmnAnalysisAiService.analyze(sessionId, bpmnElements)

        val analysisResult = result.resolveActivities(bpmnElements)

        log.info { "BPMN Multiclass Analysis Result: $analysisResult" }

        return MulticlassAnalysisResponse.fromBpmnMulticlassAnalysisResult(
            analysisResult,
            bpmnElements,
            0
        )
    }
}