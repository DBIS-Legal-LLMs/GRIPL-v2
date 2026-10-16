package de.mertendieckmann.griplbackend.ai

import de.mertendieckmann.griplbackend.model.BpmnElement
import de.mertendieckmann.griplbackend.model.analysis.BpmnMulticlassAnalysisResult
import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.UserMessage

interface MulticlassBpmnAnalysisAiService {

    // Classify BPMN elements without RAG context.
    fun analyze(
        @MemoryId sessionId: String,
        @UserMessage bpmnElements: Set<BpmnElement>
    ): BpmnMulticlassAnalysisResult

    /**
     * Classify BPMN elements with RAG context injected.
     * [formattedPrompt] is a pre-built string that combines the retrieved
     * legal knowledge and the BPMN elements in a single message.
     */
    fun analyzeWithRagContext(
        @MemoryId sessionId: String,
        @UserMessage formattedPrompt: String
    ): BpmnMulticlassAnalysisResult
}
