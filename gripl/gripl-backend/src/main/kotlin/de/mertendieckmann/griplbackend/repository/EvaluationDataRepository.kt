package de.mertendieckmann.griplbackend.repository

import tools.jackson.databind.ObjectMapper
import de.mertendieckmann.griplbackend.model.dto.Dataset
import de.mertendieckmann.griplbackend.model.dto.EvaluationData
import de.mertendieckmann.griplbackend.model.dto.EvaluationDataWithOptionalId
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import de.mertendieckmann.griplbackend.model.analysis.GdprProcessingClass
import de.mertendieckmann.griplbackend.model.dto.ExpectedValue
import org.springframework.transaction.annotation.Transactional

@Repository
class EvaluationDataRepository(
    private val jdbc: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val evaluationDataLabelRepository: EvaluationDataLabelRepository
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

    /**
     * Adds relational label assignments to the expected values loaded from the
     * existing JSON column. Values without an assignment keep an empty label list.
     */
    private fun attachLabelAssignments(
        evaluationData: List<EvaluationData>
    ): List<EvaluationData> {
        if (evaluationData.isEmpty()) {
            return emptyList()
        }

        val assignmentsByTestCase =
            evaluationDataLabelRepository.findAssignmentsForEvaluationDataIds(
                evaluationData.map { it.id }
            )

        return evaluationData.map { testCase ->
            val assignmentsByElement =
                assignmentsByTestCase[testCase.id].orEmpty()

            testCase.copy(
                expectedValues = testCase.expectedValues.map { expectedValue ->
                    expectedValue.copy(
                        labels = assignmentsByElement[expectedValue.value].orEmpty()
                    )
                }
            )
        }
    }

    
    /**
    * JSON representation stored in the existing expected_values column.
    *
    * Relational label assignments are deliberately excluded because they belong
    * in evaluation_data_label. The classification remains for the current
    * analysis and evaluation implementation.
    */
    private data class StoredExpectedValue(
        val value: String,
        val explanation: String?,
        val classification: List<GdprProcessingClass>
    )

    /** Converts expected values to the legacy JSON format used by evaluation_data. */
    private fun expectedValuesJson(
        expectedValues: List<ExpectedValue>
    ): String {
        val storedValues = expectedValues.map { expectedValue ->
            StoredExpectedValue(
                value = expectedValue.value,
                explanation = expectedValue.explanation,
                classification = expectedValue.classification
            )
        }

        return objectMapper.writeValueAsString(storedValues)
    }

    fun getAllEvaluationData(): List<EvaluationData> {
        return attachLabelAssignments(jdbc.query("SELECT * FROM evaluation_data", mapper)
        )
    }

    // ── Owner-scoped access (GRIPL-v2#33) ──────────────────────────────────
    // A test case is accessible iff its parent dataset is owned by the caller.
    // Test cases with no dataset_id (dataset deleted, or created without one)
    // have no owner and are excluded from every scoped read below. Wired into
    // EvaluationDataController, and into the evaluation-run path below
    // (GRIPL-v2#40) — otherwise a caller could run an evaluation against, and
    // read back the contents of, another user's private test cases by id.

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
        return attachLabelAssignments(jdbc.query(sql, mapper, *args))
    }

    fun getEvaluationDataByIdForOwner(id: Long, ownerUserId: String): EvaluationData? {
        val sql = """
            SELECT ed.* FROM evaluation_data ed
            JOIN dataset d ON ed.dataset_id = d.id
            WHERE ed.id = ? AND d.owner_user_id = ?
        """.trimIndent()
        return attachLabelAssignments(jdbc.query(sql, mapper, id, ownerUserId)).firstOrNull()
    }

    fun getEvaluationDataByIdsForOwner(ids: List<Int>, ownerUserId: String): List<EvaluationData> {
        if (ids.isEmpty()) return emptyList()
        val inSql = ids.joinToString(",")
        val sql = """
            SELECT ed.* FROM evaluation_data ed
            JOIN dataset d ON ed.dataset_id = d.id
            WHERE ed.id IN ($inSql) AND d.owner_user_id = ?
        """.trimIndent()
        return attachLabelAssignments(jdbc.query(sql, mapper, ownerUserId))
    }

    fun getEvaluationDataByDatasetIdsOrAllForOwner(datasetIds: List<Int>, ownerUserId: String): List<EvaluationData> {
        if (datasetIds.isEmpty()) {
            return getEvaluationDataForOwner(ownerUserId)
        }
        val inSql = datasetIds.joinToString(",")
        val sql = """
            SELECT ed.* FROM evaluation_data ed
            JOIN dataset d ON ed.dataset_id = d.id
            WHERE ed.dataset_id IN ($inSql) AND d.owner_user_id = ?
        """.trimIndent()
        return attachLabelAssignments(jdbc.query(sql, mapper, ownerUserId))
    }

    fun countEvaluationDataForDatasetsAndOwner(datasetIds: List<Long>, ownerUserId: String): Int {
        if (datasetIds.isEmpty()) return 0
        val inSql = datasetIds.joinToString(",")
        val sql = """
            SELECT COUNT(*) FROM evaluation_data ed
            JOIN dataset d ON ed.dataset_id = d.id
            WHERE ed.dataset_id IN ($inSql) AND d.owner_user_id = ?
        """.trimIndent()
        return jdbc.queryForObject(sql, Int::class.java, ownerUserId) ?: 0
    }

    // ── Unscoped reads — not wired to any endpoint ─────────────────────────
    // Kept as low-level primitives only; do not call these from a controller
    // or from the evaluation-run path without an owner filter (that's exactly
    // the gap GRIPL-v2#40 closed for the *ForOwner variants above).

    fun getEvaluationDataById(id: Long): EvaluationData? {
        return attachLabelAssignments(jdbc.query("SELECT * FROM evaluation_data WHERE id = ?", mapper, id)).firstOrNull()
    }

    @Transactional
    fun insertEvaluationData(data: EvaluationDataWithOptionalId): Int? {
        val sql = """
            INSERT INTO evaluation_data (name, bpmn_xml, expected_values, dataset_id)
            VALUES (?, ?, ?::jsonb, ?)
            RETURNING id
        """.trimIndent()

        val json = expectedValuesJson(data.expectedValues)
        val value = PGobject().apply {
            type = "jsonb"
            this.value = json
        }

        val evaluationDataId = jdbc.queryForObject(
            sql,
            Int::class.java,
            data.name,
            data.bpmnXml,
            value,
            data.datasetId
        )!!

        evaluationDataLabelRepository.replaceProvidedAssignments(
            evaluationDataId.toLong(),
            data.expectedValues
        )

        evaluationDataLabelRepository.addLegacyPredefinedAssignments(
            evaluationDataId.toLong(),
            data.expectedValues
        )

        return evaluationDataId
    }

    /**
    * Inserts an imported test case and preserves its timestamps when available.
    */
    @Transactional
    fun insertImportedEvaluationData(
        data: EvaluationDataWithOptionalId,
        createdAt: String?,
        updatedAt: String?
    ): Long {
        val sql = """
            INSERT INTO evaluation_data (
                name,
                bpmn_xml,
                expected_values,
                dataset_id,
                created_at,
                updated_at
            )
            VALUES (
                ?,
                ?,
                ?::jsonb,
                ?,
                COALESCE(?::timestamptz, now()),
                COALESCE(?::timestamptz, now())
            )
            RETURNING id
        """.trimIndent()

        val value = PGobject().apply {
            type = "jsonb"
            this.value = expectedValuesJson(data.expectedValues)
        }

        val evaluationDataId = jdbc.queryForObject(
            sql,
            Long::class.java,
            data.name,
            data.bpmnXml,
            value,
            data.datasetId,
            createdAt,
            updatedAt
        )!!

        evaluationDataLabelRepository.replaceProvidedAssignments(
            evaluationDataId,
            data.expectedValues
        )

        evaluationDataLabelRepository.addLegacyPredefinedAssignments(
            evaluationDataId,
            data.expectedValues
        )

        return evaluationDataId
    }

    /**
     * Rows in the column layout of the original Postgres CSV export of the evaluation_data table.
     * expected_values is rendered by Postgres itself, so the JSON looks exactly like in the original export.
     * Only test cases of datasets owned by [ownerUserId] are returned.
     */
    fun getEvaluationDataExportRows(datasetIds: List<Long>, ownerUserId: String): List<List<String?>> {
        if (datasetIds.isEmpty()) return emptyList()
        val sql = """
            SELECT ed.id::text, ed.bpmn_xml, ed.expected_values::text,
                   (ed.created_at AT TIME ZONE 'UTC')::text || '+00',
                   (ed.updated_at AT TIME ZONE 'UTC')::text || '+00',
                   ed.name, ed.dataset_id::text
            FROM evaluation_data ed
            JOIN dataset d ON ed.dataset_id = d.id
            WHERE ed.dataset_id IN (${datasetIds.joinToString(",")}) AND d.owner_user_id = ?
            ORDER BY ed.id
        """.trimIndent()
        val rowMapper = RowMapper { rs, _ -> (1..7).map { rs.getString(it) } }
        return jdbc.query(sql, rowMapper, ownerUserId)
    }

    /**
    * Updates a test case and its explicitly supplied relational label assignments.
    */
    @Transactional
    fun updateEvaluationData(
        data: EvaluationData,
        ownerUserId: String
    ): Int {
        val sql = """
            UPDATE evaluation_data
            SET name = ?,
                bpmn_xml = ?,
                expected_values = ?::jsonb,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            AND dataset_id IN (
                SELECT id
                FROM dataset
                WHERE owner_user_id = ?
            )
        """.trimIndent()

        val updatedRows = jdbc.update(sql) { ps ->
            ps.setString(1, data.name)
            ps.setString(2, data.bpmnXml)

            val pgObject = PGobject().apply {
                type = "jsonb"
                value = expectedValuesJson(data.expectedValues)
            }

            ps.setObject(3, pgObject)
            ps.setLong(4, data.id)
            ps.setString(5, ownerUserId)
        }

        if (updatedRows == 1) {
            evaluationDataLabelRepository.replaceProvidedAssignments(
                data.id,
                data.expectedValues
            )

            evaluationDataLabelRepository.addLegacyPredefinedAssignments(
                data.id,
                data.expectedValues
            )
        }

        return updatedRows
    }

    /** Renames a test case; only matches if its parent dataset is owned by [ownerUserId] (0 rows otherwise). */
    fun updateEvaluationDataName(id: Long, name: String?, ownerUserId: String): Int {
        val sql = """
            UPDATE evaluation_data
            SET name = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
              AND dataset_id IN (SELECT id FROM dataset WHERE owner_user_id = ?)
        """.trimIndent()

        return jdbc.update(sql, name, id, ownerUserId)
    }

    fun deleteEvaluationData(id: Long, ownerUserId: String): Int {
        val sql = """
            DELETE FROM evaluation_data
            WHERE id = ?
              AND dataset_id IN (SELECT id FROM dataset WHERE owner_user_id = ?)
        """.trimIndent()
        return jdbc.update(sql, id, ownerUserId)
    }

    // Unscoped — not wired to any endpoint or the evaluation-run path (see
    // countEvaluationDataForDatasetsAndOwner above), kept as a low-level
    // primitive only.
    fun countEvaluationDataForDatasets(datasetIds: List<Long>): Int {
        if (datasetIds.isEmpty()) return 0
        val inSql = datasetIds.joinToString(",")
        val sql = "SELECT COUNT(*) FROM evaluation_data WHERE dataset_id IN ($inSql)"
        return jdbc.queryForObject(sql, Int::class.java) ?: 0
    }
}