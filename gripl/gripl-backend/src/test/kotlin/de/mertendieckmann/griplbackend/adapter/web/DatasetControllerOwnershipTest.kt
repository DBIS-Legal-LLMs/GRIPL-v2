package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.model.dto.CreateDatasetRequest
import de.mertendieckmann.griplbackend.model.dto.Dataset
import de.mertendieckmann.griplbackend.repository.DatasetRepository
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_USER_ID_ATTRIBUTE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ResponseStatusException

/**
 * GRIPL-v2#33 — DatasetController scopes every operation to the auth-service
 * user id ([AUTHENTICATED_USER_ID_ATTRIBUTE]) put on the exchange by the JWT
 * filter. The repository is mocked; the point is that the controller passes the
 * right owner id through and maps "not yours" to 404.
 *
 * GRIPL-v2#40 — every operation here also requires an admin/researcher GRIPL
 * role; `exchangeAs` defaults to `"admin"` so the #33 tests above keep testing
 * ownership rather than role gating. The role-gating tests are at the bottom.
 */
class DatasetControllerOwnershipTest {

    private val repo = mock<DatasetRepository>()
    private val controller = DatasetController(repo)

    private fun exchangeAs(userId: String?, griplRole: String? = "admin"): MockServerWebExchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/dataset")).apply {
            if (userId != null) attributes[AUTHENTICATED_USER_ID_ATTRIBUTE] = userId
            if (griplRole != null) attributes[AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE] = griplRole
        }

    @Test
    fun `create stamps the authenticated user as owner`() {
        whenever(repo.createDataset(any(), eq("user-1"))).thenReturn(1)

        val response = controller.createDataset(CreateDatasetRequest("ds", null), exchangeAs("user-1"))

        assertEquals(HttpStatus.CREATED, response.statusCode)
        verify(repo).createDataset(any(), eq("user-1"))
    }

    @Test
    fun `list returns only the caller's datasets`() {
        val mine = listOf(dataset(1, "mine"))
        whenever(repo.getDatasetsByOwner("user-1")).thenReturn(mine)

        val result = controller.getAllDatasets(exchangeAs("user-1"))

        assertEquals(mine, result)
        verify(repo).getDatasetsByOwner("user-1")
        verify(repo, never()).getAllDatasets()
    }

    @Test
    fun `delete of a dataset the caller does not own is a 404 and does not delete`() {
        whenever(repo.getDatasetByIdAndOwner(99, "user-1")).thenReturn(null)

        val response = controller.deleteDataset(99, exchangeAs("user-1"))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        verify(repo, never()).deleteDataset(any(), any())
    }

    @Test
    fun `delete of an owned dataset succeeds`() {
        whenever(repo.getDatasetByIdAndOwner(5, "user-1")).thenReturn(dataset(5, "mine"))
        whenever(repo.deleteDataset(5, "user-1")).thenReturn(1)

        val response = controller.deleteDataset(5, exchangeAs("user-1"))

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(repo).deleteDataset(5, "user-1")
    }

    @Test
    fun `a request with no authenticated user is rejected with 401`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.getAllDatasets(exchangeAs(null))
        }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)
    }

    private fun dataset(id: Long, name: String) =
        Dataset(id = id, name = name, description = null, createdAt = "2026-01-01", updatedAt = "2026-01-01")

    // ----- role gating (GRIPL-v2#40) -----

    @Test
    fun `researcher can list datasets, same as admin`() {
        whenever(repo.getDatasetsByOwner("user-1")).thenReturn(emptyList())

        controller.getAllDatasets(exchangeAs("user-1", griplRole = "researcher"))

        verify(repo).getDatasetsByOwner("user-1")
    }

    @Test
    fun `dpo is forbidden from listing datasets`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.getAllDatasets(exchangeAs("user-1", griplRole = "dpo"))
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
        verify(repo, never()).getDatasetsByOwner(any())
    }

    @Test
    fun `end-user is forbidden from creating a dataset`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.createDataset(CreateDatasetRequest("ds", null), exchangeAs("user-1", griplRole = "end-user"))
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
        verify(repo, never()).createDataset(any(), any())
    }

    @Test
    fun `a token with no gripl role at all is forbidden, not just unrecognised roles`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.getAllDatasets(exchangeAs("user-1", griplRole = null))
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
    }
}
