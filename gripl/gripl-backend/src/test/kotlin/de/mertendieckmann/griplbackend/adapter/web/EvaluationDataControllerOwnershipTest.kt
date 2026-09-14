package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.application.PreviewGenerator
import de.mertendieckmann.griplbackend.model.dto.EvaluationData
import de.mertendieckmann.griplbackend.repository.DatasetRepository
import de.mertendieckmann.griplbackend.repository.EvaluationDataRepository
import de.mertendieckmann.griplbackend.repository.PreviewCacheRepository
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_USER_ID_ATTRIBUTE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ResponseStatusException

/**
 * GRIPL-v2#33 — a test case is reachable only through a dataset the caller
 * owns. Repositories are mocked; these assert the controller's ownership gate
 * (404 for "not yours", 400 for a create with no parent dataset).
 *
 * GRIPL-v2#40 — every operation here also requires an admin/researcher GRIPL
 * role; `exchangeAs` defaults to `"admin"` so the #33 tests above keep testing
 * ownership rather than role gating. The role-gating tests are at the bottom.
 */
class EvaluationDataControllerOwnershipTest {

    private val evalRepo = mock<EvaluationDataRepository>()
    private val datasetRepo = mock<DatasetRepository>()
    private val previewCache = mock<PreviewCacheRepository>()
    private val previewGenerator = mock<PreviewGenerator>()
    private val controller = EvaluationDataController(evalRepo, datasetRepo, previewCache, previewGenerator)

    private fun exchangeAs(userId: String, griplRole: String? = "admin") =
        MockServerWebExchange.from(MockServerHttpRequest.get("/dataset/testcase")).apply {
            attributes[AUTHENTICATED_USER_ID_ATTRIBUTE] = userId
            if (griplRole != null) attributes[AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE] = griplRole
        }

    private fun evalData(id: Long, datasetId: Long?) =
        EvaluationData(id = id, name = "tc", bpmnXml = "<x/>", expectedValues = emptyList(), datasetId = datasetId)

    @Test
    fun `get by id is 404 when the caller does not own the parent dataset`() {
        whenever(evalRepo.getEvaluationDataByIdForOwner(7, "user-1")).thenReturn(null)

        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.getBpmnDataset(7, exchangeAs("user-1"))
        }
        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
    }

    @Test
    fun `get by id returns the entry when owned`() {
        val entry = evalData(7, datasetId = 3)
        whenever(evalRepo.getEvaluationDataByIdForOwner(7, "user-1")).thenReturn(entry)

        assertEquals(entry, controller.getBpmnDataset(7, exchangeAs("user-1")))
    }

    @Test
    fun `list meta is scoped to the caller`() {
        whenever(evalRepo.getEvaluationDataForOwner("user-1", 3)).thenReturn(listOf(evalData(7, 3)))

        val result = controller.getAllBpmnDatasetMeta(3, exchangeAs("user-1"))

        assertEquals(listOf(7L), result.map { it.id })
        verify(evalRepo).getEvaluationDataForOwner("user-1", 3)
    }

    @Test
    fun `insert without a datasetId is a 400`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.insertBpmnDataset(
                name = "tc",
                bpmnFile = mock<FilePart>(),
                expectedValues = emptyList(),
                datasetId = null,
                exchange = exchangeAs("user-1"),
            )
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        verify(evalRepo, never()).insertEvaluationData(any())
    }

    @Test
    fun `insert into a dataset the caller does not own is a 404`() {
        whenever(datasetRepo.isDatasetOwnedBy(3, "user-1")).thenReturn(false)

        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.insertBpmnDataset(
                name = "tc",
                bpmnFile = mock<FilePart>(),
                expectedValues = emptyList(),
                datasetId = "3",
                exchange = exchangeAs("user-1"),
            )
        }
        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
        verify(evalRepo, never()).insertEvaluationData(any())
    }

    @Test
    fun `delete of a test case the caller does not own is a 404 and does not delete`() {
        whenever(evalRepo.getEvaluationDataByIdForOwner(7, "user-1")).thenReturn(null)

        val response = controller.deleteBpmnDataset(7, exchangeAs("user-1"))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        verify(evalRepo, never()).deleteEvaluationData(any(), any())
    }

    @Test
    fun `delete of an owned test case succeeds`() {
        whenever(evalRepo.getEvaluationDataByIdForOwner(7, "user-1")).thenReturn(evalData(7, 3))
        whenever(evalRepo.deleteEvaluationData(7, "user-1")).thenReturn(1)

        val response = controller.deleteBpmnDataset(7, exchangeAs("user-1"))

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(evalRepo).deleteEvaluationData(7, "user-1")
    }

    // ----- role gating (GRIPL-v2#40) -----

    @Test
    fun `researcher can list test case metadata, same as admin`() {
        whenever(evalRepo.getEvaluationDataForOwner("user-1", null)).thenReturn(emptyList())

        controller.getAllBpmnDatasetMeta(null, exchangeAs("user-1", griplRole = "researcher"))

        verify(evalRepo).getEvaluationDataForOwner("user-1", null)
    }

    @Test
    fun `dpo is forbidden from listing test cases`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.getAllBpmnDatasetMeta(null, exchangeAs("user-1", griplRole = "dpo"))
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
        verify(evalRepo, never()).getEvaluationDataForOwner(any(), anyOrNull())
    }

    @Test
    fun `end-user is forbidden from deleting a test case`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.deleteBpmnDataset(7, exchangeAs("user-1", griplRole = "end-user"))
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
        verify(evalRepo, never()).deleteEvaluationData(any(), any())
    }
}
