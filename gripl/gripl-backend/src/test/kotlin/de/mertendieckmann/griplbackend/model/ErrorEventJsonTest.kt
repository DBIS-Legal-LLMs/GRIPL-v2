package de.mertendieckmann.griplbackend.model

import de.mertendieckmann.griplbackend.model.dto.EvaluationReportError
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportFatal
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportModelError
import de.mertendieckmann.griplbackend.model.dto.ModelReportEnvelope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper

/** The shape of the error events in the NDJSON evaluation stream, which the frontend parses by `type`. */
class ErrorEventJsonTest {

    private val mapper = jacksonObjectMapper()

    private fun json(report: de.mertendieckmann.griplbackend.model.dto.EvaluationReport) =
        mapper.readTree(mapper.writeValueAsString(ModelReportEnvelope("Model 1", report, 2))).get("report")

    @Test
    fun `a test case error carries its code`() {
        val node = json(EvaluationReportError(7, 5, "case", "OpenRouter rejected the request: x", "LLM_REQUEST_REJECTED"))

        assertEquals("error", node.get("type").asString())
        assertEquals("LLM_REQUEST_REJECTED", node.get("code").asString())
        assertEquals("OpenRouter rejected the request: x", node.get("errorMessage").asString())
    }

    @Test
    fun `a model error names how many test cases failed`() {
        val node = json(EvaluationReportModelError(3, 3, "All 3 test cases failed: x", "LLM_AUTH_FAILED", "ab12cd34"))

        assertEquals("modelError", node.get("type").asString())
        assertEquals(3, node.get("failedTestCases").asInt())
        assertEquals("ab12cd34", node.get("requestId").asString())
    }

    @Test
    fun `a fatal event is typed fatal`() {
        val node = json(EvaluationReportFatal("The evaluation stopped", "INTERNAL_ERROR", "ab12cd34"))

        assertEquals("fatal", node.get("type").asString())
        assertEquals("INTERNAL_ERROR", node.get("code").asString())
    }
}
