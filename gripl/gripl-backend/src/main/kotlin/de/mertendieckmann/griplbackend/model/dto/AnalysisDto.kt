package de.mertendieckmann.griplbackend.model.dto

import de.mertendieckmann.griplbackend.application.BpmnExtractor
import de.mertendieckmann.griplbackend.model.BpmnElement
import de.mertendieckmann.griplbackend.model.analysis.BpmnAnalysisResult
import de.mertendieckmann.griplbackend.model.analysis.BpmnMulticlassAnalysisResult
import de.mertendieckmann.griplbackend.model.analysis.GdprProcessingClass

data class AnalysisResponse(
    val criticalElements: List<CriticalElement>,
    val amountOfRetries: Int? = null
) {
    data class CriticalElement(
        val id: String,
        val name: String?,
        val reason: String
    )

    companion object {
        fun fromBpmnAnalysisResult(result: BpmnAnalysisResult, bpmnElements: Set<BpmnElement>, amountOfRetries: Int): AnalysisResponse {
            val elements = result.elements.map { element ->
                CriticalElement(
                    id = element.id,
                    name = bpmnElements.find { it.id == element.id }?.name,
                    reason = element.reason
                )
            }

            return AnalysisResponse(
                criticalElements = elements,
                amountOfRetries = amountOfRetries
            )
        }

        fun fromBpmnAnalysisResult(result: BpmnAnalysisResult, bpmnXml: String, amountOfRetries: Int): AnalysisResponse {
            val extractor = BpmnExtractor()
            val bpmnElements = extractor.extractBpmnElements(bpmnXml)
            return fromBpmnAnalysisResult(result, bpmnElements, amountOfRetries)
        }
    }
}

data class MulticlassAnalysisResponse(
    val classifiedElements: List<ClassifiedElement>,
    val amountOfRetries: Int? = null
) {
    data class ClassifiedElement(
        val id: String,
        val name: String?,
        val reason: String,
        val classification: GdprProcessingClass
    )

    companion object {
        fun fromBpmnMulticlassAnalysisResult(
            result: BpmnMulticlassAnalysisResult,
            bpmnElements: Set<BpmnElement>,
            amountOfRetries: Int
        ): MulticlassAnalysisResponse {
            val elements = result.elements.map { element ->
                ClassifiedElement(
                    id = element.id,
                    name = bpmnElements.find { it.id == element.id }?.name,
                    reason = element.reason,
                    classification = element.classification
                )
            }

            return MulticlassAnalysisResponse(
                classifiedElements = elements,
                amountOfRetries = amountOfRetries
            )
        }
    }
}