package de.mertendieckmann.griplbackend.ai

import de.mertendieckmann.griplbackend.model.BpmnElement
import de.mertendieckmann.griplbackend.model.analysis.BpmnAnalysisResult
import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.UserMessage

interface PromptBpmnAnalysisAiService {

    
    // Analyse BPMN elements without RAG context.
    
    fun analyze(
        @MemoryId sessionId: String,
        @UserMessage bpmnElements: Set<BpmnElement>
    ): BpmnAnalysisResult

    /**
     * Analyse BPMN elements with RAG context injected.
     * [formattedPrompt] is a pre-built string that combines the retrieved
     * legal knowledge and the BPMN elements in a single message.
     */
    fun analyzeWithRagContext(
        @MemoryId sessionId: String,
        @UserMessage formattedPrompt: String
    ): BpmnAnalysisResult
}