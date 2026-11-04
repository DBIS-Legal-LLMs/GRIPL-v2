package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.adapter.auth.AuthServiceClient
import de.mertendieckmann.griplbackend.application.ProcessModelJobRunner
import de.mertendieckmann.griplbackend.model.dto.EnqueueAnalysisRequest
import de.mertendieckmann.griplbackend.model.dto.ProcessModel
import de.mertendieckmann.griplbackend.model.dto.ProcessModelListItemDto
import de.mertendieckmann.griplbackend.model.dto.ProcessModelStatus
import de.mertendieckmann.griplbackend.model.dto.UpdateProcessModelNameRequest
import de.mertendieckmann.griplbackend.repository.ProcessModelRepository
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_USER_ID_ATTRIBUTE
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import de.mertendieckmann.griplbackend.repository.CustomAnalysisEndpointRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpMethod
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import tools.jackson.databind.ObjectMapper

/** Process models are private to the user that uploaded them. */
class ProcessModelControllerOwnershipTest {

    private val repository = mock<ProcessModelRepository>()
    private val jobRunner = mock<ProcessModelJobRunner>()
    private val authServiceClient = mock<AuthServiceClient>()
    private val customEndpoints = mock<CustomAnalysisEndpointRepository>()
    private val controller = ProcessModelController(
        repository = repository,
        jobRunner = jobRunner,
        objectMapper = ObjectMapper(),
        authServiceClient = authServiceClient,
        customAnalysisEndpointRepository = customEndpoints
    )

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

    private fun bpmnFilePart(xml: String) = mock<FilePart>().also {
        whenever(it.content()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(xml.toByteArray())))
    }

    private fun formField(value: String) = mock<FormFieldPart>().also { whenever(it.value()).thenReturn(value) }

    private fun model(status: ProcessModelStatus, bpmnXml: String = "<old/>", bpmnVersion: Int = 3) =
        ProcessModel(5, "m", bpmnXml, status, null, null, null, null, null, null, null, "t", "t", bpmnVersion = bpmnVersion)

    @Test
    fun `updating a model owned by someone else is a 404 and updates nothing`() {
        whenever(repository.getByIdAndOwner(5, "attacker")).thenReturn(null)

        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.updateProcessModel(5, bpmnFilePart("<new/>"), formField("3"), exchangeAs("attacker")).block()
        }

        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
        verify(repository, never()).updateBpmnXmlIfUnchanged(any(), any(), any(), any())
    }

    @Test
    fun `updating a model that is being analyzed is a 409 and updates nothing`() {
        whenever(repository.getByIdAndOwner(5, "user-1")).thenReturn(model(ProcessModelStatus.RUNNING))

        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.updateProcessModel(5, bpmnFilePart("<new/>"), formField("3"), exchangeAs("user-1")).block()
        }

        assertEquals(HttpStatus.CONFLICT, ex.statusCode)
        verify(repository, never()).updateBpmnXmlIfUnchanged(any(), any(), any(), any())
    }

    @Test
    fun `updating a model that was saved elsewhere since is a 409 and does not overwrite it`() {
        whenever(repository.getByIdAndOwner(5, "user-1")).thenReturn(model(ProcessModelStatus.DONE, bpmnVersion = 4))

        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.updateProcessModel(5, bpmnFilePart("<new/>"), formField("3"), exchangeAs("user-1")).block()
        }

        assertEquals(HttpStatus.CONFLICT, ex.statusCode)
        assertTrue(ex.reason!!.contains("saved elsewhere"))
        verify(repository, never()).updateBpmnXmlIfUnchanged(any(), any(), any(), any())
    }

    @Test
    fun `losing the race against another save between check and update is reported as that conflict`() {
        whenever(repository.getByIdAndOwner(5, "user-1"))
            .thenReturn(model(ProcessModelStatus.DONE), model(ProcessModelStatus.DONE, bpmnVersion = 4))
        whenever(repository.updateBpmnXmlIfUnchanged(5, "user-1", "<new/>", 3)).thenReturn(false)

        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.updateProcessModel(5, bpmnFilePart("<new/>"), formField("3"), exchangeAs("user-1")).block()
        }

        assertEquals(HttpStatus.CONFLICT, ex.statusCode)
        assertTrue(ex.reason!!.contains("saved elsewhere"))
    }

    @Test
    fun `updating with a non-numeric version is a 400`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.updateProcessModel(5, bpmnFilePart("<new/>"), formField("latest"), exchangeAs("user-1")).block()
        }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun `updating the caller's own model stores the new xml`() {
        whenever(repository.getByIdAndOwner(5, "user-1"))
            .thenReturn(
                model(ProcessModelStatus.DONE),
                model(ProcessModelStatus.DONE, "<new/>", bpmnVersion = 4).copy(analysisOutdated = true)
            )
        whenever(repository.updateBpmnXmlIfUnchanged(5, "user-1", "<new/>", 3)).thenReturn(true)

        val response = controller.updateProcessModel(5, bpmnFilePart("<new/>"), formField("3"), exchangeAs("user-1")).block()

        assertEquals(HttpStatus.OK, response?.statusCode)
        assertEquals("<new/>", response?.body?.bpmnXml)
        assertEquals(true, response?.body?.analysisOutdated)
        assertEquals(4, response?.body?.bpmnVersion)
        verify(repository).updateBpmnXmlIfUnchanged(5, "user-1", "<new/>", 3)
    }

    @Test
    fun `uploading with a file name longer than the name column is a 400 instead of a database error`() {
        val file = bpmnFilePart("<bpmn/>").also { whenever(it.filename()).thenReturn("a".repeat(300) + ".bpmn") }

        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.uploadProcessModel(file, null, exchangeAs("user-1")).block()
        }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        verify(repository, never()).create(any(), any(), any())
    }

    @Test
    fun `uploading stores the trimmed name`() {
        whenever(repository.create("Onboarding", "<bpmn/>", "user-1")).thenReturn(5)
        whenever(repository.getByIdAndOwner(5, "user-1")).thenReturn(model(ProcessModelStatus.PENDING).copy(name = "Onboarding"))

        val response = controller.uploadProcessModel(
            bpmnFilePart("<bpmn/>"), formField("  Onboarding "), exchangeAs("user-1")
        ).block()

        assertEquals(HttpStatus.CREATED, response?.statusCode)
        verify(repository).create("Onboarding", "<bpmn/>", "user-1")
    }

    @Test
    fun `renaming to a name longer than the name column is a 400`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.renameProcessModel(5, UpdateProcessModelNameRequest("a".repeat(256)), exchangeAs("user-1")).block()
        }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        verify(repository, never()).updateName(any(), any(), any())
    }

    @Test
    fun `renaming a model owned by someone else is a 404`() {
        whenever(repository.updateName(5, "attacker", "new")).thenReturn(false)

        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.renameProcessModel(5, UpdateProcessModelNameRequest("new"), exchangeAs("attacker")).block()
        }

        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
    }

    @Test
    fun `renaming to a blank name is a 400 and renames nothing`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.renameProcessModel(5, UpdateProcessModelNameRequest("   "), exchangeAs("user-1")).block()
        }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        verify(repository, never()).updateName(any(), any(), any())
    }

    @Test
    fun `renaming the caller's own model stores the trimmed name`() {
        whenever(repository.updateName(5, "user-1", "new name")).thenReturn(true)
        whenever(repository.getByIdAndOwner(5, "user-1")).thenReturn(model(ProcessModelStatus.DONE).copy(name = "new name"))

        val response = controller.renameProcessModel(5, UpdateProcessModelNameRequest("  new name "), exchangeAs("user-1")).block()

        assertEquals("new name", response?.body?.name)
        verify(repository).updateName(5, "user-1", "new name")
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

    @Test
    fun `analyze with someone else's custom endpoint is a 404 and enqueues nothing`() {
        runBlocking { whenever(authServiceClient.getOpenRouterApiKey(any())).thenReturn("sk-or-v1-key") }
        whenever(customEndpoints.getByIdAndOwner(7, "attacker")).thenReturn(null)

        val ex = assertThrows(ResponseStatusException::class.java) {
            controller.analyzeProcessModels(
                EnqueueAnalysisRequest(ids = listOf(1L), endpoint = "/gdpr/analysis/custom/7"),
                exchangeAs("attacker")
            ).block()
        }

        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
        verify(jobRunner, never()).enqueue(any())
    }
}
