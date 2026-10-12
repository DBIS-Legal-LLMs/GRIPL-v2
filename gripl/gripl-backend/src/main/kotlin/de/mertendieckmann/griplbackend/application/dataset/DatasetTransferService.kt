package de.mertendieckmann.griplbackend.application.dataset

import de.mertendieckmann.griplbackend.model.dto.EvaluationDataWithOptionalId
import de.mertendieckmann.griplbackend.model.dto.ExpectedValue
import de.mertendieckmann.griplbackend.repository.DatasetRepository
import de.mertendieckmann.griplbackend.repository.EvaluationDataRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DatasetImportException(message: String) : RuntimeException(message)

data class ImportedDataset(
    val originalId: String,
    val id: Long,
    val name: String,
    val testCaseCount: Int
)

/**
 * Exports and imports datasets incl. labels as the two CSV files of the original Postgres export
 * (dataset.csv and evaluation_data.csv). Files from the version before multiclass labels (no
 * "classification" in expected_values) can be imported; their labels get an empty class list.
 */
@Service
class DatasetTransferService(
    private val datasetRepository: DatasetRepository,
    private val evaluationDataRepository: EvaluationDataRepository,
    transactionManager: PlatformTransactionManager
) {
    private val log = KotlinLogging.logger { }
    private val transactionTemplate = TransactionTemplate(transactionManager)
    private val objectMapper = jacksonObjectMapper()

    companion object {
        const val DATASET_FILE = "dataset.csv"
        const val EVALUATION_DATA_FILE = "evaluation_data.csv"
        private val DATASET_COLUMNS = listOf("id", "name", "description", "created_at", "updated_at")
        private val EVALUATION_DATA_COLUMNS =
            listOf("id", "bpmn_xml", "expected_values", "created_at", "updated_at", "name", "dataset_id")
    }

    /** @param datasetIds null exports all datasets */
    fun exportAsZip(datasetIds: List<Long>?): ByteArray {
        val datasetCsv = PostgresCsv.write(DATASET_COLUMNS, datasetRepository.getDatasetExportRows(datasetIds))
        val evaluationDataCsv = PostgresCsv.write(
            EVALUATION_DATA_COLUMNS,
            evaluationDataRepository.getEvaluationDataExportRows(datasetIds)
        )

        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry(DATASET_FILE))
            zip.write(datasetCsv.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(EVALUATION_DATA_FILE))
            zip.write(evaluationDataCsv.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return bytes.toByteArray()
    }

    /** Reads dataset.csv and evaluation_data.csv from a ZIP (file names may be inside folders). */
    fun importZip(zipBytes: ByteArray): List<ImportedDataset> {
        val files = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
                val fileName = entry.name.substringAfterLast('/').lowercase()
                if (fileName == DATASET_FILE || fileName == EVALUATION_DATA_FILE) {
                    files[fileName] = zip.readBytes().toString(Charsets.UTF_8)
                }
            }
        }
        val datasetCsv = files[DATASET_FILE]
            ?: throw DatasetImportException("ZIP does not contain $DATASET_FILE")
        val evaluationDataCsv = files[EVALUATION_DATA_FILE]
            ?: throw DatasetImportException("ZIP does not contain $EVALUATION_DATA_FILE")
        return importCsv(datasetCsv, evaluationDataCsv)
    }

    /**
     * Always creates new datasets and test cases (new ids); dataset_id references are remapped.
     * Runs in a single transaction: either everything is imported or nothing.
     */
    fun importCsv(datasetCsv: String, evaluationDataCsv: String): List<ImportedDataset> {
        val datasetRows = PostgresCsv.read(datasetCsv)
        val evaluationRows = PostgresCsv.read(evaluationDataCsv)

        if (datasetRows.isEmpty()) throw DatasetImportException("$DATASET_FILE contains no datasets")
        requireColumns(DATASET_FILE, datasetRows, listOf("id", "name"))
        requireColumns(EVALUATION_DATA_FILE, evaluationRows, listOf("bpmn_xml", "expected_values", "dataset_id"))

        val datasetIdsInFile = datasetRows.mapIndexed { index, row ->
            row["id"]?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw DatasetImportException("$DATASET_FILE row ${index + 2}: id is missing")
        }
        datasetIdsInFile.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.firstOrNull()?.let {
            throw DatasetImportException("$DATASET_FILE: id $it occurs more than once")
        }

        // Validate all test cases before writing anything
        val testCases = evaluationRows.mapIndexed { index, row ->
            val line = index + 2
            val datasetId = row["dataset_id"]?.trim()
            if (datasetId.isNullOrEmpty()) {
                throw DatasetImportException("$EVALUATION_DATA_FILE row $line: test case has no dataset_id")
            }
            if (datasetId !in datasetIdsInFile) {
                throw DatasetImportException(
                    "$EVALUATION_DATA_FILE row $line: dataset_id $datasetId does not exist in $DATASET_FILE"
                )
            }
            val bpmnXml = row["bpmn_xml"]?.takeIf { it.isNotBlank() }
                ?: throw DatasetImportException("$EVALUATION_DATA_FILE row $line: bpmn_xml is empty")
            ParsedTestCase(
                originalDatasetId = datasetId,
                name = row["name"],
                bpmnXml = bpmnXml,
                expectedValues = parseExpectedValues(row["expected_values"], line),
                createdAt = row["created_at"]?.takeIf { it.isNotBlank() },
                updatedAt = row["updated_at"]?.takeIf { it.isNotBlank() }
            )
        }

        return transactionTemplate.execute {
            // Names already in use (case-insensitive), incl. datasets created earlier in this import
            val takenNames = datasetRepository.getAllDatasets().map { it.name.trim().lowercase() }.toMutableSet()

            datasetRows.zip(datasetIdsInFile).mapIndexed { index, (row, originalId) ->
                val originalName = row["name"]?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw DatasetImportException("$DATASET_FILE row ${index + 2}: name is missing")
                val name = uniqueImportName(originalName, takenNames)
                takenNames.add(name.lowercase())
                val newId = datasetRepository.insertDatasetReturningId(
                    name = name,
                    description = row["description"],
                    createdAt = row["created_at"]?.takeIf { it.isNotBlank() },
                    updatedAt = row["updated_at"]?.takeIf { it.isNotBlank() }
                )
                val testCasesOfDataset = testCases.filter { it.originalDatasetId == originalId }
                testCasesOfDataset.forEach { testCase ->
                    evaluationDataRepository.insertImportedEvaluationData(
                        EvaluationDataWithOptionalId(
                            name = testCase.name,
                            bpmnXml = testCase.bpmnXml,
                            expectedValues = testCase.expectedValues,
                            datasetId = newId
                        ),
                        createdAt = testCase.createdAt,
                        updatedAt = testCase.updatedAt
                    )
                }
                log.info { "Imported dataset '$name' (file id $originalId) as id $newId with ${testCasesOfDataset.size} test cases" }
                ImportedDataset(originalId, newId, name, testCasesOfDataset.size)
            }
        }!!
    }

    /** "Name" → "Name (imported)" → "Name (imported 2)" … if a dataset with that name already exists */
    private fun uniqueImportName(name: String, takenNames: Set<String>): String {
        if (name.lowercase() !in takenNames) return name
        return generateSequence(1) { it + 1 }
            .map { if (it == 1) "$name (imported)" else "$name (imported $it)" }
            .first { it.lowercase() !in takenNames }
    }

    private fun parseExpectedValues(json: String?, line: Int): List<ExpectedValue> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            objectMapper.readValue<List<ExpectedValue>>(json)
        } catch (ex: Exception) {
            throw DatasetImportException(
                "$EVALUATION_DATA_FILE row $line: expected_values could not be read (${ex.message?.lineSequence()?.firstOrNull()})"
            )
        }
    }

    private fun requireColumns(file: String, rows: List<Map<String, String?>>, columns: List<String>) {
        val present = rows.firstOrNull()?.keys ?: return
        val missing = columns.filterNot { it in present }
        if (missing.isNotEmpty()) {
            throw DatasetImportException("$file is missing the column(s): ${missing.joinToString()}")
        }
    }

    private data class ParsedTestCase(
        val originalDatasetId: String,
        val name: String?,
        val bpmnXml: String,
        val expectedValues: List<ExpectedValue>,
        val createdAt: String?,
        val updatedAt: String?
    )
}
