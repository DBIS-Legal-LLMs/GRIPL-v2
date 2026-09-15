package de.mertendieckmann.griplbackend.ai

import de.mertendieckmann.griplbackend.model.analysis.BpmnMulticlassAnalysisResult
import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.UserMessage

interface MulticlassJsonFixAiService {

    fun fixAnalysisResultJson(
        @MemoryId sessionId: String,
        @UserMessage error: String
    ): BpmnMulticlassAnalysisResult
}
