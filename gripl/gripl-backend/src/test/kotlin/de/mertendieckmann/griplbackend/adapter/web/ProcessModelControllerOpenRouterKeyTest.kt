package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.adapter.auth.AuthServiceClient
import de.mertendieckmann.griplbackend.application.ProcessModelJobRunner
import de.mertendieckmann.griplbackend.model.dto.EnqueueAnalysisRequest
import de.mertendieckmann.griplbackend.repository.ProcessModelRepository
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_USER_ID_ATTRIBUTE
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import de.mertendieckmann.griplbackend.repository.CustomAnalysisEndpointRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.ObjectMapper

/**
 * gripl-backend holds no LLM provider key of its own (auth-service#8
 * follow-up) — every analysis is billed to the caller's own OpenRouter
 * account, fetched via [AuthServiceClient] and injected into the request
 * before it's enqueued.
 */
class ProcessModelControllerOpenRouterKeyTest {

    private val repository = mock<ProcessModelRepository>()
    private val jobRunner = mock<ProcessModelJobRunner>()
    private val authServiceClient = mock<AuthServiceClient>()
    private val controller = ProcessModelController(repository, jobRunner, ObjectMapper(), authServiceClient, mock<CustomAnalysisEndpointRepository>())

    private fun exchange() =
        MockServerWebExchange.from(
            MockServerHttpRequest.post("/process-models/analyze").header("Authorization", "Bearer test-token")
        ).apply { attributes[AUTHENTICATED_USER_ID_ATTRIBUTE] = "user-1" }

    private val request = EnqueueAnalysisRequest(ids = listOf(1L), endpoint = "/gdpr/analysis/prompt-engineering")

    @Test
    fun `without a stored key the call fails clearly instead of completing empty`() {
        runBlocking { whenever(authServiceClient.getOpenRouterApiKey(any())).thenReturn(null) }

        // Regression: a suspend block returning null makes `mono { }` complete
        // *empty*, not emit null — an earlier version of this controller
        // checked for null inside flatMap, which never runs on an empty
        // Mono, so the endpoint silently returned 200 with no body instead
        // of rejecting the request.
        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.analyzeProcessModels(request, exchange()).block()
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)

        verify(jobRunner, never()).enqueue(any())
    }

    @Test
    fun `with a stored key it is injected into llmProps before enqueueing`() {
        runBlocking { whenever(authServiceClient.getOpenRouterApiKey(any())).thenReturn("sk-or-v1-caller-key") }
        whenever(repository.filterOwnedIds(any(), any())).thenReturn(listOf(1L))
        whenever(jobRunner.enqueue(any())).thenReturn(listOf(1L))

        val response = controller.analyzeProcessModels(request, exchange()).block()

        assertEquals(HttpStatus.ACCEPTED, response?.statusCode)
        verify(jobRunner).enqueue(argThat { req: EnqueueAnalysisRequest -> req.llmProps?.apiKey == "sk-or-v1-caller-key" })
    }
}
