package de.mertendieckmann.griplbackend.evaluation

import de.mertendieckmann.griplbackend.application.failure.FailureMapper
import de.mertendieckmann.griplbackend.application.failure.logFailure
import de.mertendieckmann.griplbackend.model.dto.EvaluationMetadataReport
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportError
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportFatal
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportModelError
import de.mertendieckmann.griplbackend.model.dto.TestCaseReport
import de.mertendieckmann.griplbackend.model.dto.EvaluationRequest
import de.mertendieckmann.griplbackend.model.dto.ModelReportEnvelope
import de.mertendieckmann.griplbackend.model.dto.MultiEvaluationRequest
import de.mertendieckmann.griplbackend.repository.DatasetRepository
import de.mertendieckmann.griplbackend.repository.EvaluationDataRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

class MultiEvaluationRunner(
    private val singleRunner: EvaluationRunner,
    private val datasetRepository: DatasetRepository,
    private val evaluationDataRepository: EvaluationDataRepository
) {
    private val log = KotlinLogging.logger {}

    /**
     * Failures never end the stream silently: a test case that fails is reported as an error event, a model whose
     * test cases all failed as a model error event, and anything that stops the whole run as a last fatal event.
     */
    fun runAll(request: MultiEvaluationRequest, ownerUserId: String): Flow<ModelReportEnvelope> {
        if (request.models.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one model to evaluate")
        return runAllModels(request, ownerUserId).catch { e ->
            if (e is CancellationException) throw e
            val requestId = FailureMapper.newRequestId()
            val failure = log.logFailure("Evaluation run", e, mapOf("userId" to ownerUserId), requestId)
            emit(ModelReportEnvelope("", EvaluationReportFatal(failure.message, failure.code, requestId), 1))
        }
    }

    private fun runAllModels(request: MultiEvaluationRequest, ownerUserId: String): Flow<ModelReportEnvelope> = flow {

        val baseSeed = request.seed ?: (System.currentTimeMillis() * (Math.random() * 10) % Int.MAX_VALUE).toInt()
        val repetitions = request.repetitions.coerceAtLeast(1)
        emit(ModelReportEnvelope("", createMetadata(request, baseSeed, repetitions, ownerUserId), 1))

        for (runNumber in 1..repetitions) {
            log.info { "Starting evaluation run $runNumber/$repetitions" }

            for ((index, model) in request.models.withIndex()) {
                val effectiveEndpoint = model.evaluationEndpoint ?: request.defaultEvaluationEndpoint
                log.info { "Run $runNumber - Starting model ${index + 1}/${request.models.size}: '${model.label}' @ $effectiveEndpoint" }

                val runSeed = deriveRunSeed(baseSeed, runNumber)

                val singleRequest = EvaluationRequest(
                    evaluationEndpoint = effectiveEndpoint,
                    llmProps = model.llmProps?.copy(seed = runSeed),
                    maxConcurrent = request.maxConcurrent,
                    datasets = request.datasets,
                    evaluationDataIds = request.evaluationDataIds,
                    useRag = request.useRag,
                    ragMode = request.ragMode,
                    evaluateRag = request.evaluateRag,
                    activitiesOnly = request.activitiesOnly
                )

                var succeeded = 0
                var failed = 0
                var firstError: EvaluationReportError? = null

                singleRunner.run(singleRequest, ownerUserId)
                    .map { event -> ModelReportEnvelope(model.label, event, runNumber) }
                    .collect { wrapped ->
                        when (val report = wrapped.report) {
                            is TestCaseReport -> succeeded++
                            is EvaluationReportError -> {
                                failed++
                                if (firstError == null) firstError = report
                            }
                            else -> {}
                        }
                        emit(wrapped)
                    }

                val firstFailure = firstError
                if (firstFailure != null && succeeded == 0) {
                    val requestId = FailureMapper.newRequestId()
                    log.error {
                        "Run $runNumber - all $failed test case(s) failed for model '${model.label}' " +
                            "[code=${firstFailure.code}, requestId=$requestId, userId=$ownerUserId, endpoint=$effectiveEndpoint, " +
                            "model=${model.llmProps?.modelName}]: ${firstFailure.errorMessage}"
                    }
                    emit(
                        ModelReportEnvelope(
                            model.label,
                            EvaluationReportModelError(
                                totalTestCases = failed,
                                failedTestCases = failed,
                                errorMessage = "All $failed test cases failed: ${firstFailure.errorMessage}",
                                code = firstFailure.code,
                                requestId = requestId
                            ),
                            runNumber
                        )
                    )
                } else if (firstFailure != null) {
                    log.warn { "Run $runNumber - $failed of ${succeeded + failed} test case(s) failed for model '${model.label}'" }
                }

                log.info { "Run $runNumber - Finished model '${model.label}': $succeeded succeeded, $failed failed" }
            }
        }
    }

    private fun createMetadata(
        request: MultiEvaluationRequest,
        seed: Int,
        repetitions: Int,
        ownerUserId: String
    ): EvaluationMetadataReport {
        val datasets = datasetRepository.getDatasetsByIdsAndOwner(request.datasets.map { it.toLong() }, ownerUserId)
        val totalTestCases = if (request.evaluationDataIds.isNotEmpty()) {
            request.evaluationDataIds.size
        } else {
            evaluationDataRepository.countEvaluationDataForDatasetsAndOwner(request.datasets.map { it.toLong() }, ownerUserId)
        }

        return EvaluationMetadataReport(
            modelLabels = request.models.map { it.label },
            modelTemperatures = request.models.map { it.llmProps?.temperature },
            modelTopPs = request.models.map { it.llmProps?.topP },
            datasets = datasets.map { EvaluationMetadataReport.DatasetInfo(it.id, it.name) },
            totalTestCases = totalTestCases,
            seed = seed,
            defaultEvaluationEndpoint = request.defaultEvaluationEndpoint,
            totalRepetitions = repetitions,
            activitiesOnly = request.activitiesOnly
        )
    }

    /**
     * Stable and deterministic derivation of a run-specific seed from a base seed and the run number.
     * The result is guaranteed to be in the range [1..Int.MAX_VALUE].
     */
    private fun deriveRunSeed(baseSeed: Int, runNumber: Int): Int {
        val digest = MessageDigest.getInstance("SHA-256")
        val payload = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
            .putInt(baseSeed)
            .putInt(runNumber)
            .array()
        val hash = digest.digest(payload)
        val raw = ByteBuffer.wrap(hash, 0, 4).order(ByteOrder.BIG_ENDIAN).int
        val position = raw and 0x7fffffff
        return if (position == 0) 1 else position
    }
}
