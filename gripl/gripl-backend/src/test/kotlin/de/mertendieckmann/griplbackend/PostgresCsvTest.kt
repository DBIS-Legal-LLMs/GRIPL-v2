package de.mertendieckmann.griplbackend

import de.mertendieckmann.griplbackend.application.dataset.PostgresCsv
import de.mertendieckmann.griplbackend.model.analysis.GdprProcessingClass
import de.mertendieckmann.griplbackend.model.dto.ExpectedValue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostgresCsvTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `round trip keeps quotes, line breaks and nulls`() {
        val rows = listOf(
            listOf("1", "<a b=\"c\">\nline</a>", "[{\"value\": \"x\", \"classification\": [\"STORAGE\"]}]", null),
            listOf("2", "", "[]", "4")
        )
        val parsed = PostgresCsv.read(PostgresCsv.write(listOf("id", "bpmn_xml", "expected_values", "dataset_id"), rows))

        assertEquals(2, parsed.size)
        assertEquals("<a b=\"c\">\nline</a>", parsed[0]["bpmn_xml"])
        assertNull(parsed[0]["dataset_id"])
        assertEquals("", parsed[1]["bpmn_xml"])
        val labels: List<ExpectedValue> = mapper.readValue(parsed[0]["expected_values"]!!)
        assertEquals(listOf(GdprProcessingClass.STORAGE), labels.single().classification)
    }

    @Test
    fun `legacy export without classification can be read`() {
        // Original Postgres export from the version before multiclass labels (repo root /dataset)
        val legacyFile = File("../../dataset/evaluation_data.csv")
        assumeTrue(legacyFile.exists())

        val rows = PostgresCsv.read(legacyFile.readText())
        assertTrue(rows.isNotEmpty())
        rows.forEach { row ->
            assertTrue(row["bpmn_xml"]!!.contains("definitions"))
            assertTrue(row["dataset_id"]!!.isNotBlank())
            val labels: List<ExpectedValue> = mapper.readValue(row["expected_values"]!!)
            assertTrue(labels.all { it.classification.isEmpty() })
        }
    }
}
