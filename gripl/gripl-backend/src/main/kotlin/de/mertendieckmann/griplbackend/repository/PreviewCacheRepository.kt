package de.mertendieckmann.griplbackend.repository

import tools.jackson.databind.ObjectMapper
import de.mertendieckmann.griplbackend.model.dto.PreviewCache
import de.mertendieckmann.griplbackend.model.dto.PreviewCacheInsert
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class PreviewCacheRepository(
    private val jdbc: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {

    private val mapper = RowMapper { rs, _ ->
        PreviewCache(
            id = rs.getLong("id"),
            evaluationDataId = rs.getLong("evaluation_data_id"),
            urlCacheKey = rs.getString("url_cache_key"),
            svg = rs.getString("svg")
        )
    }

    fun getCachedPreview(testcaseId: Long, url: String): PreviewCache? {
        return jdbc.query("""
            SELECT * FROM preview_cache
            WHERE evaluation_data_id = ? AND url_cache_key = ?
            """.trimIndent(),
            mapper,
            testcaseId,
            url
        ).firstOrNull()
    }

    fun insertPreviewCache(preview: PreviewCacheInsert): Int {
        // ON CONFLICT DO UPDATE (rather than a plain INSERT) because two requests for the same
        // preview (same model + query params, e.g. concurrent <img> loads) can both miss the
        // cache and race to insert the same url_cache_key — without this, the loser hits the
        // UNIQUE constraint on url_cache_key and throws instead of just reusing/refreshing the row.
        val sql = """
            INSERT INTO preview_cache (evaluation_data_id, url_cache_key, svg)
            VALUES (?, ?, ?)
            ON CONFLICT (url_cache_key) DO UPDATE SET svg = EXCLUDED.svg, updated_at = CURRENT_TIMESTAMP
            RETURNING id
        """.trimIndent()

        return jdbc.queryForObject(
            sql,
            Int::class.java,
            preview.evaluationDataId,
            preview.urlCacheKey,
            preview.svg
        ) ?: throw IllegalStateException("Failed to insert preview cache")
    }
}