package de.mertendieckmann.griplbackend.model.dto

import de.mertendieckmann.griplbackend.model.analysis.GdprProcessingClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

/**
 * Covers the gold-standard explanation field on [ExpectedValue] (issue #29): every labeled
 * element must be able to carry a free-text explanation alongside its classification, and
 * rows persisted before the field was renamed from "reason" to "explanation" must still load.
 */
class EvaluationDataTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `fromRow deserializes the explanation field from the current JSON shape`() {
        val expectedJson = """
            [{"value":"Activity_1","classification":["COLLECTION"],"explanation":"Collects patient data"}]
        """.trimIndent()

        val evaluationData = EvaluationData.fromRow(
            id = 1L,
            name = "Test Case",
            bpmnXml = "<xml/>",
            expectedJson = expectedJson,
            datasetId = 1L
        )

        assertEquals(1, evaluationData.expectedValues.size)
        val value = evaluationData.expectedValues.first()
        assertEquals("Activity_1", value.value)
        assertEquals("Collects patient data", value.explanation)
        assertEquals(listOf(GdprProcessingClass.COLLECTION), value.classification)
    }

    @Test
    fun `fromRow is backward compatible with rows persisted under the legacy reason key`() {
        val legacyJson = """
            [{"value":"Activity_2","classification":[],"reason":"Legacy gold standard explanation"}]
        """.trimIndent()

        val evaluationData = EvaluationData.fromRow(
            id = 2L,
            name = "Legacy Test Case",
            bpmnXml = "<xml/>",
            expectedJson = legacyJson,
            datasetId = null
        )

        val value = evaluationData.expectedValues.single()
        assertEquals("Legacy gold standard explanation", value.explanation)
    }

    @Test
    fun `explanation is optional and defaults to null`() {
        val jsonWithoutExplanation = """[{"value":"Activity_3","classification":[]}]"""

        val values: List<ExpectedValue> = mapper.readValue(jsonWithoutExplanation)

        assertNull(values.single().explanation)
    }

    @Test
    fun `explanation round-trips through serialization using the new field name`() {
        val original = ExpectedValue(
            value = "Activity_4",
            explanation = "Stores personal data in the CRM",
            classification = emptyList()
        )

        val json = mapper.writeValueAsString(listOf(original))
        val roundTripped: List<ExpectedValue> = mapper.readValue(json)

        assertEquals(original.explanation, roundTripped.single().explanation)
        assertTrue(json.contains("\"explanation\""), "Serialized JSON should use the new field name: $json")
    }
}
