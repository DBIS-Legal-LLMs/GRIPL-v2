package de.mertendieckmann.griplbackend.application.dataset

import de.mertendieckmann.griplbackend.model.analysis.GdprProcessingClass
import de.mertendieckmann.griplbackend.model.dto.ExpectedValue
import de.mertendieckmann.griplbackend.model.dto.ProcessModel
import de.mertendieckmann.griplbackend.model.dto.ProcessModelListItemDto
import de.mertendieckmann.griplbackend.model.dto.ProcessModelStatus
import de.mertendieckmann.griplbackend.repository.ProcessModelRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.zip.ZipInputStream

class ProcessModelLabelExportServiceTest {

    private val repository = mock<ProcessModelRepository>()
    private val datasetTransferService = mock<DatasetTransferService>()
    private val service = ProcessModelLabelExportService(repository, datasetTransferService)
    private val objectMapper = jacksonObjectMapper()

    private val bpmnXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
          <bpmn:process id="Process_1" isExecutable="false">
            <bpmn:startEvent id="Start_1"/>
            <bpmn:task id="Task_collect" name="Collect data"/>
            <bpmn:exclusiveGateway id="Gateway_minor"/>
          </bpmn:process>
        </bpmn:definitions>
    """.trimIndent()

    private fun model(id: Long, name: String, resultJson: String?) =
        ProcessModel(id, name, bpmnXml, ProcessModelStatus.DONE, null, null, resultJson, null, null, null, null, "t", "t", bpmnVersion = 0)

    /** Reads the ZIP back exactly like DatasetTransferService.importZip does. */
    private fun readZip(bytes: ByteArray): Map<String, List<Map<String, String?>>> {
        val files = mutableMapOf<String, List<Map<String, String?>>>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                files[entry.name] = PostgresCsv.read(zip.readBytes().toString(Charsets.UTF_8))
            }
        }
        return files
    }

    private fun labelsOf(row: Map<String, String?>): List<ExpectedValue> = objectMapper.readValue(row["expected_values"]!!)

    @Test
    fun `multiclass results become labels with classes and a marked LLM explanation`() {
        val result = """
            {"classifiedElements": [
              {"id": "Task_collect", "name": "Collect data", "reason": "Collects the name", "classification": ["COLLECTION", "STORAGE"]}
            ], "amountOfRetries": 0}
        """.trimIndent()
        whenever(repository.filterOwnedIds(listOf(1L), "user-1")).thenReturn(listOf(1L))
        whenever(repository.getByIdAndOwner(1, "user-1")).thenReturn(model(1, "Onboarding", result))

        val export = service.exportAsZip("user-1", listOf(1L), LocalDate.of(2026, 11, 3))

        assertEquals(LabelExportSummary(1, 1, emptyList(), emptyList(), 0), export.summary)
        val files = readZip(export.zip)
        val dataset = files.getValue(DatasetTransferService.DATASET_FILE).single()
        assertEquals("LLM pre-labels – Process Analysis 2026-11-03", dataset["name"])
        val testCase = files.getValue(DatasetTransferService.EVALUATION_DATA_FILE).single()
        assertEquals("Onboarding", testCase["name"])
        assertEquals(bpmnXml, testCase["bpmn_xml"])
        assertEquals(dataset["id"], testCase["dataset_id"])
        assertEquals(
            listOf(ExpectedValue("Task_collect", "[LLM] Collects the name", listOf(GdprProcessingClass.COLLECTION, GdprProcessingClass.STORAGE))),
            labelsOf(testCase)
        )
    }

    @Test
    fun `binary results become labels without class and labels for removed elements are dropped`() {
        val result = """
            {"criticalElements": [
              {"id": "Task_collect", "name": "Collect data", "type": "task", "reason": "Personal data", "references": []},
              {"id": "Gateway_minor", "name": null, "reason": "Decides on age"},
              {"id": "Task_deleted_after_analysis", "name": "Old", "reason": "Gone"}
            ]}
        """.trimIndent()

        val labels = service.labelsOf(model(1, "m", result))

        assertEquals(
            ProcessModelLabelExportService.ModelLabels.Labeled(
                listOf(
                    ExpectedValue("Task_collect", "[LLM] Personal data", emptyList()),
                    ExpectedValue("Gateway_minor", "[LLM] Decides on age", emptyList())
                ),
                droppedCount = 1
            ),
            labels
        )
    }

    @Test
    fun `a result whose elements no longer match the result DTOs is reported as unreadable instead of exporting no labels`() {
        val result = """{"criticalElements": [{"elementId": "Task_collect", "reason": "renamed field"}]}"""

        assertEquals(ProcessModelLabelExportService.ModelLabels.Unreadable, service.labelsOf(model(1, "m", result)))
    }

    @Test
    fun `without ids all own models are exported and models without labels are reported in the summary`() {
        whenever(repository.listByOwner("user-1")).thenReturn(
            listOf(
                ProcessModelListItemDto(2, "b", ProcessModelStatus.PENDING, null, null, null, null, "t", "t"),
                ProcessModelListItemDto(1, "a", ProcessModelStatus.PENDING, null, null, null, null, "t", "t")
            )
        )
        whenever(repository.getByIdAndOwner(1, "user-1")).thenReturn(model(1, "a", null))
        whenever(repository.getByIdAndOwner(2, "user-1")).thenReturn(model(2, "b", "not json"))

        val export = service.exportAsZip("user-1", null)

        val testCases = readZip(export.zip).getValue(DatasetTransferService.EVALUATION_DATA_FILE)
        assertEquals(listOf("a", "b"), testCases.map { it["name"] })
        assertEquals(listOf(emptyList<ExpectedValue>(), emptyList()), testCases.map(::labelsOf))
        assertEquals(
            LabelExportSummary(
                exportedModelCount = 2,
                labelCount = 0,
                notAnalyzedModels = listOf("a"),
                unreadableModels = listOf("b"),
                droppedLabelCount = 0
            ),
            export.summary
        )
    }

    @Test
    fun `creating a dataset imports exactly what the ZIP contains`() {
        val result = """{"classifiedElements": [{"id": "Task_collect", "reason": "r", "classification": ["USAGE"]}]}"""
        whenever(repository.filterOwnedIds(listOf(1L), "user-1")).thenReturn(listOf(1L))
        whenever(repository.getByIdAndOwner(1, "user-1")).thenReturn(model(1, "Onboarding", result))
        val imported = ImportedDataset("1", 42, "LLM pre-labels – Process Analysis 2026-11-03", 1)
        whenever(
            datasetTransferService.importCsv(any(), any(), eq("user-1"), isNull(), isNull())
        ).thenReturn(listOf(imported))
        val date = LocalDate.of(2026, 11, 3)

        val created = service.createDataset("user-1", listOf(1L), date)

        assertEquals(imported, created.dataset)
        val datasetCsv = argumentCaptor<String>()
        val evaluationDataCsv = argumentCaptor<String>()
        verify(datasetTransferService).importCsv(datasetCsv.capture(), evaluationDataCsv.capture(), eq("user-1"), isNull(), isNull())
        val export = service.exportAsZip("user-1", listOf(1L), date)
        assertEquals(export.summary, created.summary)
        val zip = readZip(export.zip)
        assertEquals(zip[DatasetTransferService.DATASET_FILE], PostgresCsv.read(datasetCsv.firstValue))
        assertEquals(zip[DatasetTransferService.EVALUATION_DATA_FILE], PostgresCsv.read(evaluationDataCsv.firstValue))
    }

    @Test
    fun `ids of other users are not exported`() {
        whenever(repository.filterOwnedIds(listOf(1L, 9L), "user-1")).thenReturn(listOf(1L))
        whenever(repository.getByIdAndOwner(1, "user-1")).thenReturn(model(1, "mine", null))

        val testCases = readZip(service.exportAsZip("user-1", listOf(1L, 9L)).zip).getValue(DatasetTransferService.EVALUATION_DATA_FILE)

        assertEquals(listOf("mine"), testCases.map { it["name"] })
    }
}
