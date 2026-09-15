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

    fun create(
        name: String,
        promptText: String,
        responseType: CustomAnalysisResponseType,
        ragEnabled: Boolean,
        ragMode: RagMode?
    ): Long {
        val sql = """
            INSERT INTO custom_analysis_endpoint (name, prompt_text, response_type, rag_enabled, rag_mode)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()
        return jdbc.queryForObject(sql, Long::class.java, name, promptText, responseType.value, ragEnabled, ragMode?.value)!!
    }

    fun listAll(): List<CustomAnalysisEndpoint> {
        return jdbc.query("SELECT * FROM custom_analysis_endpoint ORDER BY created_at DESC", mapper)
    }

    fun getById(id: Long): CustomAnalysisEndpoint? {
        return jdbc.query("SELECT * FROM custom_analysis_endpoint WHERE id = ?", mapper, id).firstOrNull()
    }

    fun delete(id: Long): Boolean {
        return jdbc.update("DELETE FROM custom_analysis_endpoint WHERE id = ?", id) > 0
    }
}
