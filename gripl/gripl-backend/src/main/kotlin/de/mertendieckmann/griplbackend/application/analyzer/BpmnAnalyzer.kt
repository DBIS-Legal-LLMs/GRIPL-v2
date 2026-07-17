package de.mertendieckmann.griplbackend.application.analyzer

import de.mertendieckmann.griplbackend.model.dto.AnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.RagMode

interface BpmnAnalyzer {

    /**
     * @param activitiesOnly When true, only activities/tasks are classified (the pre-"all elements"
     * behaviour): the LLM prompt is restricted to activities and no RAG context is retrieved for
     * other element types.
     */
    fun analyzeBpmnForGdpr(
        bpmnXml: String,
        useRag: Boolean = false,
        ragMode: RagMode = RagMode.HYBRID,
        activitiesOnly: Boolean = false
    ): AnalysisResponse
}