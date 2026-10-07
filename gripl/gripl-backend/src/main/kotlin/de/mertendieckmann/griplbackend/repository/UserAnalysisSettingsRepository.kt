package de.mertendieckmann.griplbackend.repository

import de.mertendieckmann.griplbackend.model.dto.RagMode
import de.mertendieckmann.griplbackend.model.dto.UserAnalysisSettings
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class UserAnalysisSettingsRepository(
    private val jdbc: JdbcTemplate
) {

    private val mapper = RowMapper { rs, _ ->
        UserAnalysisSettings(
            llmBaseUrl = rs.getString("llm_base_url"),
            modelName = rs.getString("model_name"),
            seed = rs.getObject("seed") as Int?,
            temperature = rs.getObject("temperature") as Double?,
            topP = rs.getObject("top_p") as Double?,
            useRag = rs.getBoolean("use_rag"),
            ragMode = RagMode.fromString(rs.getString("rag_mode")),
            configured = true
        )
    }

    fun findByOwner(ownerUserId: String): UserAnalysisSettings? =
        jdbc.query("SELECT * FROM user_analysis_settings WHERE owner_user_id = ?", mapper, ownerUserId)
            .firstOrNull()

    fun upsert(ownerUserId: String, settings: UserAnalysisSettings) {
        jdbc.update(
            """
            INSERT INTO user_analysis_settings
                (owner_user_id, llm_base_url, model_name, seed, temperature, top_p, use_rag, rag_mode, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (owner_user_id) DO UPDATE SET
                llm_base_url = EXCLUDED.llm_base_url,
                model_name   = EXCLUDED.model_name,
                seed         = EXCLUDED.seed,
                temperature  = EXCLUDED.temperature,
                top_p        = EXCLUDED.top_p,
                use_rag      = EXCLUDED.use_rag,
                rag_mode     = EXCLUDED.rag_mode,
                updated_at   = now()
            """.trimIndent(),
            ownerUserId,
            settings.llmBaseUrl?.takeIf { it.isNotBlank() },
            settings.modelName?.takeIf { it.isNotBlank() },
            settings.seed,
            settings.temperature,
            settings.topP,
            settings.useRag,
            settings.ragMode.value
        )
    }
}
