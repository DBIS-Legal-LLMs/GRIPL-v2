package de.mertendieckmann.griplbackend.model.dto


import com.fasterxml.jackson.annotation.JsonAlias
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
    /**
     * Free-text gold-standard explanation for why this element carries the given
     * [classification], authored by a researcher during labeling. Also reused (via
     * [Evaluator][de.mertendieckmann.griplbackend.evaluation.service.Evaluator]
     * implementations) to carry the LLM's own explanation for its actual output when
     * this type represents an evaluation result rather than the gold standard.
     *
     * [JsonAlias] accepts the legacy "reason" JSON key so existing dataset rows
     * stored before this field was renamed continue to deserialize correctly.
     */
    @JsonAlias("reason")
    val explanation: String? = null,
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
