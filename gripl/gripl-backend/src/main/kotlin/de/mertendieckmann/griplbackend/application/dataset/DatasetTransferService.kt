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
import de.mertendieckmann.griplbackend.repository.EvaluationDataLabelRepository
import de.mertendieckmann.griplbackend.repository.LabelRepository

class DatasetImportException(message: String) : RuntimeException(message)

data class ImportedDataset(
    val originalId: String,
    val id: Long,
    val name: String,
    val testCaseCount: Int
)

/**
 * Exports and imports datasets as Postgres-compatible CSV files.
 *
 * Every ZIP contains dataset.csv and evaluation_data.csv. Newer exports also
 * contain labels.csv and evaluation_data_label.csv so custom label definitions
 * and their BPMN element assignments can be restored on another installation.
 * Older ZIP files without the two optional label files remain importable.
 *
 * Files from before multiclass labels, which have no "classification" field in
 * expected_values, are also supported and receive an empty class list.
 */
@Service
class DatasetTransferService(
    private val datasetRepository: DatasetRepository,
    private val evaluationDataRepository: EvaluationDataRepository,
    private val labelRepository: LabelRepository,
    private val evaluationDataLabelRepository: EvaluationDataLabelRepository,
    transactionManager: PlatformTransactionManager
) {
    private val log = KotlinLogging.logger { }
    private val transactionTemplate = TransactionTemplate(transactionManager)
    private val objectMapper = jacksonObjectMapper()

    companion object {
        const val DATASET_FILE = "dataset.csv"
        const val EVALUATION_DATA_FILE = "evaluation_data.csv"
        const val LABEL_FILE = "labels.csv"
        const val EVALUATION_DATA_LABEL_FILE = "evaluation_data_label.csv"
        private val DATASET_COLUMNS = listOf("id", "name", "description", "created_at", "updated_at")
        private val EVALUATION_DATA_COLUMNS =
            listOf("id", "bpmn_xml", "expected_values", "created_at", "updated_at", "name", "dataset_id")
        private val LABEL_COLUMNS =
            listOf("id", "name", "description", "predefined")
        private val EVALUATION_DATA_LABEL_COLUMNS =
            listOf("evaluation_data_id", "element_id", "label_id")
    }

    /**
     * Only datasets owned by [ownerUserId] are ever exported: ids that belong to someone else are silently dropped.
     * @param datasetIds null exports all of the user's datasets
     */
    fun exportAsZip(ownerUserId: String, datasetIds: List<Long>?): ByteArray {
        val ownedIds = datasetRepository.getDatasetsByOwner(ownerUserId).map { it.id }
        val idsToExport = if (datasetIds == null) ownedIds else datasetIds.filter { it in ownedIds }
        val datasetCsv = PostgresCsv.write(
            DATASET_COLUMNS, datasetRepository.getDatasetExportRows(idsToExport, ownerUserId)
        )
        val evaluationDataCsv = PostgresCsv.write(
            EVALUATION_DATA_COLUMNS,
            evaluationDataRepository.getEvaluationDataExportRows(idsToExport, ownerUserId)
        )
        val labelsCsv = PostgresCsv.write(
            LABEL_COLUMNS,
            labelRepository.getExportRowsForDatasetIds(
                idsToExport,
                ownerUserId
            )
        )

        val evaluationDataLabelCsv = PostgresCsv.write(
            EVALUATION_DATA_LABEL_COLUMNS,
            evaluationDataLabelRepository.getExportRowsForDatasetIds(
                idsToExport,
                ownerUserId
            )
        )

        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry(DATASET_FILE))
            zip.write(datasetCsv.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(EVALUATION_DATA_FILE))
            zip.write(evaluationDataCsv.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(LABEL_FILE))
            zip.write(labelsCsv.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(EVALUATION_DATA_LABEL_FILE))
            zip.write(evaluationDataLabelCsv.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return bytes.toByteArray()
    }

    /**
    * Reads the required dataset.csv and evaluation_data.csv files from a ZIP.
    *
    * labels.csv and evaluation_data_label.csv are optional together and preserve
    * custom label assignments in newer exports.
    */
    fun importZip(zipBytes: ByteArray, ownerUserId: String): List<ImportedDataset> {
        val files = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
                val fileName = entry.name.substringAfterLast('/').lowercase()
                if (
                    fileName == DATASET_FILE ||
                    fileName == EVALUATION_DATA_FILE ||
                    fileName == LABEL_FILE ||
                    fileName == EVALUATION_DATA_LABEL_FILE
                ) {
                    files[fileName] = zip.readBytes().toString(Charsets.UTF_8)
                }
            }
        }
        val datasetCsv = files[DATASET_FILE]
            ?: throw DatasetImportException("ZIP does not contain $DATASET_FILE")
        val evaluationDataCsv = files[EVALUATION_DATA_FILE]
            ?: throw DatasetImportException("ZIP does not contain $EVALUATION_DATA_FILE")
        return importCsv(
            datasetCsv = datasetCsv,
            evaluationDataCsv = evaluationDataCsv,
            ownerUserId = ownerUserId,
            labelsCsv = files[LABEL_FILE],
            evaluationDataLabelCsv = files[EVALUATION_DATA_LABEL_FILE]
        )
    }

    /**
     * Always creates new datasets and test cases (new ids) owned by [ownerUserId]; dataset_id references are remapped.
     * Runs in a single transaction: either everything is imported or nothing.
     */
    fun importCsv(
        datasetCsv: String,
        evaluationDataCsv: String,
        ownerUserId: String,
        labelsCsv: String? = null,
        evaluationDataLabelCsv: String? = null
    ): List<ImportedDataset> {
        val datasetRows = PostgresCsv.read(datasetCsv)
        val evaluationRows = PostgresCsv.read(evaluationDataCsv)

        val labelRows = labelsCsv?.let { PostgresCsv.read(it) }
        val evaluationDataLabelRows =
            evaluationDataLabelCsv?.let { PostgresCsv.read(it) }

        if ((labelRows == null) != (evaluationDataLabelRows == null)) {
            throw DatasetImportException(
                "$LABEL_FILE and $EVALUATION_DATA_LABEL_FILE must be supplied together."
            )
        }

        if (labelRows != null && evaluationDataLabelRows != null) {
            requireColumns(
                LABEL_FILE,
                labelRows,
                listOf("id", "name", "description", "predefined")
            )
            requireColumns(
                EVALUATION_DATA_LABEL_FILE,
                evaluationDataLabelRows,
                listOf("evaluation_data_id", "element_id", "label_id")
            )
        }

        val importedLabels = labelRows
            ?.let { parseImportedLabels(it) }
            .orEmpty()

        val importedAssignments = evaluationDataLabelRows
            ?.let { parseImportedAssignments(it) }
            .orEmpty()

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
                originalEvaluationDataId =
                    row["id"]?.trim()?.takeIf { it.isNotEmpty() }
                        ?: if (evaluationDataLabelRows != null) {
                            throw DatasetImportException(
                                "$EVALUATION_DATA_FILE row $line: id is missing"
                            )
                        } else {
                            null
                        },
                name = row["name"],
                bpmnXml = bpmnXml,
                expectedValues = parseExpectedValues(row["expected_values"], line),
                createdAt = row["created_at"]?.takeIf { it.isNotBlank() },
                updatedAt = row["updated_at"]?.takeIf { it.isNotBlank() }
            )
        }
        if (evaluationDataLabelRows != null) {
            val labelsByOriginalId = importedLabels.associateBy {
                it.originalId
            }

            val testCasesByOriginalId = testCases
                .mapNotNull { testCase ->
                    testCase.originalEvaluationDataId?.let { id ->
                        id to testCase
                    }
                }
                .toMap()

            if (testCasesByOriginalId.size != testCases.size) {
                throw DatasetImportException(
                    "$EVALUATION_DATA_FILE contains duplicate test case ids"
                )
            }

            importedAssignments.forEach { assignment ->
                if (assignment.originalLabelId !in labelsByOriginalId) {
                    throw DatasetImportException(
                        "$EVALUATION_DATA_LABEL_FILE references unknown label id " +
                            assignment.originalLabelId
                    )
                }

                val testCase = testCasesByOriginalId[
                    assignment.originalEvaluationDataId
                ] ?: throw DatasetImportException(
                    "$EVALUATION_DATA_LABEL_FILE references unknown test case id " +
                        assignment.originalEvaluationDataId
                )

                if (assignment.elementId !in testCase.expectedValues.map { it.value }) {
                    throw DatasetImportException(
                        "$EVALUATION_DATA_LABEL_FILE references unknown element id " +
                            assignment.elementId
                    )
                }
            }
        }

        // Exported IDs belong to the source database and must be resolved locally.
        
        return transactionTemplate.execute {
            val targetLabelIdsByOriginalId = importedLabels.associate { label ->
                label.originalId to resolveImportedLabel(label, ownerUserId)
            }

            val importedLabelIdsByTestCase = importedAssignments
                .groupBy { it.originalEvaluationDataId }
                .mapValues { (_, assignments) ->
                    assignments
                        .groupBy { it.elementId }
                        .mapValues { (_, elementAssignments) ->
                            elementAssignments
                                .map { assignment ->
                                    targetLabelIdsByOriginalId.getValue(
                                        assignment.originalLabelId
                                    )
                                }
                                .distinct()
                        }
                }
            // Names already in use by this user (case-insensitive), incl. datasets created earlier in this import
            val takenNames = datasetRepository.getDatasetsByOwner(ownerUserId).map { it.name.trim().lowercase() }.toMutableSet()

            datasetRows.zip(datasetIdsInFile).mapIndexed { index, (row, originalId) ->
                val originalName = row["name"]?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw DatasetImportException("$DATASET_FILE row ${index + 2}: name is missing")
                val name = uniqueImportName(originalName, takenNames)
                takenNames.add(name.lowercase())
                val newId = datasetRepository.insertDatasetReturningId(
                    name = name,
                    description = row["description"],
                    createdAt = row["created_at"]?.takeIf { it.isNotBlank() },
                    updatedAt = row["updated_at"]?.takeIf { it.isNotBlank() },
                    ownerUserId = ownerUserId
                )
                val testCasesOfDataset = testCases.filter { it.originalDatasetId == originalId }
                testCasesOfDataset.forEach { testCase ->
                    val importedEvaluationDataId =
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

                    val labelIdsByElement = testCase.originalEvaluationDataId
                        ?.let { importedLabelIdsByTestCase[it] }
                        .orEmpty()

                    evaluationDataLabelRepository.addImportedAssignments(
                        importedEvaluationDataId,
                        labelIdsByElement
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

    /**
    * Resolves one label from an export to a label in the current database.
    *
    * Existing labels with the same name are reused. Unknown custom labels are
    * created, while missing predefined labels indicate an invalid target setup.
    */
    private fun resolveImportedLabel(
        importedLabel: ImportedLabel,
        ownerUserId: String
    ): Long {
        val existingLabel = labelRepository.findByNameIncludingArchived(
            importedLabel.name
        )

        if (existingLabel != null) {
            if (existingLabel.predefined != importedLabel.predefined) {
                throw DatasetImportException(
                    "Imported label '${importedLabel.name}' conflicts with " +
                        "an existing label of a different type."
                )
            }

            return existingLabel.id
        }

        if (importedLabel.predefined) {
            throw DatasetImportException(
                "Required predefined label '${importedLabel.name}' is missing."
            )
        }

        return labelRepository.create(
            name = importedLabel.name,
            description = importedLabel.description,
            userId = ownerUserId
        ).id
    }

    private fun parseImportedLabels(
        rows: List<Map<String, String?>>
    ): List<ImportedLabel> {
        val labels = rows.mapIndexed { index, row ->
            val line = index + 2

            ImportedLabel(
                originalId = row["id"]?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw DatasetImportException("$LABEL_FILE row $line: id is missing"),
                name = row["name"]?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw DatasetImportException("$LABEL_FILE row $line: name is missing"),
                description = row["description"]?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw DatasetImportException(
                        "$LABEL_FILE row $line: description is missing"
                    ),
                predefined = row["predefined"]
                    ?.trim()
                    ?.toBooleanStrictOrNull()
                    ?: throw DatasetImportException(
                        "$LABEL_FILE row $line: predefined must be true or false"
                    )
            )
        }

        labels
            .groupingBy { it.originalId }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .firstOrNull()
            ?.let { duplicateId ->
                throw DatasetImportException(
                    "$LABEL_FILE: id $duplicateId occurs more than once"
                )
            }

        return labels
    }

    private fun parseImportedAssignments(
        rows: List<Map<String, String?>>
    ): List<ImportedLabelAssignment> {
        return rows.mapIndexed { index, row ->
            val line = index + 2

            ImportedLabelAssignment(
                originalEvaluationDataId =
                    row["evaluation_data_id"]?.trim()?.takeIf { it.isNotEmpty() }
                        ?: throw DatasetImportException(
                            "$EVALUATION_DATA_LABEL_FILE row $line: evaluation_data_id is missing"
                        ),
                elementId = row["element_id"]?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw DatasetImportException(
                        "$EVALUATION_DATA_LABEL_FILE row $line: element_id is missing"
                    ),
                originalLabelId =
                    row["label_id"]?.trim()?.takeIf { it.isNotEmpty() }
                        ?: throw DatasetImportException(
                            "$EVALUATION_DATA_LABEL_FILE row $line: label_id is missing"
                        )
            )
        }
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

    private data class ImportedLabel(
        val originalId: String,
        val name: String,
        val description: String,
        val predefined: Boolean
    )

    private data class ImportedLabelAssignment(
        val originalEvaluationDataId: String,
        val elementId: String,
        val originalLabelId: String
    )

    private data class ParsedTestCase(
        val originalDatasetId: String,
        val originalEvaluationDataId: String?,
        val name: String?,
        val bpmnXml: String,
        val expectedValues: List<ExpectedValue>,
        val createdAt: String?,
        val updatedAt: String?
    )
}
