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

    /**
    * Returns label definitions assigned to test cases of the selected datasets.
    *
    * Deleted labels are included so historical test cases remain transferable.
    */
    fun getExportRowsForDatasetIds(
        datasetIds: List<Long>,
        ownerUserId: String
    ): List<List<String?>> {
        if (datasetIds.isEmpty()) {
            return emptyList()
        }

        val sql = """
            SELECT DISTINCT
                labels.id::text,
                labels.name,
                labels.description,
                labels.predefined::text
            FROM labels
            JOIN evaluation_data_label
                ON evaluation_data_label.label_id = labels.id
            JOIN evaluation_data
                ON evaluation_data.id = evaluation_data_label.evaluation_data_id
            JOIN dataset
                ON dataset.id = evaluation_data.dataset_id
            WHERE evaluation_data.dataset_id IN (${datasetIds.joinToString(",")})
            AND dataset.owner_user_id = ?
            ORDER BY 1
        """.trimIndent()

        val rowMapper = RowMapper { rs, _ ->
            (1..4).map { rs.getString(it) }
        }

        return jdbc.query(sql, rowMapper, ownerUserId)
    }

    /**
    * Finds a label by name and prefers an active label when archived and active
    * labels with the same name exist.
    */
    fun findByNameIncludingArchived(name: String): Label? {
        val sql = """
            SELECT *
            FROM labels
            WHERE lower(trim(name)) = lower(trim(?))
            ORDER BY (deleted_at IS NULL) DESC, id
            LIMIT 1
        """.trimIndent()

        return jdbc.query(sql, mapper, name).firstOrNull()
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