package de.mertendieckmann.griplbackend.repository

import de.mertendieckmann.griplbackend.model.dto.ProcessModel
import de.mertendieckmann.griplbackend.model.dto.ProcessModelListItemDto
import de.mertendieckmann.griplbackend.model.dto.ProcessModelStatus
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class ProcessModelRepository(
    private val jdbc: JdbcTemplate
) {

    private val fullMapper = RowMapper { rs, _ ->
        ProcessModel.fromRow(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            bpmnXml = rs.getString("bpmn_xml"),
            status = rs.getString("status"),
            analysisEndpoint = rs.getString("analysis_endpoint"),
            analysisOptionsJson = rs.getString("analysis_options"),
            analysisResultJson = rs.getString("analysis_result"),
            amountOfRetries = rs.getObject("amount_of_retries") as Int?,
            totalElements = rs.getObject("total_elements") as Int?,
            criticalElementCount = rs.getObject("critical_element_count") as Int?,
            errorMessage = rs.getString("error_message"),
            createdAt = rs.getString("created_at"),
            updatedAt = rs.getString("updated_at")
        )
    }

    private val listItemMapper = RowMapper { rs, _ ->
        ProcessModelListItemDto(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            status = ProcessModelStatus.fromString(rs.getString("status")),
            analysisEndpoint = rs.getString("analysis_endpoint"),
            totalElements = rs.getObject("total_elements") as Int?,
            criticalElementCount = rs.getObject("critical_element_count") as Int?,
            errorMessage = rs.getString("error_message"),
            createdAt = rs.getString("created_at"),
            updatedAt = rs.getString("updated_at")
        )
    }

    // ── Owner-scoped access ────────────────────────────────────────────────
    // Process models are private to the auth-service user (JWT `sub`) that
    // uploaded them; every path reachable from ProcessModelController goes
    // through one of these. Rows with a NULL owner (pre-V9) match nobody.

    fun create(name: String, bpmnXml: String, ownerUserId: String): Long {
        val sql = "INSERT INTO process_model (name, bpmn_xml, owner_user_id) VALUES (?, ?, ?) RETURNING id"
        return jdbc.queryForObject(sql, Long::class.java, name, bpmnXml, ownerUserId)!!
    }

    fun listByOwner(ownerUserId: String): List<ProcessModelListItemDto> {
        val sql = """
            SELECT id, name, status, analysis_endpoint, total_elements, critical_element_count, error_message, created_at, updated_at
            FROM process_model
            WHERE owner_user_id = ?
            ORDER BY created_at DESC
        """.trimIndent()
        return jdbc.query(sql, listItemMapper, ownerUserId)
    }

    fun getByIdAndOwner(id: Long, ownerUserId: String): ProcessModel? {
        val sql = "SELECT * FROM process_model WHERE id = ? AND owner_user_id = ?"
        return jdbc.query(sql, fullMapper, id, ownerUserId).firstOrNull()
    }

    /** The subset of [ids] that exist and belong to [ownerUserId]. */
    fun filterOwnedIds(ids: List<Long>, ownerUserId: String): List<Long> {
        if (ids.isEmpty()) return emptyList()
        val inSql = ids.joinToString(",")
        val sql = "SELECT id FROM process_model WHERE id IN ($inSql) AND owner_user_id = ?"
        val owned = jdbc.query(sql, { rs, _ -> rs.getLong("id") }, ownerUserId).toSet()
        return ids.filter { it in owned }
    }

    fun deleteIfNotRunning(id: Long, ownerUserId: String): Boolean {
        val sql = "DELETE FROM process_model WHERE id = ? AND owner_user_id = ? AND status <> 'RUNNING'"
        return jdbc.update(sql, id, ownerUserId) > 0
    }

    // ── Background job runner only ─────────────────────────────────────────
    // ProcessModelJobRunner is a detached worker with no request/user context;
    // the ids it receives were already filtered to the caller's own models at
    // enqueue time (ProcessModelController.analyzeProcessModels), and queued
    // ids are resumed after a restart. Never call this from a controller.

    fun getById(id: Long): ProcessModel? {
        val sql = "SELECT * FROM process_model WHERE id = ?"
        return jdbc.query(sql, fullMapper, id).firstOrNull()
    }

    fun markQueued(id: Long, endpoint: String, optionsJson: String) {
        val sql = """
            UPDATE process_model
            SET status = 'QUEUED', analysis_endpoint = ?, analysis_options = ?::jsonb,
                error_message = NULL, updated_at = now()
            WHERE id = ?
        """.trimIndent()
        val options = PGobject().apply { type = "jsonb"; value = optionsJson }
        jdbc.update(sql, endpoint, options, id)
    }

    fun markRunning(id: Long) {
        jdbc.update("UPDATE process_model SET status = 'RUNNING', updated_at = now() WHERE id = ?", id)
    }

    fun markDone(id: Long, resultJson: String, criticalCount: Int, totalElements: Int, amountOfRetries: Int?) {
        val sql = """
            UPDATE process_model
            SET status = 'DONE', analysis_result = ?::jsonb, critical_element_count = ?,
                total_elements = ?, amount_of_retries = ?, error_message = NULL, updated_at = now()
            WHERE id = ?
        """.trimIndent()
        val result = PGobject().apply { type = "jsonb"; value = resultJson }
        jdbc.update(sql, result, criticalCount, totalElements, amountOfRetries, id)
    }

    fun markError(id: Long, message: String) {
        val sql = "UPDATE process_model SET status = 'ERROR', error_message = ?, updated_at = now() WHERE id = ?"
        jdbc.update(sql, message, id)
    }

    fun getIdsByStatus(status: ProcessModelStatus): List<Long> {
        return jdbc.query(
            "SELECT id FROM process_model WHERE status = ? ORDER BY created_at ASC",
            { rs, _ -> rs.getLong("id") },
            status.value
        )
    }

    fun resetStaleRunningToError(message: String): Int {
        return jdbc.update(
            "UPDATE process_model SET status = 'ERROR', error_message = ?, updated_at = now() WHERE status = 'RUNNING'",
            message
        )
    }
}
