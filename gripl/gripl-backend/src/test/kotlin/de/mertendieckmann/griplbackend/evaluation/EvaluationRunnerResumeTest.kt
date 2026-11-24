package de.mertendieckmann.griplbackend.evaluation

import de.mertendieckmann.griplbackend.evaluation.service.Evaluator
import de.mertendieckmann.griplbackend.evaluation.service.RagasEvaluationService
import de.mertendieckmann.griplbackend.model.dto.EvaluationData
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportError
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportStepInfo
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportSummary
import de.mertendieckmann.griplbackend.model.dto.EvaluationRequest
import de.mertendieckmann.griplbackend.model.dto.ResumeState
import de.mertendieckmann.griplbackend.model.evaluation.EvaluationMetrics
import de.mertendieckmann.griplbackend.repository.EvaluationDataRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** A resumed run only evaluates what is missing, but its summary covers every test case. */
class EvaluationRunnerResumeTest {

    private val repo = mock<EvaluationDataRepository>()
    private val evaluator = mock<Evaluator>()
    private val runner = EvaluationRunner(repo, evaluator, mock<RagasEvaluationService>())
    private val request = EvaluationRequest(datasets = listOf(5))

    private fun entry(id: Long) = EvaluationData(id, "case $id", "<bpmn id=\"$id\"/>", emptyList(), 5L)

    private val finishedMetrics = EvaluationMetrics(
        truePositives = 2, falsePositives = 1, falseNegatives = 0, trueNegatives = 3, isSuccessful = true
    )

    @Test
    fun `only test cases that are not finished are evaluated and the summary includes the finished ones`() {
        runBlocking {
            whenever(repo.getEvaluationDataByDatasetIdsOrAllForOwner(eq(listOf(5)), eq("user-1")))
                .thenReturn(listOf(entry(1), entry(2), entry(3)))
            whenever(evaluator.evaluate(any(), any())).thenThrow(RuntimeException("LLM unavailable"))

            val resume = ResumeState(completedTestCaseIds = setOf(1L, 2L), completedMetrics = listOf(finishedMetrics, finishedMetrics))
            val events = runner.run(request, "user-1", resume).toList()

            verify(evaluator).evaluate(eq("<bpmn id=\"3\"/>"), any())
            verify(evaluator, never()).evaluate(eq("<bpmn id=\"1\"/>"), any())
            verify(evaluator, never()).evaluate(eq("<bpmn id=\"2\"/>"), any())

            val step = events.filterIsInstance<EvaluationReportStepInfo>().single()
            assertEquals(3L, step.currentTestCaseId)
            assertEquals(3, step.currentTestCaseNumber)
            assertEquals(3, step.totalTestCases)
            assertEquals(3L, events.filterIsInstance<EvaluationReportError>().single().testCaseId)

            val summary = events.filterIsInstance<EvaluationReportSummary>().single()
            assertEquals(3, summary.total)
            assertEquals(2, summary.passed)
            assertEquals(1, summary.error)
            assertEquals(4, summary.totalTruePositives)
            assertEquals(2, summary.totalFalsePositives)
            assertEquals(6, summary.totalTrueNegatives)
        }
    }

    @Test
    fun `without resume state every test case is evaluated`() {
        runBlocking {
            whenever(repo.getEvaluationDataByDatasetIdsOrAllForOwner(eq(listOf(5)), eq("user-1")))
                .thenReturn(listOf(entry(1), entry(2)))
            whenever(evaluator.evaluate(any(), any())).thenThrow(RuntimeException("LLM unavailable"))

            val events = runner.run(request, "user-1").toList()

            assertEquals(2, events.filterIsInstance<EvaluationReportError>().size)
            assertEquals(2, events.filterIsInstance<EvaluationReportSummary>().single().total)
        }
    }
}
