package de.mertendieckmann.griplbackend.repository

import de.mertendieckmann.griplbackend.model.dto.Label
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

/** Provides database access to global labels. */
@Repository
class LabelRepository(
    private val jdbc: JdbcTemplate
) {

    /** Converts a database row into a Label. */
    private val mapper = RowMapper<Label> { rs, _ ->
        Label(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            description = rs.getString("description"),
            predefined = rs.getBoolean("predefined"),
            createdBy = rs.getString("created_by"),
            createdAt = rs.getString("created_at"),
            updatedBy = rs.getString("updated_by"),
            updatedAt = rs.getString("updated_at"),
            deletedBy = rs.getString("deleted_by"),
            deletedAt = rs.getString("deleted_at")
        )
    }

    /** Returns all labels available that are not deleted. */
    fun findAllActive(): List<Label> {
        val sql = """
            SELECT *
            FROM labels
            WHERE deleted_at IS NULL
            ORDER BY predefined DESC, lower(name), id
        """.trimIndent()

        return jdbc.query(sql, mapper)
    }

    /** Returns the predefined labels used by the GDPR evaluation. */
    fun findAllPredefined(): List<Label> {
        val sql = """
            SELECT *
            FROM labels
            WHERE predefined = true
              AND deleted_at IS NULL
            ORDER BY id
        """.trimIndent()

        return jdbc.query(sql, mapper)
    }

    /** Includes deleted labels so existing assignments remain readable. */
    fun findById(id: Long): Label? {
        val sql = """
            SELECT *
            FROM labels
            WHERE id = ?
        """.trimIndent()

        return jdbc.query(sql, mapper, id).firstOrNull()
    }

    /** Creates a custom label and records the authenticated creator. */
    fun create(
        name: String,
        description: String,
        userId: String
    ): Label {
        val sql = """
            INSERT INTO labels (
                name,
                description,
                predefined,
                created_by,
                updated_by
            )
            VALUES (?, ?, false, ?, ?)
            RETURNING *
        """.trimIndent()

        return jdbc.queryForObject(
            sql,
            mapper,
            name,
            description,
            userId,
            userId
        )!!
    }

    /** Updates an active label and records who changed it. */
    fun update(
        id: Long,
        name: String,
        description: String,
        userId: String
    ): Label? {
        val sql = """
            UPDATE labels
            SET name = ?,
                description = ?,
                updated_by = ?,
                updated_at = now()
            WHERE id = ?
              AND deleted_at IS NULL
            RETURNING *
        """.trimIndent()

        return jdbc.query(
            sql,
            mapper,
            name,
            description,
            userId,
            id
        ).firstOrNull()
    }

    /** Soft-deletes an active custom label without removing its row. */
    fun softDelete(id: Long, userId: String): Boolean {
        val sql = """
            UPDATE labels
            SET deleted_by = ?,
                deleted_at = now(),
                updated_by = ?,
                updated_at = now()
            WHERE id = ?
              AND predefined = false
              AND deleted_at IS NULL
        """.trimIndent()

        return jdbc.update(sql, userId, userId, id) > 0
    }
}