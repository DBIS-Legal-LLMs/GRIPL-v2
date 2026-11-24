package de.mertendieckmann.griplbackend.repository

import de.mertendieckmann.griplbackend.model.dto.EvaluationRunEvent
import de.mertendieckmann.griplbackend.model.dto.EvaluationRunStatus
import de.mertendieckmann.griplbackend.model.dto.ResumeState
import de.mertendieckmann.griplbackend.model.dto.RunModelKey
import de.mertendieckmann.griplbackend.model.evaluation.EvaluationMetrics
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

/** A stored evaluation run; [requestJson] never contains an API key. */
data class EvaluationRunRow(
    val id: Long,
    val ownerUserId: String,
    val name: String? = null,
    val status: EvaluationRunStatus,
    val requestJson: String,
    val metadataJson: String?,
    val totalTestCases: Int?,
    val completedTestCases: Int,
    val errorMessage: String?,
    val createdAt: String,
    val startedAt: String?,
    val finishedAt: String?
)

@Repository
class EvaluationRunRepository(
    private val jdbc: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {

    private val selectRun = """
        SELECT r.*,
               (SELECT count(*) FROM evaluation_run_event e
                WHERE e.run_id = r.id AND e.type IN ('testCase', 'error')) AS completed_test_cases
        FROM evaluation_run r
    """.trimIndent()

    private val runMapper = RowMapper { rs, _ ->
        // ISO 8601 (e.g. 2026-11-24T10:00:00.123Z), which every browser parses.
        fun iso(column: String): String? = rs.getTimestamp(column)?.toInstant()?.toString()
        EvaluationRunRow(
            id = rs.getLong("id"),
            ownerUserId = rs.getString("owner_user_id"),
            name = rs.getString("name"),
            status = EvaluationRunStatus.valueOf(rs.getString("status")),
            requestJson = rs.getString("request"),
            metadataJson = rs.getString("metadata"),
            totalTestCases = rs.getObject("total_test_cases") as Int?,
            completedTestCases = rs.getInt("completed_test_cases"),
            errorMessage = rs.getString("error_message"),
            createdAt = iso("created_at")!!,
            startedAt = iso("started_at"),
            finishedAt = iso("finished_at")
        )
    }

    private fun jsonb(json: String) = PGobject().apply { type = "jsonb"; value = json }

    // ── Owner-scoped access (controller) ───────────────────────────────────

    fun listByOwner(ownerUserId: String): List<EvaluationRunRow> =
        jdbc.query("$selectRun WHERE r.owner_user_id = ? ORDER BY r.created_at DESC, r.id DESC", runMapper, ownerUserId)

    fun getByIdAndOwner(id: Long, ownerUserId: String): EvaluationRunRow? =
        jdbc.query("$selectRun WHERE r.id = ? AND r.owner_user_id = ?", runMapper, id, ownerUserId).firstOrNull()

    /** Stored events with an id greater than [afterId], oldest first. The caller must own the run. */
    fun eventsAfter(runId: Long, ownerUserId: String, afterId: Long, limit: Int): List<EvaluationRunEvent> =
        jdbc.query(
            """
            SELECT e.id, e.model_label, e.run_number, e.payload
            FROM evaluation_run_event e
            JOIN evaluation_run r ON r.id = e.run_id
            WHERE e.run_id = ? AND r.owner_user_id = ? AND e.id > ?
            ORDER BY e.id
            LIMIT ?
            """.trimIndent(),
            { rs, _ ->
                EvaluationRunEvent(
                    id = rs.getLong("id"),
                    modelLabel = rs.getString("model_label"),
                    runNumber = rs.getInt("run_number"),
                    report = objectMapper.readTree(rs.getString("payload"))
                )
            },
            runId, ownerUserId, afterId, limit
        )

    /** Sets or (with null) removes the name of the run. Returns false if the run does not exist or is not the owner's. */
    fun rename(id: Long, ownerUserId: String, name: String?): Boolean =
        jdbc.update(
            "UPDATE evaluation_run SET name = ?, updated_at = now() WHERE id = ? AND owner_user_id = ?",
            name, id, ownerUserId
        ) > 0

    /** Deletes the run and its events unless it is active. Returns false if nothing was deleted. */
    fun deleteIfNotActive(id: Long, ownerUserId: String): Boolean =
        jdbc.update(
            "DELETE FROM evaluation_run WHERE id = ? AND owner_user_id = ? AND status NOT IN ('QUEUED', 'RUNNING')",
            id, ownerUserId
        ) > 0

    // ── Job runner access ──────────────────────────────────────────────────

    /**
     * Inserts a QUEUED run. Throws [org.springframework.dao.DuplicateKeyException]
     * if the owner already has an active run.
     */
    fun insertRun(ownerUserId: String, requestJson: String, name: String? = null): Long =
        jdbc.queryForObject(
            "INSERT INTO evaluation_run (owner_user_id, status, request, name) VALUES (?, 'QUEUED', ?, ?) RETURNING id",
            Long::class.java, ownerUserId, jsonb(requestJson), name
        )!!

    fun getById(id: Long): EvaluationRunRow? =
        jdbc.query("$selectRun WHERE r.id = ?", runMapper, id).firstOrNull()

    /**
     * Sets a stopped run back to QUEUED. Returns false if the run is not resumable.
     * Throws [org.springframework.dao.DuplicateKeyException] if the owner already has an active run.
     */
    fun markQueuedForResume(id: Long): Boolean =
        jdbc.update(
            """
            UPDATE evaluation_run
            SET status = 'QUEUED', error_message = NULL, finished_at = NULL, updated_at = now()
            WHERE id = ? AND status IN ('CANCELLED', 'INTERRUPTED', 'ERROR')
            """.trimIndent(),
            id
        ) > 0

    fun markRunning(id: Long) {
        jdbc.update(
            "UPDATE evaluation_run SET status = 'RUNNING', started_at = coalesce(started_at, now()), updated_at = now() WHERE id = ?",
            id
        )
    }

    fun saveMetadata(id: Long, metadataJson: String, totalTestCases: Int) {
        jdbc.update(
            "UPDATE evaluation_run SET metadata = ?, total_test_cases = ?, updated_at = now() WHERE id = ?",
            jsonb(metadataJson), totalTestCases, id
        )
    }

    fun insertEvent(
        runId: Long,
        runNumber: Int,
        modelLabel: String,
        type: String,
        testCaseId: Long?,
        payloadJson: String,
        metricsJson: String?
    ) {
        jdbc.update(
            """
            INSERT INTO evaluation_run_event (run_id, run_number, model_label, type, test_case_id, payload, metrics)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            runId, runNumber, modelLabel, type, testCaseId, jsonb(payloadJson), metricsJson?.let { jsonb(it) }
        )
    }

    fun markDone(id: Long) = finish(id, EvaluationRunStatus.DONE, null)

    fun markCancelled(id: Long) = finish(id, EvaluationRunStatus.CANCELLED, null)

    fun markInterrupted(id: Long, message: String) = finish(id, EvaluationRunStatus.INTERRUPTED, message)

    fun markError(id: Long, message: String) = finish(id, EvaluationRunStatus.ERROR, message)

    /** Only an active run is finished, so a late status update cannot overwrite a newer one. */
    private fun finish(id: Long, status: EvaluationRunStatus, message: String?) {
        jdbc.update(
            """
            UPDATE evaluation_run
            SET status = ?, error_message = ?, finished_at = now(), updated_at = now()
            WHERE id = ? AND status IN ('QUEUED', 'RUNNING')
            """.trimIndent(),
            status.name, message, id
        )
    }

    /** Active runs cannot survive a restart: the job and the user's API key were in memory only. */
    fun resetActiveToInterrupted(message: String): Int =
        jdbc.update(
            """
            UPDATE evaluation_run
            SET status = 'INTERRUPTED', error_message = ?, finished_at = now(), updated_at = now()
            WHERE status IN ('QUEUED', 'RUNNING')
            """.trimIndent(),
            message
        )

    /**
     * Prepares a resume: failed test cases are run again, so their error events are deleted,
     * together with the summary of every model/repetition that had one. Then returns what
     * is finished per model and repetition.
     */
    @Transactional
    fun prepareResume(runId: Long): Map<RunModelKey, ResumeState> {
        jdbc.update(
            """
            DELETE FROM evaluation_run_event s
            WHERE s.run_id = ? AND s.type = 'summary' AND EXISTS (
                SELECT 1 FROM evaluation_run_event e
                WHERE e.run_id = s.run_id AND e.type = 'error'
                  AND e.run_number = s.run_number AND e.model_label = s.model_label
            )
            """.trimIndent(),
            runId
        )
        jdbc.update("DELETE FROM evaluation_run_event WHERE run_id = ? AND type = 'error'", runId)

        val finished = jdbc.query(
            "SELECT run_number, model_label FROM evaluation_run_event WHERE run_id = ? AND type = 'summary'",
            { rs, _ -> RunModelKey(rs.getInt("run_number"), rs.getString("model_label")) },
            runId
        ).toSet()

        data class Completed(val key: RunModelKey, val testCaseId: Long, val metrics: EvaluationMetrics?)
        val completed = jdbc.query(
            """
            SELECT run_number, model_label, test_case_id, metrics
            FROM evaluation_run_event
            WHERE run_id = ? AND type = 'testCase'
            """.trimIndent(),
            { rs, _ ->
                Completed(
                    key = RunModelKey(rs.getInt("run_number"), rs.getString("model_label")),
                    testCaseId = rs.getLong("test_case_id"),
                    metrics = rs.getString("metrics")?.let { objectMapper.readValue(it, EvaluationMetrics::class.java) }
                )
            },
            runId
        )

        val keys = finished + completed.map { it.key }
        return keys.associateWith { key ->
            val ofKey = completed.filter { it.key == key }
            ResumeState(
                completedTestCaseIds = ofKey.map { it.testCaseId }.toSet(),
                completedMetrics = ofKey.mapNotNull { it.metrics },
                finished = key in finished
            )
        }
    }
}
