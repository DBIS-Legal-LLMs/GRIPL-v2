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

    // ── Owner-scoped access (GRIPL-v2#33) ──────────────────────────────────
    // Datasets are private to the auth-service user (JWT `sub`) that created
    // them. Every path reachable from DatasetController / EvaluationDataController
    // goes through one of these; getDatasetsByIdsAndOwner below covers the
    // evaluation-run path (GRIPL-v2#40).

    fun createDataset(request: CreateDatasetRequest, ownerUserId: String): Int {
        val sql = "INSERT INTO dataset (name, description, owner_user_id) VALUES (?, ?, ?)"
        return jdbc.update(sql, request.name, request.description, ownerUserId)
    }

    fun getDatasetsByOwner(ownerUserId: String): List<Dataset> {
        val sql = "SELECT * FROM dataset WHERE owner_user_id = ? ORDER BY id"
        return jdbc.query(sql, mapper, ownerUserId)
    }

    fun getDatasetByIdAndOwner(id: Long, ownerUserId: String): Dataset? {
        val sql = "SELECT * FROM dataset WHERE id = ? AND owner_user_id = ?"
        return jdbc.query(sql, mapper, id, ownerUserId).firstOrNull()
    }

    fun isDatasetOwnedBy(id: Long, ownerUserId: String): Boolean {
        val sql = "SELECT COUNT(*) FROM dataset WHERE id = ? AND owner_user_id = ?"
        return (jdbc.queryForObject(sql, Int::class.java, id, ownerUserId) ?: 0) > 0
    }

    fun deleteDataset(id: Long, ownerUserId: String): Int {
        val sql = "DELETE FROM dataset WHERE id = ? AND owner_user_id = ?"
        return jdbc.update(sql, id, ownerUserId)
    }

    // ── Evaluation-run path (GRIPL-v2#40) ──────────────────────────────────
    // The classification/evaluation runners take dataset ids from the request
    // body, so they must scope by owner too — otherwise any caller with a
    // privileged GRIPL role could run an evaluation against, and read back
    // the contents of, another user's private dataset by id.

    fun getDatasetsByIdsAndOwner(ids: List<Long>, ownerUserId: String): List<Dataset> {
        if (ids.isEmpty()) return emptyList()
        val inSql = ids.joinToString(",")
        val sql = "SELECT * FROM dataset WHERE id IN ($inSql) AND owner_user_id = ?"
        return jdbc.query(sql, mapper, ownerUserId)
    }

    // ── Unscoped reads — not wired to any endpoint ─────────────────────────
    // Kept as low-level primitives only; do not call these from a controller
    // or from the evaluation-run path without an owner filter (that's exactly
    // the gap GRIPL-v2#40 closed for getDatasetsByIds, above).

    fun getAllDatasets(): List<Dataset> {
        val sql = "SELECT * FROM dataset"
        return jdbc.query(sql, mapper)
    }

    fun getDatasetById(id: Long): Dataset? {
        val sql = "SELECT * FROM dataset WHERE id = ?"
        return jdbc.query(sql, mapper, id).firstOrNull()
    }
}
