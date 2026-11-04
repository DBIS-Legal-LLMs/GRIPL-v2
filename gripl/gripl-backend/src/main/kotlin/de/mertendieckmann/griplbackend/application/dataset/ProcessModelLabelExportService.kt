package de.mertendieckmann.griplbackend.application.dataset

import de.mertendieckmann.griplbackend.model.dto.AnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.ExpectedValue
import de.mertendieckmann.griplbackend.model.dto.MulticlassAnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.ProcessModel
import de.mertendieckmann.griplbackend.repository.ProcessModelRepository
import io.camunda.zeebe.model.bpmn.Bpmn
import io.camunda.zeebe.model.bpmn.instance.BaseElement
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.time.LocalDate

/**
 * What happened to the models' labels in an export, so the UI can tell the user instead of
 * labels going missing unnoticed. Model lists hold model names.
 */
data class LabelExportSummary(
    val exportedModelCount: Int,
    val labelCount: Int,
    /** No analysis result yet — exported without labels, as expected */
    val notAnalyzedModels: List<String>,
    /** Has an analysis result that could not be read — exported without labels */
    val unreadableModels: List<String>,
    /** Labels left out because their element was removed from the diagram after the analysis */
    val droppedLabelCount: Int
)

class LabelingZipExport(val zip: ByteArray, val summary: LabelExportSummary)

data class CreatedLabelingDataset(val dataset: ImportedDataset, val summary: LabelExportSummary)

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

    // Results are read with the response DTOs they were written from. Fields this export
    // doesn't need (and that may change independently, e.g. RAG context) are ignored.
    private val objectMapper = JsonMapper.builder()
        .addModule(kotlinModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()

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
    fun exportAsZip(ownerUserId: String, ids: List<Long>?, date: LocalDate = LocalDate.now()): LabelingZipExport {
        val export = buildExport(ownerUserId, ids, date)
        return LabelingZipExport(DatasetTransferService.writeZip(export.datasetRows, export.testCaseRows), export.summary)
    }

    /**
     * Same content as [exportAsZip], but created directly as a labeling dataset owned by [ownerUserId].
     * Goes through the regular import on purpose, so the result is identical to downloading the ZIP
     * and importing it (incl. the "(imported)" suffix on name clashes).
     */
    fun createDataset(ownerUserId: String, ids: List<Long>?, date: LocalDate = LocalDate.now()): CreatedLabelingDataset {
        val export = buildExport(ownerUserId, ids, date)
        val dataset = datasetTransferService.importCsv(
            PostgresCsv.write(DatasetTransferService.DATASET_COLUMNS, export.datasetRows),
            PostgresCsv.write(DatasetTransferService.EVALUATION_DATA_COLUMNS, export.testCaseRows),
            ownerUserId
        ).single()
        return CreatedLabelingDataset(dataset, export.summary)
    }

    private class Export(
        val datasetRows: List<List<String?>>,
        val testCaseRows: List<List<String?>>,
        val summary: LabelExportSummary
    )

    /** Rows in the [DatasetTransferService.DATASET_COLUMNS] / [DatasetTransferService.EVALUATION_DATA_COLUMNS] layout. */
    private fun buildExport(ownerUserId: String, ids: List<Long>?, date: LocalDate): Export {
        val ownedIds = if (ids == null) {
            repository.listByOwner(ownerUserId).map { it.id }
        } else {
            repository.filterOwnedIds(ids, ownerUserId)
        }
        val models = ownedIds.distinct().sorted().mapNotNull { repository.getByIdAndOwner(it, ownerUserId) }
        val labelsByModel = models.map { it to labelsOf(it) }

        val datasetRow = listOf(
            DATASET_ID,
            "LLM pre-labels – Process Analysis $date",
            "Exported from ${models.size} process model(s) of the Process Analysis on $date. " +
                "All labels are unreviewed LLM analysis results; explanations starting with " +
                "\"${LLM_EXPLANATION_PREFIX.trim()}\" are the LLM's reasoning.",
            null,
            null
        )
        val testCaseRows = labelsByModel.map { (model, labels) ->
            listOf(
                model.id.toString(),
                model.bpmnXml,
                objectMapper.writeValueAsString((labels as? ModelLabels.Labeled)?.labels.orEmpty()),
                null,
                null,
                model.name,
                DATASET_ID
            )
        }
        val summary = LabelExportSummary(
            exportedModelCount = models.size,
            labelCount = labelsByModel.sumOf { (_, labels) -> (labels as? ModelLabels.Labeled)?.labels?.size ?: 0 },
            notAnalyzedModels = labelsByModel.filter { it.second is ModelLabels.NotAnalyzed }.map { it.first.name },
            unreadableModels = labelsByModel.filter { it.second is ModelLabels.Unreadable }.map { it.first.name },
            droppedLabelCount = labelsByModel.sumOf { (_, labels) -> (labels as? ModelLabels.Labeled)?.droppedCount ?: 0 }
        )
        return Export(listOf(datasetRow), testCaseRows, summary)
    }

    internal sealed interface ModelLabels {
        data object NotAnalyzed : ModelLabels
        data object Unreadable : ModelLabels
        /** [droppedCount] labels were left out because their element is no longer in the diagram */
        data class Labeled(val labels: List<ExpectedValue>, val droppedCount: Int) : ModelLabels
    }

    /**
     * The model's analysis result as labels. Labels for elements that no longer exist in the
     * stored diagram (it was edited after the analysis) are dropped, as they could never be matched.
     */
    internal fun labelsOf(model: ProcessModel): ModelLabels {
        val resultJson = model.analysisResultJson ?: return ModelLabels.NotAnalyzed
        val labels = try {
            readLabels(resultJson)
        } catch (e: Exception) {
            log.warn(e) { "Analysis result of process model ${model.id} could not be read, exporting it without labels" }
            return ModelLabels.Unreadable
        }
        val existingIds = elementIds(model)
        val kept = labels.filter { existingIds == null || it.value in existingIds }.distinctBy { it.value }
        return ModelLabels.Labeled(kept, droppedCount = labels.distinctBy { it.value }.size - kept.size)
    }

    /** Reads the element list of either stored result shape: binary ([AnalysisResponse]) or multiclass ([MulticlassAnalysisResponse]). */
    private fun readLabels(resultJson: String): List<ExpectedValue> {
        val result = objectMapper.readTree(resultJson)
        val classified = result.get("classifiedElements")
        val critical = result.get("criticalElements")
        return when {
            classified != null && !classified.isNull ->
                objectMapper.treeToValue(classified, Array<MulticlassAnalysisResponse.ClassifiedElement>::class.java)
                    .map { ExpectedValue(it.id, llmExplanation(it.reason), it.classification) }
            critical != null && !critical.isNull ->
                objectMapper.treeToValue(critical, Array<AnalysisResponse.CriticalElement>::class.java)
                    .map { ExpectedValue(it.id, llmExplanation(it.reason), emptyList()) }
            else -> throw IllegalArgumentException("Neither classifiedElements nor criticalElements in analysis result")
        }
    }

    private fun llmExplanation(reason: String?): String? =
        reason?.takeIf { it.isNotBlank() }?.let { LLM_EXPLANATION_PREFIX + it }

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
}
