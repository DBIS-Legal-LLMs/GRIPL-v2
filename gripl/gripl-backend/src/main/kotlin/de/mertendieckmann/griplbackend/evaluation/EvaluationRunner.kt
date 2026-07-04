package de.mertendieckmann.griplbackend.evaluation
import de.mertendieckmann.griplbackend.model.evaluation.ClassificationSets
import de.mertendieckmann.griplbackend.application.BpmnExtractor
import de.mertendieckmann.griplbackend.evaluation.metrics.MetricsAccumulator
import de.mertendieckmann.griplbackend.evaluation.service.Evaluator
import de.mertendieckmann.griplbackend.evaluation.service.RagasEvaluationService
import de.mertendieckmann.griplbackend.model.dto.*
import de.mertendieckmann.griplbackend.model.evaluation.EvaluationMetrics
import de.mertendieckmann.griplbackend.model.evaluation.EvaluationOutcome
import de.mertendieckmann.griplbackend.model.evaluation.RagMetrics
import de.mertendieckmann.griplbackend.repository.EvaluationDataRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class EvaluationRunner(
    private val evaluationDataRepository: EvaluationDataRepository,
    private val evaluator: Evaluator,
    private val ragasEvaluationService: RagasEvaluationService
) {

    private val log = KotlinLogging.logger {}
    private val bpmnExtractor = BpmnExtractor()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun run(request: EvaluationRequest): Flow<EvaluationReport> {
        val metricsAccumulator = MetricsAccumulator()
        val startedCounter = AtomicInteger(0)

        val entriesFlow = (if (request.evaluationDataIds.isNotEmpty()) {
            evaluationDataRepository.getEvaluationDataByIds(request.evaluationDataIds)
        } else {
            evaluationDataRepository.getEvaluationDataByDatasetIdsOrAll(request.datasets)
        }).sortedBy { it.id }.asFlow()

        log.info { "Starting evaluation with endpoint=${request.evaluationEndpoint}; maxConcurrent=${request.maxConcurrent}; evaluateRag=${request.evaluateRag}" }

        return entriesFlow
            .flatMapMerge(concurrency = request.maxConcurrent.coerceAtLeast(1)) { entry ->
                flow {
                    val currentNumber = startedCounter.incrementAndGet()
                    emit(buildStepInfo(entry, currentNumber, entriesFlow.count()))

                    when (val outcome = evaluateSingleEntry(entry, request)) {
                        is EvaluationOutcome.Error -> {
                            metricsAccumulator.addError()
                            emit(outcome.errorReport)
                        }
                        is EvaluationOutcome.Success -> {
                            metricsAccumulator.add(outcome.metrics)
                            emit(outcome.testCaseReport)
                        }
                    }
                }
            }
            .onCompletion {
                emit(metricsAccumulator.toSummary())
            }
    }

    private suspend fun evaluateSingleEntry(
    entry: EvaluationData,
    evaluationRequest: EvaluationRequest
): EvaluationOutcome = try {
    evaluateEntryOrFailFast(entry, evaluationRequest)
} catch (e: Exception) {
    EvaluationOutcome.Error(
        EvaluationReportError(
            testCaseId = entry.id,
            datasetId = entry.datasetId,
            testCaseName = entry.name ?: "Test Case ${entry.id}",
            errorMessage = e.message ?: "Unbekannter Fehler aufgetreten"
        )
    )
}

private suspend fun evaluateEntryOrFailFast(
    entry: EvaluationData,
    evaluationRequest: EvaluationRequest
): EvaluationOutcome {

    val expectedValues = entry.expectedValues
    val actualResult = evaluator.evaluate(entry.bpmnXml, evaluationRequest)
    val actualValues = actualResult.expectedValues

    if (
        evaluationRequest.evaluateRag &&
        actualResult.analysisResponse.ragPromptContext == null
    ) {
        return EvaluationOutcome.Error(
            EvaluationReportError(
                testCaseId = entry.id,
                datasetId = entry.datasetId,
                testCaseName = entry.name ?: "Test Case ${entry.id}",
                errorMessage =
                    "RAG evaluation (evaluateRag=true) was requested, but endpoint " +
                        "'${evaluationRequest.evaluationEndpoint}' returned no RAG context — the endpoint " +
                        "does not support RAG (e.g. baseline) or useRag is false. " +
                        "Disable 'Evaluate RAG Quality' or use a RAG-capable endpoint with useRag=true."
            )
        )
    }

    val bpmnModel = parseBpmn(entry.bpmnXml)
    val bpmnElements = bpmnExtractor.extractBpmnElements(bpmnModel)

    // With activitiesOnly, scoring is restricted to activities
    val scopeIds: Set<String>? =
        if (evaluationRequest.activitiesOnly) {
            classifiableElementIds(
                bpmnModel,
                activitiesOnly = true
            )
        } else {
            null
        }

    val expectedActivityIds = expectedValues
        .map { it.value }
        .filter { scopeIds == null || it in scopeIds }

    val actualActivityIds = actualValues
        .map { it.value }
        .filter { scopeIds == null || it in scopeIds }

    // Display-name fallbacks for unnamed elements
    val labelOverrides = bpmnElements
        .mapNotNull { element ->
            element.derivedNameFromFlows()?.let {
                element.id to it
            }
        }
        .toMap() +
        actualResult.analysisResponse.criticalElements
            .mapNotNull { element ->
                element.name?.let {
                    element.id to it
                }
            }
            .toMap()

    val isMultiLabelEvaluation =
        evaluationRequest.evaluationEndpoint.contains(
            "multiclass",
            ignoreCase = true
        )

    val expectedComparisonValues =
        if (isMultiLabelEvaluation) {
            expectedValues
                .filter { scopeIds == null || it.value in scopeIds }
                .flatMap { expected ->
                    expected.classification.map { classification ->
                        "${expected.value}:$classification"
                    }
                }
        } else {
            expectedActivityIds
        }

    val actualComparisonValues =
        if (isMultiLabelEvaluation) {
            actualValues
                .filter { scopeIds == null || it.value in scopeIds }
                .flatMap { actual ->
                    actual.classification.map { classification ->
                        "${actual.value}:$classification"
                    }
                }
        } else {
            actualActivityIds
        }

    val classification: ClassificationSets<String> =
        computeClassificationSets(
            expectedComparisonValues,
            actualComparisonValues
    )
    val classificationIds: ClassificationSets<String> = ClassificationSets(
        truePositiveIds = classification.truePositiveIds
            .map { if (isMultiLabelEvaluation) it.substringBefore(":") else it }
            .distinct(),
        falsePositiveIds = classification.falsePositiveIds
            .map { if (isMultiLabelEvaluation) it.substringBefore(":") else it }
            .distinct(),
        falseNegativeIds = classification.falseNegativeIds
            .map { if (isMultiLabelEvaluation) it.substringBefore(":") else it }
            .distinct()
    )

    val perElementType = computePerElementTypeCounts(
        bpmnModel,
        classificationIds,
        evaluationRequest.activitiesOnly
    )

    if (
        evaluationRequest.evaluateRag &&
        actualResult.analysisResponse.ragContext.isNullOrEmpty()
    ) {
        log.warn {
            "RAG context missing or empty -> proceeding with evaluation " +
                "but metrics might be affected for ${entry.id}"
        }
    }

    val ragMetrics: RagMetrics? =
        if (
            evaluationRequest.evaluateRag &&
            actualActivityIds.isNotEmpty()
        ) {
            ragasEvaluationService.scoreTestCase(
                actualResult.analysisResponse,
                bpmnElements,
                scopeIds
            )
        } else {
            if (
                evaluationRequest.evaluateRag &&
                actualActivityIds.isEmpty()
            ) {
                log.info {
                    "Skipping RAGAS evaluation for ${entry.id}: " +
                        "LLM returned no critical elements, nothing to score faithfulness against"
                }
            }

            null
        }

    val trueNegativesCount =
        if (isMultiLabelEvaluation) {
            0
        } else {
            computeTrueNegativesCount(
                bpmnModel = bpmnModel,
                truePositivesCount = classification.truePositiveIds.size,
                falsePositivesCount = classification.falsePositiveIds.size,
                falseNegativesCount = classification.falseNegativeIds.size,
                activitiesOnly = evaluationRequest.activitiesOnly
            )
        }

    val amountOfRetries =
        actualResult.analysisResponse.amountOfRetries ?: 0

    val metrics = EvaluationMetrics(
        truePositives = classification.truePositiveIds.size,
        falsePositives = classification.falsePositiveIds.size,
        falseNegatives = classification.falseNegativeIds.size,
        trueNegatives = trueNegativesCount,
        isSuccessful =
            actualComparisonValues.toSet() ==
                expectedComparisonValues.toSet(),
        amountOfRetries = amountOfRetries,
        ragMetrics = ragMetrics,
        perElementType = perElementType
    )

    val testCaseReport = TestCaseReport(
        testCaseId = entry.id,
        testCaseName = entry.name,
        datasetId = entry.datasetId,
        imageSrc = buildPreviewUrl(
            testCaseId = entry.id,
            correctActivityIds = classificationIds.truePositiveIds,
            falsePositiveIds = classificationIds.falsePositiveIds,
            falseNegativeIds = classificationIds.falseNegativeIds
        ),
        correctActivityIds = classificationIds.truePositiveIds,
        falsePositiveIds = classificationIds.falsePositiveIds,
        falseNegativeIds = classificationIds.falseNegativeIds,
        expectedNamesWithIds = getNamesWithIds(
            bpmnModel,
            expectedActivityIds,
            labelOverrides
        ),
        actualNamesWithIds = getNamesWithIds(
            bpmnModel,
            actualActivityIds,
            labelOverrides
        ),
        isSuccessful = metrics.isSuccessful,
        result = actualValues,
        amountOfRetries = amountOfRetries,
        ragMetrics = ragMetrics,
        ragPromptContext = actualResult.analysisResponse.ragPromptContext,
        perElementType = perElementType
    )

    return EvaluationOutcome.Success(
        testCaseReport,
        metrics
    )
}
}