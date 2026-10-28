package de.mertendieckmann.griplbackend.repository

import de.mertendieckmann.griplbackend.model.analysis.GdprProcessingClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.transaction.annotation.Transactional

/**
 * Verifies that legacy JSON classifications and relational global labels are
 * returned together when a test case is loaded.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:postgresql://localhost:5432/gripl_labels_test",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres"
    ]
)
@Transactional
@Rollback
class EvaluationDataLabelRepositoryTest(
    @Autowired private val evaluationDataRepository: EvaluationDataRepository,
    @Autowired private val labelRepository: LabelRepository,
    @Autowired private val jdbc: JdbcTemplate
) {

    @Test
    fun `legacy classifications remain and assigned labels are added`() {
        val datasetId = jdbc.queryForObject(
            """
            INSERT INTO dataset (name, description, owner_user_id)
            VALUES (?, ?, ?)
            RETURNING id
            """.trimIndent(),
            Long::class.java,
            "Label relation test dataset",
            "Used only by EvaluationDataLabelRepositoryTest",
            "researcher-1"
        )!!

        val expectedValuesJson = """
            [
                {
                    "value": "Activity_A",
                    "explanation": "Legacy GDPR classification",
                    "classification": ["COLLECTION"]
                }
            ]
        """.trimIndent()

        val testCaseId = jdbc.queryForObject(
            """
            INSERT INTO evaluation_data (name, bpmn_xml, expected_values, dataset_id)
            VALUES (?, ?, ?::jsonb, ?)
            RETURNING id
            """.trimIndent(),
            Long::class.java,
            "Legacy label test case",
            "<definitions />",
            expectedValuesJson,
            datasetId
        )!!

        val predefinedCollection =
            labelRepository.findAllPredefined().single { it.name == "COLLECTION" }

        val customLabel = labelRepository.create(
            name = "Contract review",
            description = "Reviews personal data in contracts",
            userId = "researcher-1"
        )

        jdbc.update(
            """
            INSERT INTO evaluation_data_label (
                evaluation_data_id,
                element_id,
                label_id
            )
            VALUES (?, ?, ?)
            """.trimIndent(),
            testCaseId,
            "Activity_A",
            predefinedCollection.id
        )

        jdbc.update(
            """
            INSERT INTO evaluation_data_label (
                evaluation_data_id,
                element_id,
                label_id
            )
            VALUES (?, ?, ?)
            """.trimIndent(),
            testCaseId,
            "Activity_A",
            customLabel.id
        )

        val loaded = evaluationDataRepository.getEvaluationDataById(testCaseId)
        assertNotNull(loaded)

        val expectedValue = loaded!!.expectedValues.single()
        assertEquals(
            listOf(GdprProcessingClass.COLLECTION),
            expectedValue.classification
        )
        assertEquals(
            setOf("COLLECTION", "Contract review"),
            expectedValue.labels.map { it.name }.toSet()
        )
    }

    @Test
    fun `archived assigned labels remain visible in loaded test cases`() {
        val datasetId = jdbc.queryForObject(
            """
            INSERT INTO dataset (name, description, owner_user_id)
            VALUES (?, ?, ?)
            RETURNING id
            """.trimIndent(),
            Long::class.java,
            "Archived label test dataset",
            "Used only by EvaluationDataLabelRepositoryTest",
            "researcher-1"
        )!!

        val testCaseId = jdbc.queryForObject(
            """
            INSERT INTO evaluation_data (name, bpmn_xml, expected_values, dataset_id)
            VALUES (?, ?, ?::jsonb, ?)
            RETURNING id
            """.trimIndent(),
            Long::class.java,
            "Archived label test case",
            "<definitions />",
            """[{"value":"Activity_A","classification":[]}]""",
            datasetId
        )!!

        val archivedLabel = labelRepository.create(
            name = "Archived custom label",
            description = "Will be archived during this test",
            userId = "researcher-1"
        )

        jdbc.update(
            """
            INSERT INTO evaluation_data_label (
                evaluation_data_id,
                element_id,
                label_id
            )
            VALUES (?, ?, ?)
            """.trimIndent(),
            testCaseId,
            "Activity_A",
            archivedLabel.id
        )

        labelRepository.softDelete(archivedLabel.id, "admin-1")

        val loaded = evaluationDataRepository.getEvaluationDataById(testCaseId)
        val assignment = loaded!!
            .expectedValues
            .single()
            .labels
            .single()

        assertEquals(archivedLabel.id, assignment.id)
        assertNotNull(assignment.deletedAt)
        assertTrue(assignment.name.contains("Archived"))
    }
}