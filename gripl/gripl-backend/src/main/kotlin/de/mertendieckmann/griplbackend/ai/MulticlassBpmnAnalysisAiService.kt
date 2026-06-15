package de.mertendieckmann.griplbackend.ai

import de.mertendieckmann.griplbackend.model.BpmnElement
import de.mertendieckmann.griplbackend.model.analysis.BpmnMulticlassAnalysisResult
import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.UserMessage

interface MulticlassBpmnAnalysisAiService {
    fun analyze(
        @MemoryId sessionId: String,
        @UserMessage bpmnElements: Set<BpmnElement>
    ): BpmnMulticlassAnalysisResult
}