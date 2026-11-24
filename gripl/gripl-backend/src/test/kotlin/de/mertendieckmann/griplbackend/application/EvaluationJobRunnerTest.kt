package de.mertendieckmann.griplbackend.application

import de.mertendieckmann.griplbackend.config.LlmConfig
import de.mertendieckmann.griplbackend.evaluation.MultiEvaluationRunner
import de.mertendieckmann.griplbackend.evaluation.metrics.MetricsAccumulator
import de.mertendieckmann.griplbackend.model.dto.EvaluationMetadataReport
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportError
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportStepInfo
import de.mertendieckmann.griplbackend.model.dto.ModelReportEnvelope
import de.mertendieckmann.griplbackend.model.dto.ModelRunConfig
import de.mertendieckmann.griplbackend.model.dto.MultiEvaluationRequest
import de.mertendieckmann.griplbackend.model.dto.ResumeState
import de.mertendieckmann.griplbackend.model.dto.RunModelKey
import de.mertendieckmann.griplbackend.model.dto.TestCaseReport
import de.mertendieckmann.griplbackend.model.evaluation.EvaluationMetrics
import de.mertendieckmann.griplbackend.repository.EvaluationRunRepository
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DuplicateKeyException
import tools.jackson.module.kotlin.jacksonObjectMapper

class EvaluationJobRunnerTest {

    private val key = "sk-or-v1-secret-key"
    private val repository = mock<EvaluationRunRepository>()
    private val multiRunner = mock<MultiEvaluationRunner>()
    private val runner = EvaluationJobRunner(repository, multiRunner, jacksonObjectMapper())

    private val storedRequest = MultiEvaluationRequest(
        defaultEvaluationEndpoint = "/gdpr/analysis/prompt-engineering",
        models = listOf(
            ModelRunConfig("a", llmProps = LlmConfig.Companion.LlmPropsOverride(modelName = "model/a")),
            ModelRunConfig("b", llmProps = LlmConfig.Companion.LlmPropsOverride(modelName = "model/b"))
        ),
        seed = 1234,
        datasets = listOf(5),
        repetitions = 3
    )
    private val runRequest = storedRequest.copy(
        models = storedRequest.models.map { it.copy(llmProps = it.llmProps!!.copy(apiKey = key)) }
    )

    private val metadata = EvaluationMetadataReport(
        modelLabels = listOf("a", "b"), modelTemperatures = listOf(null, null), modelTopPs = listOf(null, null),
        datasets = emptyList(), totalTestCases = 10, defaultEvaluationEndpoint = storedRequest.defaultEvaluationEndpoint
    )
    private val metrics = EvaluationMetrics(
        truePositives = 1, falsePositives = 0, falseNegatives = 0, trueNegatives = 2, isSuccessful = true
    )
    private val testCase = TestCaseReport(
        testCaseId = 7L, imageSrc = "url", correctActivityIds = listOf("x"), falsePositiveIds = emptyList(),
        falseNegativeIds = emptyList(), expectedNamesWithIds = emptyList(), actualNamesWithIds = emptyList(),
        isSuccessful = true, result = emptyList(), metrics = metrics
    )

    init {
        whenever(repository.insertRun(any(), any())).thenReturn(42L)
    }

    @Test
    fun `a run stores its results as they arrive and never stores the api key`() {
        whenever(multiRunner.runAll(any(), any(), anyOrNull())).thenReturn(
            flowOf(
                ModelReportEnvelope("", metadata, 1),
                ModelReportEnvelope("a", EvaluationReportStepInfo("case", 7L, 1, 10), 1),
                ModelReportEnvelope("a", testCase, 1),
                ModelReportEnvelope("a", EvaluationReportError(8L, 5L, "case 8", "boom"), 1),
                ModelReportEnvelope("a", MetricsAccumulator().toSummary(), 1)
            )
        )

        assertEquals(42L, runner.start("user-1", storedRequest, runRequest))

        verify(repository, timeout(2000)).markDone(42L)
        val storedJson = argumentCaptor<String>()
        verify(repository).insertRun(eq("user-1"), storedJson.capture())
        assertFalse(storedJson.firstValue.contains(key), storedJson.firstValue)
        assertTrue(storedJson.firstValue.contains("1234"), "the seed is stored")

        // The job runs with the key that only lives in memory.
        verify(multiRunner).runAll(eq(runRequest), eq("user-1"), isNull())
        // 10 test cases × 2 models × 3 repetitions
        verify(repository).saveMetadata(eq(42L), any(), eq(60))

        val payloads = argumentCaptor<String>()
        val metricsJson = argumentCaptor<String>()
        verify(repository).insertEvent(eq(42L), eq(1), eq("a"), eq("testCase"), eq(7L), payloads.capture(), metricsJson.capture())
        assertTrue(payloads.firstValue.contains("\"type\":\"testCase\""), payloads.firstValue)
        assertFalse(payloads.firstValue.contains("\"metrics\""), "metrics are not part of the report JSON")
        assertTrue(metricsJson.firstValue.contains("\"truePositives\":1"), metricsJson.firstValue)
        verify(repository).insertEvent(eq(42L), eq(1), eq("a"), eq("error"), eq(8L), any(), isNull())
        verify(repository).insertEvent(eq(42L), eq(1), eq("a"), eq("summary"), isNull(), any(), isNull())
        assertTrue(runner.currentSteps(42L).isEmpty())
    }

    @Test
    fun `a request that still contains an api key is not stored`() {
        assertThrows(IllegalArgumentException::class.java) {
            runner.start("user-1", runRequest, runRequest)
        }
        verify(repository, never()).insertRun(any(), any())
    }

    @Test
    fun `a second active run of the same user is rejected`() {
        whenever(repository.insertRun(any(), any())).thenThrow(DuplicateKeyException("active run"))
        assertThrows(ActiveEvaluationRunExistsException::class.java) {
            runner.start("user-1", storedRequest, runRequest)
        }
        verify(multiRunner, never()).runAll(any(), any(), anyOrNull())
    }

    @Test
    fun `cancelling keeps the results and marks the run cancelled`() {
        whenever(multiRunner.runAll(any(), any(), anyOrNull())).thenReturn(
            flow {
                emit(ModelReportEnvelope("a", testCase, 1))
                awaitCancellation()
            }
        )
        runner.start("user-1", storedRequest, runRequest)
        verify(repository, timeout(2000)).insertEvent(eq(42L), any(), any(), eq("testCase"), any(), any(), any())

        runBlocking { runner.cancel(42L) }

        verify(repository).markCancelled(42L)
        verify(repository, never()).markDone(any())
        verify(repository, never()).markInterrupted(any(), any())
    }

    @Test
    fun `resuming continues from what is stored`() {
        val resumeState = mapOf(RunModelKey(1, "a") to ResumeState(setOf(7L), listOf(metrics), finished = false))
        whenever(repository.markQueuedForResume(42L)).thenReturn(true)
        whenever(repository.prepareResume(42L)).thenReturn(resumeState)
        whenever(multiRunner.runAll(any(), any(), anyOrNull())).thenReturn(flowOf())

        assertTrue(runner.resume(42L, "user-1", runRequest))

        verify(repository, timeout(2000)).markDone(42L)
        verify(multiRunner).runAll(eq(runRequest), eq("user-1"), eq(resumeState))
    }

    @Test
    fun `a run stopped before anything was stored starts over when resumed`() {
        whenever(repository.markQueuedForResume(42L)).thenReturn(true)
        whenever(repository.prepareResume(42L)).thenReturn(emptyMap())
        whenever(multiRunner.runAll(any(), any(), anyOrNull())).thenReturn(flowOf())

        assertTrue(runner.resume(42L, "user-1", runRequest))

        verify(repository, timeout(2000)).markDone(42L)
        // No resume state: the metadata event is emitted again.
        verify(multiRunner).runAll(eq(runRequest), eq("user-1"), isNull())
    }

    @Test
    fun `a run that is not stopped cannot be resumed`() {
        whenever(repository.markQueuedForResume(42L)).thenReturn(false)

        assertFalse(runner.resume(42L, "user-1", runRequest))

        verify(repository, never()).prepareResume(any())
        verify(multiRunner, never()).runAll(any(), any(), anyOrNull())
    }

    @Test
    fun `after a restart active runs are set to interrupted`() {
        runner.recoverOnStartup()
        verify(repository).resetActiveToInterrupted(EVALUATION_INTERRUPTED_MESSAGE)
    }
}
