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
import de.mertendieckmann.griplbackend.model.dto.EvaluationDataWithOptionalId
import de.mertendieckmann.griplbackend.model.dto.ExpectedValue
import org.junit.jupiter.api.Assertions.assertFalse

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
    @Test
    fun `saving and updating a test case persists explicitly supplied label ids`() {
        val datasetId = jdbc.queryForObject(
            """
            INSERT INTO dataset (name, description, owner_user_id)
            VALUES (?, ?, ?)
            RETURNING id
            """.trimIndent(),
            Long::class.java,
            "Saved label test dataset",
            "Used only by EvaluationDataLabelRepositoryTest",
            "researcher-1"
        )!!

        val predefinedCollection =
            labelRepository.findAllPredefined().single { it.name == "COLLECTION" }

        val firstCustomLabel = labelRepository.create(
            name = "First saved custom label",
            description = "Assigned when the test case is created",
            userId = "researcher-1"
        )

        val secondCustomLabel = labelRepository.create(
            name = "Second saved custom label",
            description = "Assigned when the test case is updated",
            userId = "researcher-1"
        )

        val evaluationDataId = evaluationDataRepository.insertEvaluationData(
            EvaluationDataWithOptionalId(
                name = "Saved label test case",
                bpmnXml = "<definitions />",
                datasetId = datasetId,
                expectedValues = listOf(
                    ExpectedValue(
                        value = "Activity_A",
                        explanation = "Collection of personal data",
                        classification = listOf(GdprProcessingClass.COLLECTION),
                        labelIds = listOf(
                            predefinedCollection.id,
                            firstCustomLabel.id
                        )
                    )
                )
            )
        )!!.toLong()

        val created = evaluationDataRepository
            .getEvaluationDataById(evaluationDataId)!!

        assertEquals(
            setOf("COLLECTION", "First saved custom label"),
            created.expectedValues.single().labels.map { it.name }.toSet()
        )

        val legacyStyleUpdate = created.copy(
            name = "Updated by an older client"
        )

        assertEquals(
            1,
            evaluationDataRepository.updateEvaluationData(
                legacyStyleUpdate,
                "researcher-1"
            )
        )

        val loadedAfterLegacyUpdate = evaluationDataRepository
            .getEvaluationDataById(evaluationDataId)!!

        assertEquals(
            setOf("COLLECTION", "First saved custom label"),
            loadedAfterLegacyUpdate.expectedValues
                .single()
                .labels
                .map { it.name }
                .toSet()
        )

        val updated = loadedAfterLegacyUpdate.copy(
            expectedValues = created.expectedValues.map { expectedValue ->
                expectedValue.copy(
                    labelIds = listOf(
                        predefinedCollection.id,
                        secondCustomLabel.id
                    )
                )
            }
        )

        assertEquals(
            1,
            evaluationDataRepository.updateEvaluationData(updated, "researcher-1")
        )

        val loadedAfterUpdate = evaluationDataRepository
            .getEvaluationDataById(evaluationDataId)!!

        assertEquals(
            setOf("COLLECTION", "Second saved custom label"),
            loadedAfterUpdate.expectedValues.single().labels.map { it.name }.toSet()
        )

        val storedJson = jdbc.queryForObject(
            "SELECT expected_values::text FROM evaluation_data WHERE id = ?",
            String::class.java,
            evaluationDataId
        )!!

        assertFalse(storedJson.contains("\"labels\""))
        assertFalse(storedJson.contains("\"labelIds\""))
    }
    @Test
    fun `importing a legacy test case creates predefined label assignments`() {
        val datasetId = jdbc.queryForObject(
            """
            INSERT INTO dataset (name, description, owner_user_id)
            VALUES (?, ?, ?)
            RETURNING id
            """.trimIndent(),
            Long::class.java,
            "Imported legacy label dataset",
            "Used only by EvaluationDataLabelRepositoryTest",
            "researcher-1"
        )!!

        val evaluationDataId = evaluationDataRepository.insertImportedEvaluationData(
            EvaluationDataWithOptionalId(
                name = "Imported legacy label test case",
                bpmnXml = "<definitions />",
                datasetId = datasetId,
                expectedValues = listOf(
                    ExpectedValue(
                        value = "Activity_A",
                        explanation = "Imported from an older CSV file",
                        classification = listOf(GdprProcessingClass.COLLECTION)
                    )
                )
            ),
            createdAt = null,
            updatedAt = null
        )

        val loaded = evaluationDataRepository
            .getEvaluationDataById(evaluationDataId)!!

        val expectedValue = loaded.expectedValues.single()

        assertEquals(
            listOf(GdprProcessingClass.COLLECTION),
            expectedValue.classification
        )
        assertEquals(
            listOf("COLLECTION"),
            expectedValue.labels.map { it.name }
        )
    }
}