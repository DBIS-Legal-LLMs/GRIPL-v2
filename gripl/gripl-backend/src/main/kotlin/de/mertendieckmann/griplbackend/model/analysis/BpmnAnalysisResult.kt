package de.mertendieckmann.griplbackend.model.analysis

import de.mertendieckmann.griplbackend.model.BpmnElement
import io.github.oshai.kotlinlogging.KotlinLogging
import jdk.jfr.Description

data class BpmnAnalysisResult(
    @Description("List of BPMN Elements (activities, events, gateways, data objects) that are classified as relevant for GDPR compliance")
    var elements: List<Element>
) {
    init {
        elements = elements
            .filter { it.isRelevant }
    }

    @Description("Represents a BPMN Element (activity/task, event, gateway, data object/store) that is classified as relevant for GDPR compliance")
    data class Element(
        @Description("The ID of the BPMN Element")
        val id: String,
        @Description("A short human-readable label (max ~8 words) ONLY for elements that have neither a name nor any labeled flows, generated from the element's type and surrounding context, e.g. \"Gateway: patient is a minor?\" or \"Message event: send record to insurer\". Leave null for elements that have a name or labeled flows – their display name is already known or derived from the flow labels.")
        val label: String? = null,
        @Description("The detailed reason why the BPMN Element is relevant for GDPR compliance and why you think personal data is processed, transmitted, stored or used in a decision.")
        val reason: String,
        @Description("Indicates whether the BPMN Element is relevant for GDPR compliance")
        val isRelevant: Boolean = true,
        @Description("Verbatim text passages from the retrieved legal excerpts that directly supported this classification. Only populated when RAG context was provided.")
        val references: List<Reference> = emptyList()
    ) {
        @Description("A verbatim citation from a legal document retrieved via RAG that supported the GDPR classification decision.")
        data class Reference(
            @Description("The exact text passage copied verbatim from the Supporting Legal Excerpts section — not paraphrased or reformulated.")
            val exactText: String,
            @Description("The source document identifier from the [Source: ...] label preceding the excerpt, or null if not available.")
            val sourceDocument: String? = null
        )
    }

    /**
     * Resolves (possibly) incomplete element IDs to existing full IDs from the provided BPMN elements.
     * Considers all structural BPMN elements (activities, events, gateways, data objects/stores), not just tasks.
     * Also removes duplicates after resolution and removes elements that do not exist in the provided BPMN elements.
     */
    fun resolveActivities(actualBpmnElements: Set<BpmnElement>): BpmnAnalysisResult {
        val existingElementIds = actualBpmnElements
            .filterNot { it.type == "textAnnotation" }
            .map { it.id }.toSet()

        val resolvedDistinct = elements.mapNotNull { element ->
            val resolvedId = resolveElementIdUniquely(element.id, existingElementIds)
            resolvedId?.let { if (it == element.id) element else element.copy(id = it) }
        }.distinctBy { it.id }

        return BpmnAnalysisResult(elements = resolvedDistinct)
    }

    /**
     * Attempts to uniquely map a potentially incomplete ID to an existing full ID.
     * First, prefix match (startsWith)
     * If no matches: substring match (contains)
     * Only unique matches will be completed, otherwise null
     */
    private fun resolveElementIdUniquely(partialId: String, existingElementIds: Set<String>): String? {
        if (partialId in existingElementIds) return partialId

        val prefixMatches = existingElementIds.filter { it.startsWith(partialId) }
        prefixMatches.singleOrNull()?.let {
            log.warn { "LLM element ID '$partialId' is not an exact match; resolved via unique prefix match to '$it'" }
            return it
        }

        val substringMatch = existingElementIds.filter { it.contains(partialId) }.singleOrNull()
        if (substringMatch != null) {
            log.warn { "LLM element ID '$partialId' is not an exact match; resolved via unique substring match to '$substringMatch'" }
        }
        return substringMatch
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}