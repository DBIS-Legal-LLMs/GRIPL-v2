package de.mertendieckmann.griplbackend.repository

import tools.jackson.databind.ObjectMapper
import de.mertendieckmann.griplbackend.model.dto.Dataset
import de.mertendieckmann.griplbackend.model.dto.EvaluationData
import de.mertendieckmann.griplbackend.model.dto.EvaluationDataWithOptionalId
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class EvaluationDataRepository(
    private val jdbc: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {

    private val mapper = RowMapper { rs, _ ->
        EvaluationData.fromRow(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            bpmnXml = rs.getString("bpmn_xml"),
            expectedJson = rs.getString("expected_values"),
            datasetId = rs.getLong("dataset_id"),
        )
    }

    fun getAllEvaluationData(): List<EvaluationData> {
        return jdbc.query("SELECT * FROM evaluation_data", mapper)
    }

    fun getEvaluationDataByDatasetIdsOrAll(dataset: List<Int>): List<EvaluationData> {
        if (dataset.isEmpty()) {
            return getAllEvaluationData()
        }
        val inSql = dataset.joinToString(",")
        return jdbc.query("SELECT * FROM evaluation_data WHERE dataset_id IN ($inSql)", mapper)
    }

    // ── Owner-scoped access (GRIPL-v2#33) ──────────────────────────────────
    // A test case is accessible iff its parent dataset is owned by the caller.
    // Test cases with no dataset_id (dataset deleted, or created without one)
    // have no owner and are excluded from every scoped read below. Wired into
    // EvaluationDataController only; the evaluation-run paths above stay
    // unscoped (role gating for those is GRIPL-v2#40).

    fun getEvaluationDataForOwner(ownerUserId: String, datasetId: Int? = null): List<EvaluationData> {
        val sql = buildString {
            append(
                """
                SELECT ed.* FROM evaluation_data ed
                JOIN dataset d ON ed.dataset_id = d.id
                WHERE d.owner_user_id = ?
                """.trimIndent()
            )
            if (datasetId != null) append(" AND ed.dataset_id = ?")
        }
        val args = listOfNotNull(ownerUserId, datasetId?.toLong()).toTypedArray()
        return jdbc.query(sql, mapper, *args)
    }

    fun getEvaluationDataByIdForOwner(id: Long, ownerUserId: String): EvaluationData? {
        val sql = """
            SELECT ed.* FROM evaluation_data ed
            JOIN dataset d ON ed.dataset_id = d.id
            WHERE ed.id = ? AND d.owner_user_id = ?
        """.trimIndent()
        return jdbc.query(sql, mapper, id, ownerUserId).firstOrNull()
    }

    fun getEvaluationDataByIds(ids: List<Int>): List<EvaluationData> {
        if (ids.isEmpty()) return emptyList()
        val inSql = ids.joinToString(",")
        return jdbc.query("SELECT * FROM evaluation_data WHERE id IN ($inSql)", mapper)
    }

    fun getEvaluationDataById(id: Long): EvaluationData? {
        return jdbc.query("SELECT * FROM evaluation_data WHERE id = ?", mapper, id).firstOrNull()
    }

    fun insertEvaluationData(data: EvaluationDataWithOptionalId): Int? {
        val sql = """
            INSERT INTO evaluation_data (name, bpmn_xml, expected_values, dataset_id)
            VALUES (?, ?, ?::jsonb, ?)
            RETURNING id
        """.trimIndent()

        val json  = objectMapper.writeValueAsString(data.expectedValues)
        val value = PGobject().apply { type = "jsonb"; this.value = json }

        return jdbc.queryForObject(
            sql,
            Int::class.java,
            data.name,
            data.bpmnXml,
            value,
            data.datasetId
        )!!
    }

    fun updateEvaluationData(data: EvaluationData, ownerUserId: String): Int {
        val sql = """
            UPDATE evaluation_data
            SET name = ?, bpmn_xml = ?, expected_values = ?::jsonb, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
              AND dataset_id IN (SELECT id FROM dataset WHERE owner_user_id = ?)
        """.trimIndent()

        return jdbc.update(sql) { ps ->
            ps.setString(1, data.name)
            ps.setString(2, data.bpmnXml)

            val json     = objectMapper.writeValueAsString(data.expectedValues)
            val pgObject = PGobject().apply {
                type  = "jsonb"
                value = json
            }
            ps.setObject(3, pgObject)
            ps.setLong(4, data.id)
            ps.setString(5, ownerUserId)
        }
    }

    fun deleteEvaluationData(id: Long, ownerUserId: String): Int {
        val sql = """
            DELETE FROM evaluation_data
            WHERE id = ?
              AND dataset_id IN (SELECT id FROM dataset WHERE owner_user_id = ?)
        """.trimIndent()
        return jdbc.update(sql, id, ownerUserId)
    }

    fun countEvaluationDataForDatasets(datasetIds: List<Long>): Int {
        if (datasetIds.isEmpty()) return 0
        val inSql = datasetIds.joinToString(",")
        val sql = "SELECT COUNT(*) FROM evaluation_data WHERE dataset_id IN ($inSql)"
        return jdbc.queryForObject(sql, Int::class.java) ?: 0
    }
}