package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.application.dataset.CreatedLabelingDataset
import de.mertendieckmann.griplbackend.application.dataset.ImportedDataset
import de.mertendieckmann.griplbackend.application.dataset.LabelExportSummary
import de.mertendieckmann.griplbackend.application.dataset.LabelingZipExport
import de.mertendieckmann.griplbackend.application.dataset.ProcessModelLabelExportService
import de.mertendieckmann.griplbackend.model.dto.CreateLabelingDatasetRequest
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_USER_ID_ATTRIBUTE
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.net.URLDecoder

/** Handing process models over to the labeling tool is open to the labeling roles only, like the dataset import. */
class ProcessModelLabelingControllerTest {

    private val labelExportService = mock<ProcessModelLabelExportService>()
    private val controller = ProcessModelLabelingController(labelExportService = labelExportService, objectMapper = ObjectMapper())
    private val summary = LabelExportSummary(
        exportedModelCount = 2,
        labelCount = 5,
        notAnalyzedModels = listOf("Prüfung anmelden"),
        unreadableModels = emptyList(),
        droppedLabelCount = 1
    )

    private fun exchangeAs(griplRole: String) =
        MockServerWebExchange.from(MockServerHttpRequest.get("/process-models/export-labels")).apply {
            attributes[AUTHENTICATED_USER_ID_ATTRIBUTE] = "user-1"
            attributes[AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE] = griplRole
        }

    @Test
    fun `an end-user cannot export for labeling`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.exportForLabeling(null, exchangeAs("end-user"))
        }

        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
        verify(labelExportService, never()).exportAsZip(any(), anyOrNull(), any())
    }

    @Test
    fun `a researcher gets the export as a ZIP download with the summary in a header`() {
        val zip = byteArrayOf(1, 2, 3)
        whenever(labelExportService.exportAsZip(any(), anyOrNull(), any())).thenReturn(LabelingZipExport(zip, summary))

        val response = controller.exportForLabeling(listOf(1L, 2L), exchangeAs("researcher")).block()

        assertEquals(HttpStatus.OK, response?.statusCode)
        assertArrayEquals(zip, response?.body)
        assertTrue(response!!.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)!!.contains("gripl-llm-prelabels-"))
        val header = response.headers.getFirst(ProcessModelLabelingController.EXPORT_SUMMARY_HEADER)!!
        assertTrue(header.all { it.code < 128 } && ' ' !in header && '+' !in header, "header must be plain ASCII: $header")
        // What the frontend does: decodeURIComponent + JSON.parse
        val decoded = URLDecoder.decode(header, Charsets.UTF_8)
        assertEquals(summary, jacksonObjectMapper().readValue<LabelExportSummary>(decoded))
        verify(labelExportService).exportAsZip(eq("user-1"), eq(listOf(1L, 2L)), any())
    }

    @Test
    fun `an end-user cannot create a labeling dataset`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.createLabelingDataset(CreateLabelingDatasetRequest(), exchangeAs("end-user"))
        }

        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
        verify(labelExportService, never()).createDataset(any(), anyOrNull(), any())
    }

    @Test
    fun `an admin can create a labeling dataset and gets the summary`() {
        val created = CreatedLabelingDataset(ImportedDataset("1", 42, "LLM pre-labels", 3), summary)
        whenever(labelExportService.createDataset(any(), anyOrNull(), any())).thenReturn(created)

        val response = controller.createLabelingDataset(CreateLabelingDatasetRequest(ids = null), exchangeAs("admin")).block()

        assertEquals(HttpStatus.CREATED, response?.statusCode)
        assertEquals(created, response?.body)
    }
}
