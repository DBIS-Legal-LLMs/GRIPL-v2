package de.mertendieckmann.griplbackend.application

import de.mertendieckmann.griplbackend.application.analyzer.AnalysisService
import de.mertendieckmann.griplbackend.config.LlmConfig
import de.mertendieckmann.griplbackend.model.dto.EnqueueAnalysisRequest
import de.mertendieckmann.griplbackend.model.dto.ProcessModel
import de.mertendieckmann.griplbackend.model.dto.ProcessModelStatus
import de.mertendieckmann.griplbackend.repository.CustomAnalysisEndpointRepository
import de.mertendieckmann.griplbackend.repository.ProcessModelRepository
import dev.langchain4j.exception.InvalidRequestException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.ConcurrentHashMap

/**
 * The caller's OpenRouter key is passed to the job in memory only (GRIPL#62): it must never reach
 * process_model.analysis_options, and a restart must not leave a job queued that no longer has a key.
 */
class ProcessModelJobRunnerTest {

    private val key = "sk-or-v1-secret-key"
    private val repository = mock<ProcessModelRepository>()
    private val analysisService = mock<AnalysisService>()
    private val runner = ProcessModelJobRunner(repository, analysisService, mock<CustomAnalysisEndpointRepository>())

    private val request = EnqueueAnalysisRequest(
        ids = listOf(7L),
        endpoint = "/gdpr/analysis/prompt-engineering",
        llmProps = LlmConfig.Companion.LlmPropsOverride(modelName = "some/model", apiKey = key),
        useRag = false
    )

    private fun modelWith(optionsJson: String) = ProcessModel(
        id = 7L, name = "m", bpmnXml = "<bpmn/>", status = ProcessModelStatus.QUEUED,
        analysisEndpoint = request.endpoint, analysisOptionsJson = optionsJson, analysisResultJson = null,
        amountOfRetries = null, totalElements = null, criticalElementCount = null, errorMessage = null,
        createdAt = "", updatedAt = "", bpmnVersion = 1
    )

    private fun storedOptions(): String {
        val stored = argumentCaptor<String>()
        verify(repository).markQueued(any(), any(), stored.capture())
        return stored.firstValue
    }

    @Test
    fun `the stored analysis options never contain the api key`() {
        runner.enqueue(request)

        val json = storedOptions()
        assertFalse(json.contains(key), json)
        assertTrue(json.contains("some/model"), "the other options are still stored: $json")
    }

    @Test
    fun `the job runs with the key that was given at enqueue time and forgets it afterwards`() {
        // the analysis fails on purpose: the point is which llmProps it was called with
        whenever(analysisService.analyzePromptEngineering(any(), any(), any(), any(), any())).thenThrow(RuntimeException("boom"))
        // the row only exists once markQueued has stored the options, so the worker reads them back from there
        whenever(repository.getById(7L)).thenAnswer { modelWith(storedOptions()) }

        runner.enqueue(request)

        val used = argumentCaptor<LlmConfig.Companion.LlmPropsOverride>()
        verify(analysisService, timeout(5000)).analyzePromptEngineering(any(), used.capture(), any(), any(), any())
        assertEquals(key, used.firstValue.apiKey)
        assertEquals("some/model", used.firstValue.modelName)

        verify(repository, timeout(5000)).markError(eq(7L), any())
        val keys = ProcessModelJobRunner::class.java.getDeclaredField("apiKeys").apply { isAccessible = true }
            .get(runner) as ConcurrentHashMap<*, *>
        // markError runs before the worker's finally block, so give it the moment it needs
        val deadline = System.currentTimeMillis() + 5000
        while (keys.isNotEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertTrue(keys.isEmpty())
    }

    @Test
    fun `a rejected model is stored as a readable message without the provider's raw body`() {
        whenever(analysisService.analyzePromptEngineering(any(), any(), any(), any(), any())).thenThrow(
            InvalidRequestException("""{"error":{"message":"gpt-3.5-turbo is not a valid model ID","code":400},"user_id":"user_2abc"}""")
        )
        whenever(repository.getById(7L)).thenAnswer { modelWith(storedOptions()) }

        runner.enqueue(request)

        verify(repository, timeout(5000)).markError(7L, "OpenRouter rejected the request: gpt-3.5-turbo is not a valid model ID")
    }

    @Test
    fun `an unexpected exception is stored as a generic message that does not leak its text`() {
        whenever(analysisService.analyzePromptEngineering(any(), any(), any(), any(), any())).thenThrow(RuntimeException("jdbc:postgresql://db/gripl password=hunter2"))
        whenever(repository.getById(7L)).thenAnswer { modelWith(storedOptions()) }

        runner.enqueue(request)

        val message = argumentCaptor<String>()
        verify(repository, timeout(5000)).markError(eq(7L), message.capture())
        assertFalse(message.firstValue.contains("hunter2"), message.firstValue)
    }

    @Test
    fun `a deleted custom endpoint is reported with its own explanation`() {
        val custom = request.copy(endpoint = "/gdpr/analysis/custom/42")
        whenever(repository.getById(7L)).thenAnswer {
            modelWith(storedOptions()).copy(analysisEndpoint = custom.endpoint)
        }

        runner.enqueue(custom)

        verify(repository, timeout(5000)).markError(7L, "Custom analysis endpoint 42 no longer exists")
    }

    @Test
    fun `after a restart queued and running jobs are set to error and nothing is resumed`() {
        whenever(repository.resetInterruptedToError(any())).thenReturn(2)

        runner.recoverOnStartup()

        verify(repository).resetInterruptedToError(INTERRUPTED_MESSAGE)
        verify(repository, never()).getIdsByStatus(any())
        verify(analysisService, never()).analyzePromptEngineering(any(), any(), any(), any(), any())
        assertTrue(INTERRUPTED_MESSAGE.contains("queue the analysis again"))
    }
}
