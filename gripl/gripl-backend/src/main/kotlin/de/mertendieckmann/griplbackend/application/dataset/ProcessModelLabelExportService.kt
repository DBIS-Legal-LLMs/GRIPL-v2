package de.mertendieckmann.griplbackend.application.dataset

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import de.mertendieckmann.griplbackend.model.analysis.GdprProcessingClass
import de.mertendieckmann.griplbackend.model.dto.ExpectedValue
import de.mertendieckmann.griplbackend.model.dto.ProcessModel
import de.mertendieckmann.griplbackend.repository.ProcessModelRepository
import io.camunda.zeebe.model.bpmn.Bpmn
import io.camunda.zeebe.model.bpmn.instance.BaseElement
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate

/**
 * Exports Process Analysis models as one dataset in the import format of the labeling tool
 * (see [DatasetTransferService]), with the LLM's analysis results as pre-filled labels.
 * Binary results become labels without a class, multiclass results keep their classes.
 */
@Service
class ProcessModelLabelExportService(
    private val repository: ProcessModelRepository,
    private val datasetTransferService: DatasetTransferService
) {
    private val log = KotlinLogging.logger { }
    private val objectMapper = jacksonObjectMapper()

    companion object {
        /** Marks explanations that were written by the LLM rather than a human labeler. */
        const val LLM_EXPLANATION_PREFIX = "[LLM] "

        /** Only links the test cases to the dataset inside the file; the import assigns new ids. */
        private const val DATASET_ID = "1"
    }

    /**
     * Only process models owned by [ownerUserId] are ever exported: ids that belong to someone else are silently dropped.
     * @param ids null exports all of the user's process models
     */
    fun exportAsZip(ownerUserId: String, ids: List<Long>?, date: LocalDate = LocalDate.now()): ByteArray {
        val (datasetRows, testCaseRows) = buildRows(ownerUserId, ids, date)
        return DatasetTransferService.writeZip(datasetRows, testCaseRows)
    }

    /**
     * Same content as [exportAsZip], but created directly as a labeling dataset owned by [ownerUserId].
     * Goes through the regular import on purpose, so the result is identical to downloading the ZIP
     * and importing it (incl. the "(imported)" suffix on name clashes).
     */
    fun createDataset(ownerUserId: String, ids: List<Long>?, date: LocalDate = LocalDate.now()): ImportedDataset {
        val (datasetRows, testCaseRows) = buildRows(ownerUserId, ids, date)
        return datasetTransferService.importCsv(
            PostgresCsv.write(DatasetTransferService.DATASET_COLUMNS, datasetRows),
            PostgresCsv.write(DatasetTransferService.EVALUATION_DATA_COLUMNS, testCaseRows),
            ownerUserId
        ).single()
    }

    /** Rows in the [DatasetTransferService.DATASET_COLUMNS] / [DatasetTransferService.EVALUATION_DATA_COLUMNS] layout. */
    private fun buildRows(
        ownerUserId: String,
        ids: List<Long>?,
        date: LocalDate
    ): Pair<List<List<String?>>, List<List<String?>>> {
        val ownedIds = if (ids == null) {
            repository.listByOwner(ownerUserId).map { it.id }
        } else {
            repository.filterOwnedIds(ids, ownerUserId)
        }
        val models = ownedIds.distinct().sorted().mapNotNull { repository.getByIdAndOwner(it, ownerUserId) }

        val datasetRow = listOf(
            DATASET_ID,
            "LLM pre-labels – Process Analysis $date",
            "Exported from ${models.size} process model(s) of the Process Analysis on $date. " +
                "All labels are unreviewed LLM analysis results; explanations starting with " +
                "\"${LLM_EXPLANATION_PREFIX.trim()}\" are the LLM's reasoning.",
            null,
            null
        )
        val testCaseRows = models.map { model ->
            listOf(
                model.id.toString(),
                model.bpmnXml,
                objectMapper.writeValueAsString(toExpectedValues(model)),
                null,
                null,
                model.name,
                DATASET_ID
            )
        }
        return listOf(datasetRow) to testCaseRows
    }

    /**
     * The model's analysis result as labels. Labels for elements that no longer exist in the
     * stored diagram (it was edited after the analysis) are dropped, as they could never be matched.
     */
    internal fun toExpectedValues(model: ProcessModel): List<ExpectedValue> {
        val resultJson = model.analysisResultJson ?: return emptyList()
        val result = try {
            objectMapper.readValue<StoredAnalysisResult>(resultJson)
        } catch (e: Exception) {
            log.warn(e) { "Analysis result of process model ${model.id} could not be read, exporting it without labels" }
            return emptyList()
        }
        val existingIds = elementIds(model)

        return (result.classifiedElements ?: result.criticalElements ?: emptyList())
            .filter { existingIds == null || it.id in existingIds }
            .distinctBy { it.id }
            .map { element ->
                ExpectedValue(
                    value = element.id,
                    explanation = element.reason?.takeIf { it.isNotBlank() }?.let { LLM_EXPLANATION_PREFIX + it },
                    classification = element.classification
                )
            }
    }

    /** Ids of all elements in the stored diagram, or null if it can't be parsed (then nothing is filtered). */
    private fun elementIds(model: ProcessModel): Set<String>? = try {
        Bpmn.readModelFromStream(model.bpmnXml.byteInputStream())
            .getModelElementsByType(BaseElement::class.java)
            .mapNotNull { it.id }
            .toSet()
    } catch (e: Exception) {
        log.warn(e) { "BPMN of process model ${model.id} could not be parsed, exporting its labels unfiltered" }
        null
    }

    /** Covers both result shapes: binary (`criticalElements`) and multiclass (`classifiedElements`). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class StoredAnalysisResult(
        val criticalElements: List<StoredElement>? = null,
        val classifiedElements: List<StoredElement>? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class StoredElement(
        val id: String,
        val reason: String? = null,
        val classification: List<GdprProcessingClass> = emptyList()
    )
}
