package de.mertendieckmann.griplbackend.evaluation

import de.mertendieckmann.griplbackend.evaluation.service.Evaluator
import de.mertendieckmann.griplbackend.evaluation.service.RagasEvaluationService
import de.mertendieckmann.griplbackend.model.dto.Dataset
import de.mertendieckmann.griplbackend.model.dto.EvaluationData
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportError
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportFatal
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportModelError
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportSummary
import de.mertendieckmann.griplbackend.model.dto.EvaluationRequest
import de.mertendieckmann.griplbackend.model.dto.ModelRunConfig
import de.mertendieckmann.griplbackend.model.dto.MultiEvaluationRequest
import de.mertendieckmann.griplbackend.repository.DatasetRepository
import de.mertendieckmann.griplbackend.repository.EvaluationDataRepository
import dev.langchain4j.exception.InvalidRequestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException

/**
 * An evaluation that fails must say so: a failed test case becomes an error event with a readable
 * message, a model whose test cases all failed a model error event, and an aborted run a last fatal event.
 */
class EvaluationFailureVisibilityTest {

    private val rejected = InvalidRequestException(
        """{"error":{"message":"gpt-3.5-turbo is not a valid model ID","code":400}}"""
    )
    private val readable = "OpenRouter rejected the request: gpt-3.5-turbo is not a valid model ID"

    private val repo = mock<EvaluationDataRepository>()
    private val evaluator = mock<Evaluator>()
    private val singleRunner = EvaluationRunner(repo, evaluator, mock<RagasEvaluationService>())

    private fun entry(id: Long) = EvaluationData(id = id, name = "case $id", bpmnXml = "<bpmn/>", expectedValues = emptyList(), datasetId = 5)

    @Test
    fun `a failing test case is reported with a readable message and a code`() = runBlocking {
        whenever(repo.getEvaluationDataByDatasetIdsOrAllForOwner(any(), any())).thenReturn(listOf(entry(1)))
        whenever(evaluator.evaluate(any(), any())).thenThrow(RuntimeException("Failed to evaluate BPMN XML at endpoint", rejected))

        val events = singleRunner.run(EvaluationRequest(datasets = listOf(5)), "user-1").toList()

        val error = events.filterIsInstance<EvaluationReportError>().single()
        assertEquals(readable, error.errorMessage)
        assertEquals("LLM_REQUEST_REJECTED", error.code)
        assertEquals(1L, error.testCaseId)
        assertEquals(1, events.filterIsInstance<EvaluationReportSummary>().single().error)
    }

    @Test
    fun `cancelling an evaluation is not turned into a test case error`() = runBlocking {
        whenever(repo.getEvaluationDataByDatasetIdsOrAllForOwner(any(), any())).thenReturn(listOf(entry(1)))
        whenever(evaluator.evaluate(any(), any())).thenThrow(CancellationException("stopped"))

        val events = singleRunner.run(EvaluationRequest(datasets = listOf(5)), "user-1").toList()

        assertFalse(events.any { it is EvaluationReportError })
    }

    private fun multiRunner(single: EvaluationRunner = singleRunner): MultiEvaluationRunner {
        val datasets = mock<DatasetRepository>()
        whenever(datasets.getDatasetsByIdsAndOwner(any(), any())).thenReturn(listOf(Dataset(5, "d", null, "now", "now")))
        val data = mock<EvaluationDataRepository>()
        whenever(data.countEvaluationDataForDatasetsAndOwner(any(), any())).thenReturn(2)
        return MultiEvaluationRunner(single, datasets, data)
    }

    private val request = MultiEvaluationRequest(
        defaultEvaluationEndpoint = "endpoint", models = listOf(ModelRunConfig(label = "Model 1")), seed = 1, datasets = listOf(5)
    )

    @Test
    fun `when every test case of a model fails the model gets an error event`() = runBlocking {
        whenever(repo.getEvaluationDataByDatasetIdsOrAllForOwner(any(), any())).thenReturn(listOf(entry(1), entry(2)))
        whenever(evaluator.evaluate(any(), any())).thenThrow(RuntimeException("wrapped", rejected))

        val reports = multiRunner().runAll(request, "user-1").toList().map { it.report }

        val modelError = reports.filterIsInstance<EvaluationReportModelError>().single()
        assertEquals(2, modelError.totalTestCases)
        assertEquals(2, modelError.failedTestCases)
        assertEquals("LLM_REQUEST_REJECTED", modelError.code)
        assertEquals("All 2 test cases failed: $readable", modelError.errorMessage)
        assertNotNull(modelError.requestId)
        assertEquals(2, reports.filterIsInstance<EvaluationReportError>().size)
    }

    @Test
    fun `a model with some failing test cases gets no model error event`() = runBlocking {
        val partial = mock<EvaluationRunner>()
        whenever(partial.run(any(), any())).thenReturn(flow {
            emit(EvaluationReportError(1, 5, "case 1", "boom"))
            emit(de.mertendieckmann.griplbackend.model.dto.EvaluationReportStepInfo("case 2", 2, 2, 2))
        })

        val reports = multiRunner(partial).runAll(request, "user-1").toList().map { it.report }

        assertTrue(reports.filterIsInstance<EvaluationReportModelError>().isNotEmpty(), "no test case succeeded, so the model failed")
        val someSucceeded = mock<EvaluationRunner>()
        whenever(someSucceeded.run(any(), any())).thenReturn(flow {
            emit(EvaluationReportError(1, 5, "case 1", "boom"))
            emit(testCase(2))
        })
        val mixed = multiRunner(someSucceeded).runAll(request, "user-1").toList().map { it.report }
        assertFalse(mixed.any { it is EvaluationReportModelError })
    }

    private fun testCase(id: Long) = de.mertendieckmann.griplbackend.model.dto.TestCaseReport(
        testCaseId = id, testCaseName = "case $id", datasetId = 5, imageSrc = "", correctActivityIds = emptyList(),
        falsePositiveIds = emptyList(), falseNegativeIds = emptyList(), expectedNamesWithIds = emptyList(),
        actualNamesWithIds = emptyList(), isSuccessful = true, result = emptyList(), amountOfRetries = 0,
        ragMetrics = null, ragPromptContext = null, perElementType = emptyMap()
    )

    @Test
    fun `a failure that stops the run ends the stream with a fatal event instead of cutting it off`() = runBlocking {
        val broken = mock<EvaluationRunner>()
        whenever(broken.run(any(), any())).thenReturn(flow { throw RuntimeException("db is gone", rejected) })

        val reports = multiRunner(broken).runAll(request, "user-1").toList().map { it.report }

        val fatal = assertInstanceOf(EvaluationReportFatal::class.java, reports.last())
        assertEquals("LLM_REQUEST_REJECTED", fatal.code)
        assertEquals(readable, fatal.errorMessage)
        assertNotNull(fatal.requestId)
    }

    @Test
    fun `an evaluation without models is rejected with a 400`() {
        val exception = assertThrows(ResponseStatusException::class.java) {
            multiRunner().runAll(request.copy(models = emptyList()), "user-1")
        }
        assertEquals(400, exception.statusCode.value())
    }
}
