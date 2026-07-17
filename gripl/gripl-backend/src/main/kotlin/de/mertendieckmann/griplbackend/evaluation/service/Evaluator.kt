package de.mertendieckmann.griplbackend.evaluation.service

import de.mertendieckmann.griplbackend.model.dto.AnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.EvaluationRequest
import de.mertendieckmann.griplbackend.model.dto.ExpectedValue
import kotlin.jvm.Throws

/**
 * Result of evaluating a single BPMN test case against the analysis service.
 *
 * The full [AnalysisResponse] is preserved so downstream consumers (e.g. the
 * Ragas evaluator) can access per-element retrieval contexts and explanations
 * without making a second analysis call.
 */
data class EvaluationCallResult(
    val expectedValues: List<ExpectedValue>,
    val amountOfRetries: Int?,
    val analysisResponse: AnalysisResponse
)

interface Evaluator {

    /**
     * Evaluates the given BPMN XML using the specified evaluation request.
     */
    @Throws(Exception::class)
    suspend fun evaluate(bpmnXml: String, evaluationRequest: EvaluationRequest): EvaluationCallResult
}
