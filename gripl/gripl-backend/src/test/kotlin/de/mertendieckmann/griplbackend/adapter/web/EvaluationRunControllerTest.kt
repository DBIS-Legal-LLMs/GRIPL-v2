package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.adapter.auth.AuthServiceClient
import de.mertendieckmann.griplbackend.application.ActiveEvaluationRunExistsException
import de.mertendieckmann.griplbackend.application.EvaluationJobRunner
import de.mertendieckmann.griplbackend.config.LlmConfig
import de.mertendieckmann.griplbackend.model.dto.EvaluationRunStatus
import de.mertendieckmann.griplbackend.model.dto.ModelRunConfig
import de.mertendieckmann.griplbackend.model.dto.MultiEvaluationRequest
import de.mertendieckmann.griplbackend.model.dto.RenameEvaluationRunRequest
import de.mertendieckmann.griplbackend.repository.CustomAnalysisEndpointRepository
import de.mertendieckmann.griplbackend.repository.EvaluationRunRepository
import de.mertendieckmann.griplbackend.repository.EvaluationRunRow
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_USER_ID_ATTRIBUTE
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ResponseStatusException
import tools.jackson.module.kotlin.jacksonObjectMapper

class EvaluationRunControllerTest {

    private val jobRunner = mock<EvaluationJobRunner>()
    private val repository = mock<EvaluationRunRepository>()
    private val env = mock<Environment>()
    private val authServiceClient = mock<AuthServiceClient>()
    private val controller = EvaluationRunController(
        jobRunner, repository, env, authServiceClient, mock<CustomAnalysisEndpointRepository>(), jacksonObjectMapper()
    )

    private val authKey = "sk-or-v1-from-auth-service"

    private fun exchangeAs(griplRole: String?, userId: String = "user-1") =
        MockServerWebExchange.from(
            MockServerHttpRequest.post("/gdpr/evaluation/runs").header("Authorization", "Bearer test-token")
        ).apply {
            attributes[AUTHENTICATED_USER_ID_ATTRIBUTE] = userId
            if (griplRole != null) attributes[AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE] = griplRole
        }

    private val request = MultiEvaluationRequest(
        defaultEvaluationEndpoint = "/gdpr/analysis/prompt-engineering",
        models = listOf(
            ModelRunConfig("a", llmProps = LlmConfig.Companion.LlmPropsOverride(modelName = "m", apiKey = "sk-or-v1-from-yaml"))
        ),
        seed = null,
        datasets = listOf(5)
    )

    private fun row(status: EvaluationRunStatus) = EvaluationRunRow(
        id = 42L, ownerUserId = "user-1", status = status,
        requestJson = jacksonObjectMapper().writeValueAsString(request.copy(seed = 7, models = listOf(ModelRunConfig("a")))),
        metadataJson = null, totalTestCases = null, completedTestCases = 0, errorMessage = null,
        createdAt = "2026-11-24", startedAt = null, finishedAt = null
    )

    private suspend fun stubKeyAndPlaceholders() {
        whenever(env.resolvePlaceholders(any())).thenAnswer { it.arguments[0] }
        whenever(authServiceClient.getOpenRouterApiKey(any())).thenReturn(authKey)
    }

    @Test
    fun `dpo and end-user cannot start or list runs`() {
        listOf("dpo", "end-user").forEach { role ->
            val start = assertThrows(ResponseStatusException::class.java) {
                runBlocking { controller.startRun(request, exchangeAs(role)) }
            }
            assertEquals(HttpStatus.FORBIDDEN, start.statusCode)
            val list = assertThrows(ResponseStatusException::class.java) {
                runBlocking { controller.listRuns(exchangeAs(role)) }
            }
            assertEquals(HttpStatus.FORBIDDEN, list.statusCode)
        }
    }

    @Test
    fun `the stored request has no api key and a fixed seed, the job gets the key`() {
        runBlocking {
            stubKeyAndPlaceholders()
            whenever(jobRunner.start(any(), any(), any(), anyOrNull())).thenReturn(42L)

            val response = controller.startRun(request, exchangeAs("researcher"))

            assertEquals(HttpStatus.ACCEPTED, response.statusCode)
            assertEquals(42L, response.body!!.id)
            val stored = argumentCaptor<MultiEvaluationRequest>()
            val run = argumentCaptor<MultiEvaluationRequest>()
            verify(jobRunner).start(eq("user-1"), stored.capture(), run.capture(), anyOrNull())
            assertNull(stored.firstValue.models.single().llmProps?.apiKey)
            assertNotNull(stored.firstValue.seed)
            assertEquals(stored.firstValue.seed, run.firstValue.seed)
            assertEquals(authKey, run.firstValue.models.single().llmProps?.apiKey)
        }
    }

    @Test
    fun `starting while a run is active is a conflict`() {
        runBlocking {
            stubKeyAndPlaceholders()
            whenever(jobRunner.start(any(), any(), any(), anyOrNull())).thenThrow(ActiveEvaluationRunExistsException())

            val ex = assertThrows(ResponseStatusException::class.java) {
                runBlocking { controller.startRun(request, exchangeAs("researcher")) }
            }
            assertEquals(HttpStatus.CONFLICT, ex.statusCode)
        }
    }

    @Test
    fun `another user's run is not found`() {
        whenever(repository.getByIdAndOwner(42L, "user-2")).thenReturn(null)

        listOf<suspend () -> Unit>(
            { controller.getRun(42L, exchangeAs("researcher", "user-2")) },
            { controller.getEvents(42L, 0, 100, exchangeAs("researcher", "user-2")) },
            { controller.cancelRun(42L, exchangeAs("researcher", "user-2")) },
            { controller.resumeRun(42L, exchangeAs("researcher", "user-2")) },
            { controller.deleteRun(42L, exchangeAs("researcher", "user-2")) }
        ).forEach { call ->
            val ex = assertThrows(ResponseStatusException::class.java) { runBlocking { call() } }
            assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
        }
        verify(repository, never()).eventsAfter(any(), any(), any(), any())
    }

    @Test
    fun `a finished run can be neither cancelled nor resumed`() {
        runBlocking {
            stubKeyAndPlaceholders()
            whenever(repository.getByIdAndOwner(42L, "user-1")).thenReturn(row(EvaluationRunStatus.DONE))

            val cancel = assertThrows(ResponseStatusException::class.java) {
                runBlocking { controller.cancelRun(42L, exchangeAs("researcher")) }
            }
            assertEquals(HttpStatus.CONFLICT, cancel.statusCode)
            val resume = assertThrows(ResponseStatusException::class.java) {
                runBlocking { controller.resumeRun(42L, exchangeAs("researcher")) }
            }
            assertEquals(HttpStatus.CONFLICT, resume.statusCode)
            verify(jobRunner, never()).resume(any(), any(), any())
        }
    }

    @Test
    fun `resuming fetches the key again and keeps the stored seed`() {
        runBlocking {
            stubKeyAndPlaceholders()
            whenever(repository.getByIdAndOwner(42L, "user-1")).thenReturn(row(EvaluationRunStatus.INTERRUPTED))
            whenever(jobRunner.resume(any(), any(), any())).thenReturn(true)

            assertEquals(HttpStatus.ACCEPTED, controller.resumeRun(42L, exchangeAs("researcher")).statusCode)

            val run = argumentCaptor<MultiEvaluationRequest>()
            verify(jobRunner).resume(eq(42L), eq("user-1"), run.capture())
            assertEquals(7, run.firstValue.seed)
            assertEquals(authKey, run.firstValue.models.single().llmProps?.apiKey)
        }
    }

    @Test
    fun `the name is trimmed, stored apart from the request, and a blank name is no name`() {
        runBlocking {
            stubKeyAndPlaceholders()
            whenever(jobRunner.start(any(), any(), any(), anyOrNull())).thenReturn(42L)

            controller.startRun(request.copy(name = "  GPT vs Llama  "), exchangeAs("researcher"))
            controller.startRun(request.copy(name = "   "), exchangeAs("researcher"))

            val stored = argumentCaptor<MultiEvaluationRequest>()
            val names = argumentCaptor<String>()
            verify(jobRunner, org.mockito.kotlin.times(2)).start(eq("user-1"), stored.capture(), any(), names.capture())
            assertEquals(listOf("GPT vs Llama", null), names.allValues)
            stored.allValues.forEach { assertNull(it.name) }
        }
    }

    @Test
    fun `renaming sets or removes the name of the caller's own run only`() {
        runBlocking {
            whenever(repository.rename(42L, "user-1", "New name")).thenReturn(true)
            whenever(repository.rename(42L, "user-1", null)).thenReturn(true)
            whenever(repository.getByIdAndOwner(42L, "user-1")).thenReturn(row(EvaluationRunStatus.DONE).copy(name = "New name"))

            val renamed = controller.renameRun(42L, RenameEvaluationRunRequest(" New name "), exchangeAs("researcher"))
            assertEquals("New name", renamed.name)
            controller.renameRun(42L, RenameEvaluationRunRequest(""), exchangeAs("researcher"))
            verify(repository).rename(42L, "user-1", null)

            whenever(repository.rename(42L, "user-2", "x")).thenReturn(false)
            val notFound = assertThrows(ResponseStatusException::class.java) {
                runBlocking { controller.renameRun(42L, RenameEvaluationRunRequest("x"), exchangeAs("researcher", "user-2")) }
            }
            assertEquals(HttpStatus.NOT_FOUND, notFound.statusCode)

            val tooLong = assertThrows(ResponseStatusException::class.java) {
                runBlocking { controller.renameRun(42L, RenameEvaluationRunRequest("x".repeat(201)), exchangeAs("researcher")) }
            }
            assertEquals(HttpStatus.BAD_REQUEST, tooLong.statusCode)
        }
    }
}
