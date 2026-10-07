package de.mertendieckmann.griplbackend.repository

import de.mertendieckmann.griplbackend.model.dto.CustomAnalysisEndpoint
import de.mertendieckmann.griplbackend.model.dto.CustomAnalysisResponseType
import de.mertendieckmann.griplbackend.model.dto.RagMode
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class CustomAnalysisEndpointRepository(
    private val jdbc: JdbcTemplate
) {

    private val mapper = RowMapper { rs, _ ->
        CustomAnalysisEndpoint.fromRow(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            promptText = rs.getString("prompt_text"),
            responseType = rs.getString("response_type"),
            ragEnabled = rs.getBoolean("rag_enabled"),
            ragMode = rs.getString("rag_mode"),
            createdAt = rs.getString("created_at"),
            updatedAt = rs.getString("updated_at")
        )
    }

    // ── Owner-scoped access ────────────────────────────────────────────────
    // Custom endpoints are private to the user that created them. Rows with a
    // NULL owner (pre-V10) match nobody.

    fun create(
        name: String,
        promptText: String,
        responseType: CustomAnalysisResponseType,
        ragEnabled: Boolean,
        ragMode: RagMode?,
        ownerUserId: String
    ): Long {
        val sql = """
            INSERT INTO custom_analysis_endpoint (name, prompt_text, response_type, rag_enabled, rag_mode, owner_user_id)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()
        return jdbc.queryForObject(
            sql, Long::class.java, name, promptText, responseType.value, ragEnabled, ragMode?.value, ownerUserId
        )!!
    }

    fun listByOwner(ownerUserId: String): List<CustomAnalysisEndpoint> {
        return jdbc.query(
            "SELECT * FROM custom_analysis_endpoint WHERE owner_user_id = ? ORDER BY created_at DESC",
            mapper, ownerUserId
        )
    }

    fun getByIdAndOwner(id: Long, ownerUserId: String): CustomAnalysisEndpoint? {
        return jdbc.query(
            "SELECT * FROM custom_analysis_endpoint WHERE id = ? AND owner_user_id = ?",
            mapper, id, ownerUserId
        ).firstOrNull()
    }

    fun deleteByOwner(id: Long, ownerUserId: String): Boolean {
        return jdbc.update(
            "DELETE FROM custom_analysis_endpoint WHERE id = ? AND owner_user_id = ?", id, ownerUserId
        ) > 0
    }

    // ── Internal lookups only ──────────────────────────────────────────────
    // For code that runs without a request/user context (the process-model job
    // runner, the evaluator's endpoint-type check). Whoever hands such code an
    // endpoint id must already have checked ownership at the request boundary
    // (see ControllerUtils.requireOwnedCustomEndpoint). Never call from a
    // controller path that serves user-supplied ids.

    fun getById(id: Long): CustomAnalysisEndpoint? {
        return jdbc.query("SELECT * FROM custom_analysis_endpoint WHERE id = ?", mapper, id).firstOrNull()
    }
}
