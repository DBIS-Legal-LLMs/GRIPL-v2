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
        val sql = "SELECT * FROM dataset ORDER BY id ASC"
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

    fun updateDataset(id: Long, name: String, description: String?): Int {
        val sql = "UPDATE dataset SET name = ?, description = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
        return jdbc.update(sql, name, description, id)
    }

    fun deleteDataset(id: Long): Int {
        val sql = "DELETE FROM dataset WHERE id = ?"
        return jdbc.update(sql, id)
    }
}