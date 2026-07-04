package de.mertendieckmann.griplbackend.model.dto


import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import de.mertendieckmann.griplbackend.model.analysis.GdprProcessingClass

data class EvaluationData(
    val id: Long,
    val name: String?,
    val bpmnXml: String,
    val expectedValues: List<ExpectedValue>,
    val datasetId: Long? = null
) {
    companion object {
        private val mapper = jacksonObjectMapper()

        fun fromRow(id: Long, name: String?, bpmnXml: String, expectedJson: String, datasetId: Long?): EvaluationData {
            val list: List<ExpectedValue> = mapper.readValue(expectedJson)
            return EvaluationData(
                id = id,
                name = name,
                bpmnXml = bpmnXml,
                expectedValues = list,
                datasetId = datasetId
            )
        }
    }
}

data class ExpectedValue(
    val value: String,
    val reason: String? = null,
    val classification: List<GdprProcessingClass> = emptyList()
)

data class EvaluationDataMeta(
    val id: Long,
    val name: String?,
    val datasetId: Long?
)

data class EvaluationDataWithOptionalId(
    val id: Long? = null,
    val name: String? = null,
    val bpmnXml: String,
    val expectedValues: List<ExpectedValue>,
    val datasetId: Long? = null
)