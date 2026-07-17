package de.mertendieckmann.griplbackend.model.dto

import de.mertendieckmann.griplbackend.application.BpmnExtractor
import de.mertendieckmann.griplbackend.model.BpmnElement
import de.mertendieckmann.griplbackend.model.analysis.BpmnAnalysisResult

data class AnalysisResponse(
    val criticalElements: List<CriticalElement>,
    val amountOfRetries: Int? = null,
    val ragContext: Map<String, RagElementContext>? = null,
    val ragPromptContext: List<String>? = null
) {
    data class CriticalElement(
        val id: String,
        val name: String?,
        val type: String? = null,
        val reason: String,
        val references: List<LlmReference> = emptyList()
    )

    companion object {
        fun fromBpmnAnalysisResult(
            result: BpmnAnalysisResult,
            bpmnElements: Set<BpmnElement>,
            amountOfRetries: Int,
            ragContext: Map<String, RagElementContext>? = null,
            ragPromptContext: List<String>? = null
        ): AnalysisResponse {
            val elements = result.elements.map { element ->
                val bpmnElement = bpmnElements.find { it.id == element.id }
                CriticalElement(
                    id = element.id,
                    // Prefer the element's real BPMN name, then the deterministic name
                    // derived from its labeled flows; the LLM-generated label is the last
                    // resort for unnamed elements without any labeled flows.
                    name = bpmnElement?.name?.takeIf { it.isNotBlank() }
                        ?: bpmnElement?.derivedNameFromFlows()
                        ?: element.label,
                    type = bpmnElement?.type,
                    reason = element.reason,
                    references = element.references.map { ref ->
                        LlmReference(exactText = ref.exactText, sourceDocument = ref.sourceDocument)
                    }
                )
            }

            return AnalysisResponse(
                criticalElements = elements,
                amountOfRetries = amountOfRetries,
                ragContext = ragContext,
                ragPromptContext = ragPromptContext
            )
        }

        fun fromBpmnAnalysisResult(result: BpmnAnalysisResult, bpmnXml: String, amountOfRetries: Int): AnalysisResponse {
            val extractor = BpmnExtractor()
            val bpmnElements = extractor.extractBpmnElements(bpmnXml)
            return fromBpmnAnalysisResult(result, bpmnElements, amountOfRetries)
        }
    }
}

data class RagElementContext(
    val activityName: String?,
    val entities: List<RagEntity>,
    val relationships: List<RagRelationship>,
    val documents: List<RagDocument>
)

data class RagEntity(
    val label: String,
    val type: String,
    val description: String
)

data class RagRelationship(
    val source: String,
    val target: String,
    val label: String
)

data class LlmReference(
    val exactText: String,
    val sourceDocument: String? = null
)

data class RagDocument(
    val content: String,
    val preview: String,
    val sourceDocument: String? = null
)