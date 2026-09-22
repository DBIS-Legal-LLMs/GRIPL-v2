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

    fun create(name: String, bpmnXml: String): Long {
        val sql = "INSERT INTO process_model (name, bpmn_xml) VALUES (?, ?) RETURNING id"
        return jdbc.queryForObject(sql, Long::class.java, name, bpmnXml)!!
    }

    fun listAll(): List<ProcessModelListItemDto> {
        val sql = """
            SELECT id, name, status, analysis_endpoint, total_elements, critical_element_count, error_message, created_at, updated_at
            FROM process_model
            ORDER BY created_at DESC
        """.trimIndent()
        return jdbc.query(sql, listItemMapper)
    }

    fun getById(id: Long): ProcessModel? {
        val sql = "SELECT * FROM process_model WHERE id = ?"
        return jdbc.query(sql, fullMapper, id).firstOrNull()
    }

    fun deleteIfNotRunning(id: Long): Boolean {
        val sql = "DELETE FROM process_model WHERE id = ? AND status <> 'RUNNING'"
        return jdbc.update(sql, id) > 0
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
