package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.adapter.auth.AuthServiceClient
import de.mertendieckmann.griplbackend.application.ProcessModelJobRunner
import de.mertendieckmann.griplbackend.model.dto.EnqueueAnalysisRequest
import de.mertendieckmann.griplbackend.model.dto.ProcessModelListItemDto
import de.mertendieckmann.griplbackend.model.dto.ProcessModelStatus
import de.mertendieckmann.griplbackend.repository.ProcessModelRepository
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_USER_ID_ATTRIBUTE
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.ObjectMapper

/** Process models are private to the user that uploaded them. */
class ProcessModelControllerOwnershipTest {

    private val repository = mock<ProcessModelRepository>()
    private val jobRunner = mock<ProcessModelJobRunner>()
    private val authServiceClient = mock<AuthServiceClient>()
    private val controller = ProcessModelController(repository, jobRunner, ObjectMapper(), authServiceClient)

    private fun exchangeAs(userId: String?) =
        MockServerWebExchange.from(
            MockServerHttpRequest.method(HttpMethod.GET, "/process-models").header("Authorization", "Bearer t")
        ).apply { if (userId != null) attributes[AUTHENTICATED_USER_ID_ATTRIBUTE] = userId }

    @Test
    fun `list only returns the caller's own models`() {
        val mine = listOf(
            ProcessModelListItemDto(1, "mine", ProcessModelStatus.PENDING, null, null, null, null, "t", "t")
        )
        whenever(repository.listByOwner("user-1")).thenReturn(mine)

        assertEquals(mine, controller.listProcessModels(exchangeAs("user-1")).block())

        verify(repository).listByOwner("user-1")
    }

    @Test
    fun `getting a model owned by someone else is a 404 like a missing one`() {
        whenever(repository.getByIdAndOwner(5, "attacker")).thenReturn(null)

        val response = controller.getProcessModel(5, exchangeAs("attacker")).block()

        assertEquals(HttpStatus.NOT_FOUND, response?.statusCode)
    }

    @Test
    fun `deleting a model owned by someone else is a 404 and deletes nothing`() {
        whenever(repository.getByIdAndOwner(5, "attacker")).thenReturn(null)

        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.deleteProcessModel(5, exchangeAs("attacker")).block()
        }

        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
        verify(repository, never()).deleteIfNotRunning(any(), any())
    }

    @Test
    fun `analyze only enqueues the caller's own ids and skips the rest`() {
        runBlocking { whenever(authServiceClient.getOpenRouterApiKey(any())).thenReturn("sk-or-v1-key") }
        whenever(repository.filterOwnedIds(eq(listOf(1L, 2L)), eq("user-1"))).thenReturn(listOf(1L))
        whenever(jobRunner.enqueue(any())).thenReturn(listOf(1L))

        val response = controller.analyzeProcessModels(
            EnqueueAnalysisRequest(ids = listOf(1L, 2L), endpoint = "/gdpr/analysis/prompt-engineering"),
            exchangeAs("user-1")
        ).block()

        assertEquals(listOf(1L), response?.body?.enqueuedIds)
        assertEquals(listOf(2L), response?.body?.skippedIds)
        verify(jobRunner).enqueue(argThat { req: EnqueueAnalysisRequest -> req.ids == listOf(1L) })
    }

    @Test
    fun `a request with no authenticated user is rejected with 401`() {
        val ex = assertThrows(ResponseStatusException::class.java) { controller.listProcessModels(exchangeAs(null)) }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)
    }
}
