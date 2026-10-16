package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.adapter.web.utils.ControllerUtils
import de.mertendieckmann.griplbackend.model.dto.CreateCustomAnalysisEndpointRequest
import de.mertendieckmann.griplbackend.model.dto.CustomAnalysisEndpoint
import de.mertendieckmann.griplbackend.model.dto.CustomAnalysisResponseType
import de.mertendieckmann.griplbackend.repository.CustomAnalysisEndpointRepository
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_USER_ID_ATTRIBUTE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ResponseStatusException

/** Custom analysis endpoints are private to the user that created them. */
class CustomAnalysisEndpointControllerOwnershipTest {

    private val repository = mock<CustomAnalysisEndpointRepository>()
    private val controller = CustomAnalysisEndpointController(repository)

    private fun exchangeAs(userId: String) =
        MockServerWebExchange.from(MockServerHttpRequest.get("/custom-analysis-endpoints"))
            .apply { attributes[AUTHENTICATED_USER_ID_ATTRIBUTE] = userId }

    private fun endpoint(id: Long) = CustomAnalysisEndpoint(
        id, "e$id", "prompt", CustomAnalysisResponseType.BINARY, false, null, "t", "t"
    )

    @Test
    fun `create stamps the authenticated user as owner`() {
        whenever(repository.create(any(), any(), any(), any(), anyOrNull(), eq("user-1"))).thenReturn(3L)
        whenever(repository.getByIdAndOwner(3, "user-1")).thenReturn(endpoint(3))

        val response = controller.create(
            CreateCustomAnalysisEndpointRequest("n", "p", CustomAnalysisResponseType.BINARY, false, null),
            exchangeAs("user-1")
        )

        assertEquals(HttpStatus.CREATED, response.statusCode)
        verify(repository).create(any(), any(), any(), any(), anyOrNull(), eq("user-1"))
    }

    @Test
    fun `list only returns the caller's own endpoints`() {
        whenever(repository.listByOwner("user-1")).thenReturn(listOf(endpoint(1)))

        assertEquals(listOf(endpoint(1)), controller.listAll(exchangeAs("user-1")))
    }

    @Test
    fun `deleting someone else's endpoint is a 404`() {
        whenever(repository.deleteByOwner(5, "attacker")).thenReturn(false)

        assertEquals(HttpStatus.NOT_FOUND, controller.delete(5, exchangeAs("attacker")).statusCode)
    }

    @Test
    fun `requireOwnedCustomEndpoint rejects a foreign custom endpoint but lets built-ins through`() {
        whenever(repository.getByIdAndOwner(7, "attacker")).thenReturn(null)

        val ex = assertThrows(ResponseStatusException::class.java) {
            ControllerUtils.requireOwnedCustomEndpoint("/gdpr/analysis/custom/7", repository, "attacker")
        }
        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)

        ControllerUtils.requireOwnedCustomEndpoint("/gdpr/analysis/prompt-engineering", repository, "attacker")
        ControllerUtils.requireOwnedCustomEndpoint(null, repository, "attacker")
        verify(repository, never()).getByIdAndOwner(eq(0), any())
    }
}
