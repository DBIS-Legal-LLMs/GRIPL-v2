package de.mertendieckmann.griplbackend.repository

import tools.jackson.databind.ObjectMapper
import de.mertendieckmann.griplbackend.model.dto.CreateDatasetRequest
import de.mertendieckmann.griplbackend.model.dto.Dataset
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class DatasetRepository(
    private val jdbc: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {

    private val mapper = RowMapper { rs, _ ->
        Dataset.fromRow(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            description = rs.getString("description"),
            createdAt = rs.getString("created_at"),
            updatedAt = rs.getString("updated_at")
        )
    }

    fun createDataset(request: CreateDatasetRequest): Int {
        val sql = "INSERT INTO dataset (name, description) VALUES (?, ?)"
        return jdbc.update(sql, request.name, request.description)
    }

    fun getAllDatasets(): List<Dataset> {
        val sql = "SELECT * FROM dataset"
        return jdbc.query(sql, mapper)
    }

    fun getDatasetById(id: Long): Dataset? {
        val sql = "SELECT * FROM dataset WHERE id = ?"
        return jdbc.query(sql, mapper, id).firstOrNull()
    }

    fun getDatasetsByIds(ids: List<Long>): List<Dataset> {
        if (ids.isEmpty()) return emptyList()
        val inSql = ids.joinToString(",")
        val sql = "SELECT * FROM dataset WHERE id IN ($inSql)"
        return jdbc.query(sql, mapper)
    }

    /** Inserts a dataset and keeps the given timestamps (Postgres text format) if present. */
    fun insertDatasetReturningId(name: String, description: String?, createdAt: String?, updatedAt: String?): Long {
        val sql = """
            INSERT INTO dataset (name, description, created_at, updated_at)
            VALUES (?, ?, COALESCE(?::timestamptz, now()), COALESCE(?::timestamptz, now()))
            RETURNING id
        """.trimIndent()
        return jdbc.queryForObject(sql, Long::class.java, name, description, createdAt, updatedAt)!!
    }

    /**
     * Rows in the column layout of the original Postgres CSV export of the dataset table.
     * Timestamps are rendered in UTC like "2026-02-11 18:40:58.465728+00".
     */
    fun getDatasetExportRows(datasetIds: List<Long>?): List<List<String?>> {
        if (datasetIds != null && datasetIds.isEmpty()) return emptyList()
        val where = if (datasetIds != null) "WHERE id IN (${datasetIds.joinToString(",")})" else ""
        val sql = """
            SELECT id::text, name, description,
                   (created_at AT TIME ZONE 'UTC')::text || '+00',
                   (updated_at AT TIME ZONE 'UTC')::text || '+00'
            FROM dataset $where ORDER BY id
        """.trimIndent()
        val rowMapper = RowMapper { rs, _ -> (1..5).map { rs.getString(it) } }
        return jdbc.query(sql, rowMapper)
    }

    fun deleteDataset(id: Long): Int {
        val sql = "DELETE FROM dataset WHERE id = ?"
        return jdbc.update(sql, id)
    }
}