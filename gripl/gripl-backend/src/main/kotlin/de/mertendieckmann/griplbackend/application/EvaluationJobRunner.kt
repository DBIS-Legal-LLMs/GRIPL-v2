package de.mertendieckmann.griplbackend.application

import de.mertendieckmann.griplbackend.evaluation.MultiEvaluationRunner
import de.mertendieckmann.griplbackend.model.dto.EvaluationMetadataReport
import de.mertendieckmann.griplbackend.model.dto.EvaluationReport
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportError
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportStepInfo
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportSummary
import de.mertendieckmann.griplbackend.model.dto.ModelReportEnvelope
import de.mertendieckmann.griplbackend.model.dto.MultiEvaluationRequest
import de.mertendieckmann.griplbackend.model.dto.ResumeState
import de.mertendieckmann.griplbackend.model.dto.RunModelKey
import de.mertendieckmann.griplbackend.model.dto.TestCaseReport
import de.mertendieckmann.griplbackend.repository.EvaluationRunRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.ConcurrentHashMap

internal const val EVALUATION_INTERRUPTED_MESSAGE =
    "Interrupted by application restart, resume the run to evaluate the remaining test cases"

/** The user already has a QUEUED or RUNNING evaluation. */
class ActiveEvaluationRunExistsException : RuntimeException("An evaluation is already running")

/**
 * Runs evaluations as background jobs that do not depend on the browser connection.
 * Every testCase, error and summary event is stored as it arrives, so a cancelled,
 * failed or interrupted run keeps its partial results and can be resumed.
 *
 * The caller's OpenRouter API key is only held in memory by the running job (it is part
 * of the request the job closes over) and is never written to the database. A run that
 * is active when the application restarts is set to INTERRUPTED; resuming it fetches the
 * key again.
 */
@Service
class EvaluationJobRunner(
    private val repository: EvaluationRunRepository,
    private val multiEvaluationRunner: MultiEvaluationRunner,
    private val objectMapper: ObjectMapper
) {
    private val log = KotlinLogging.logger { }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("evaluation-job-runner"))
    private val jobs = ConcurrentHashMap<Long, Job>()
    private val cancelRequested = ConcurrentHashMap.newKeySet<Long>()
    /** Test cases being evaluated right now, per run. Keyed by run number, model label and test case id. */
    private val currentSteps = ConcurrentHashMap<Long, ConcurrentHashMap<String, ModelReportEnvelope>>()

    @EventListener(ApplicationReadyEvent::class)
    fun recoverOnStartup() {
        val resetCount = repository.resetActiveToInterrupted(EVALUATION_INTERRUPTED_MESSAGE)
        if (resetCount > 0) {
            log.warn { "Set $resetCount queued or running evaluation run(s) to INTERRUPTED after restart" }
        }
    }

    @PreDestroy
    fun shutdown() {
        // Runs that are still active are set to INTERRUPTED by recoverOnStartup on the next start.
        scope.cancel()
    }

    /**
     * Stores [storedRequest] (which must not contain an API key) and starts the job with
     * [runRequest] (resolved, with the key). Both must carry the same seed, so a resume
     * reproduces the seeds per repetition.
     */
    fun start(ownerUserId: String, storedRequest: MultiEvaluationRequest, runRequest: MultiEvaluationRequest): Long {
        require(storedRequest.models.none { it.llmProps?.apiKey != null }) { "The stored request must not contain an API key" }
        require(storedRequest.seed != null && storedRequest.seed == runRequest.seed) { "The request needs a fixed seed" }
        val id = try {
            repository.insertRun(ownerUserId, objectMapper.writeValueAsString(storedRequest))
        } catch (e: org.springframework.dao.DuplicateKeyException) {
            throw ActiveEvaluationRunExistsException()
        }
        launch(id, ownerUserId, runRequest, resume = null)
        return id
    }

    /** Resumes a CANCELLED, INTERRUPTED or ERROR run. Returns false if the run is not resumable. */
    fun resume(id: Long, ownerUserId: String, runRequest: MultiEvaluationRequest): Boolean {
        val queued = try {
            repository.markQueuedForResume(id)
        } catch (e: org.springframework.dao.DuplicateKeyException) {
            throw ActiveEvaluationRunExistsException()
        }
        if (!queued) return false
        val resume = try {
            repository.prepareResume(id)
        } catch (e: Exception) {
            repository.markError(id, e.message ?: "Could not prepare the resume")
            throw e
        }
        // Stopped before anything was stored (not even the metadata): run it from the start.
        val startOver = resume.isEmpty() && repository.getById(id)?.metadataJson == null
        launch(id, ownerUserId, runRequest, if (startOver) null else resume)
        return true
    }

    /** Stops the run; what is finished stays stored. Returns when the run is no longer active. */
    suspend fun cancel(id: Long) {
        val job = jobs[id]
        if (job == null) {
            // No job in this process (e.g. it just ended): only fix a status that is still active.
            repository.markCancelled(id)
            return
        }
        cancelRequested.add(id)
        job.cancelAndJoin()
    }

    /** stepInfo envelopes of the test cases the run is evaluating right now. */
    fun currentSteps(id: Long): List<ModelReportEnvelope> =
        currentSteps[id]?.values?.sortedBy { (it.report as EvaluationReportStepInfo).currentTestCaseNumber }.orEmpty()

    private fun launch(id: Long, ownerUserId: String, runRequest: MultiEvaluationRequest, resume: Map<RunModelKey, ResumeState>?) {
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                repository.markRunning(id)
                multiEvaluationRunner.runAll(runRequest, ownerUserId, resume).collect { envelope ->
                    handle(id, runRequest, envelope)
                }
                repository.markDone(id)
                log.info { "Evaluation run $id finished" }
            } catch (e: CancellationException) {
                if (id in cancelRequested) {
                    repository.markCancelled(id)
                    log.info { "Evaluation run $id cancelled" }
                } else {
                    repository.markInterrupted(id, EVALUATION_INTERRUPTED_MESSAGE)
                }
                throw e
            } catch (e: Exception) {
                log.error(e) { "Evaluation run $id failed" }
                repository.markError(id, e.message ?: "Evaluation failed")
            } finally {
                jobs.remove(id)
                cancelRequested.remove(id)
                currentSteps.remove(id)
            }
        }
        jobs[id] = job
        job.start()
    }

    private fun handle(id: Long, request: MultiEvaluationRequest, envelope: ModelReportEnvelope) {
        when (val report = envelope.report) {
            is EvaluationMetadataReport -> {
                val total = report.totalTestCases * request.models.size * request.repetitions.coerceAtLeast(1)
                repository.saveMetadata(id, toJson(report), total)
            }
            is EvaluationReportStepInfo -> {
                currentSteps.computeIfAbsent(id) { ConcurrentHashMap() }[
                    stepKey(envelope, report.currentTestCaseId)
                ] = envelope
            }
            is TestCaseReport -> {
                storeEvent(id, envelope, "testCase", report.testCaseId, report.metrics?.let { objectMapper.writeValueAsString(it) })
                currentSteps[id]?.remove(stepKey(envelope, report.testCaseId))
            }
            is EvaluationReportError -> {
                storeEvent(id, envelope, "error", report.testCaseId, null)
                currentSteps[id]?.remove(stepKey(envelope, report.testCaseId))
            }
            is EvaluationReportSummary -> storeEvent(id, envelope, "summary", null, null)
        }
    }

    private fun storeEvent(id: Long, envelope: ModelReportEnvelope, type: String, testCaseId: Long?, metricsJson: String?) {
        repository.insertEvent(
            runId = id,
            runNumber = envelope.runNumber,
            modelLabel = envelope.modelLabel,
            type = type,
            testCaseId = testCaseId,
            payloadJson = toJson(envelope.report),
            metricsJson = metricsJson
        )
    }

    /** Same JSON (incl. the "type" property) as the report has in the NDJSON stream. */
    private fun toJson(report: EvaluationReport): String =
        objectMapper.writerFor(EvaluationReport::class.java).writeValueAsString(report)

    private fun stepKey(envelope: ModelReportEnvelope, testCaseId: Long) =
        "${envelope.runNumber}|${envelope.modelLabel}|$testCaseId"
}
