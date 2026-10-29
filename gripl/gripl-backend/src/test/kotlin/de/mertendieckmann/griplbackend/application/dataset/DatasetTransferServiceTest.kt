package de.mertendieckmann.griplbackend.application.dataset

import de.mertendieckmann.griplbackend.model.analysis.GdprProcessingClass
import de.mertendieckmann.griplbackend.model.dto.EvaluationDataWithOptionalId
import de.mertendieckmann.griplbackend.model.dto.ExpectedValue
import de.mertendieckmann.griplbackend.repository.EvaluationDataRepository
import de.mertendieckmann.griplbackend.repository.LabelRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * Verifies that custom labels and their BPMN element assignments survive
 * a complete dataset ZIP export and import.
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
class DatasetTransferServiceTest(
    @Autowired private val datasetTransferService: DatasetTransferService,
    @Autowired private val evaluationDataRepository: EvaluationDataRepository,
    @Autowired private val labelRepository: LabelRepository,
    @Autowired private val jdbc: JdbcTemplate
) {

    @Test
    fun `export and import preserve custom label assignments`() {
        val ownerUserId = "researcher-1"

        val datasetId = jdbc.queryForObject(
            """
            INSERT INTO dataset (name, description, owner_user_id)
            VALUES (?, ?, ?)
            RETURNING id
            """.trimIndent(),
            Long::class.java,
            "Custom label export dataset",
            "Used only by DatasetTransferServiceTest",
            ownerUserId
        )!!

        val predefinedCollection =
            labelRepository.findAllPredefined().single {
                it.name == "COLLECTION"
            }

        val customLabel = labelRepository.create(
            name = "Exported custom label",
            description = "Must survive dataset transfer",
            userId = ownerUserId
        )

        evaluationDataRepository.insertEvaluationData(
            EvaluationDataWithOptionalId(
                name = "Custom label export test case",
                bpmnXml = "<definitions />",
                datasetId = datasetId,
                expectedValues = listOf(
                    ExpectedValue(
                        value = "Activity_A",
                        classification = listOf(
                            GdprProcessingClass.COLLECTION
                        ),
                        labelIds = listOf(
                            predefinedCollection.id,
                            customLabel.id
                        )
                    )
                )
            )
        )

        val zipBytes = datasetTransferService.exportAsZip(
            ownerUserId,
            listOf(datasetId)
        )

        assertEquals(
            setOf(
                DatasetTransferService.DATASET_FILE,
                DatasetTransferService.EVALUATION_DATA_FILE,
                DatasetTransferService.LABEL_FILE,
                DatasetTransferService.EVALUATION_DATA_LABEL_FILE
            ),
            zipEntryNames(zipBytes)
        )

        val importedDataset = datasetTransferService
            .importZip(zipBytes, ownerUserId)
            .single()

        val importedTestCaseId = jdbc.queryForObject(
            """
            SELECT id
            FROM evaluation_data
            WHERE dataset_id = ?
            """.trimIndent(),
            Long::class.java,
            importedDataset.id
        )!!

        val importedTestCase = evaluationDataRepository
            .getEvaluationDataById(importedTestCaseId)!!

        val importedExpectedValue =
            importedTestCase.expectedValues.single()

        assertEquals(
            listOf(GdprProcessingClass.COLLECTION),
            importedExpectedValue.classification
        )
        assertEquals(
            setOf("COLLECTION", "Exported custom label"),
            importedExpectedValue.labels.map { it.name }.toSet()
        )
    }

    private fun zipEntryNames(zipBytes: ByteArray): Set<String> =
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            buildSet {
                generateSequence { zip.nextEntry }
                    .forEach { entry -> add(entry.name) }
            }
        }
}