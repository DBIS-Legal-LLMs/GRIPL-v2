package de.mertendieckmann.griplbackend.repository

import de.mertendieckmann.griplbackend.model.dto.LabelAssignment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

/** Provides database access to labels assigned to BPMN elements in test cases. */
@Repository
class EvaluationDataLabelRepository(
    private val jdbc: JdbcTemplate
) {

    /**
     * Returns all label assignments for the given test cases, including archived labels.
     *
     * The outer map is keyed by test case ID and the inner map by BPMN element ID.
     */
    fun findAssignmentsForEvaluationDataIds(
        evaluationDataIds: Collection<Long>
    ): Map<Long, Map<String, List<LabelAssignment>>> {
        if (evaluationDataIds.isEmpty()) {
            return emptyMap()
        }

        val placeholders = evaluationDataIds.joinToString(",") { "?" }
        val sql = """
            SELECT
                evaluation_data_label.evaluation_data_id,
                evaluation_data_label.element_id,
                labels.id,
                labels.name,
                labels.description,
                labels.predefined,
                labels.deleted_at
            FROM evaluation_data_label
            JOIN labels
                ON labels.id = evaluation_data_label.label_id
            WHERE evaluation_data_label.evaluation_data_id IN ($placeholders)
            ORDER BY
                evaluation_data_label.evaluation_data_id,
                evaluation_data_label.element_id,
                labels.id
        """.trimIndent()

        val rows = jdbc.query(
            sql,
            mapper,
            *evaluationDataIds.toTypedArray()
        )

        return rows
            .groupBy { it.evaluationDataId }
            .mapValues { (_, testCaseRows) ->
                testCaseRows
                    .groupBy { it.elementId }
                    .mapValues { (_, elementRows) ->
                        elementRows.map { it.label }
                    }
            }
    }

    /** Holds one row while the query result is grouped by test case and BPMN element. */
    private data class AssignmentRow(
        val evaluationDataId: Long,
        val elementId: String,
        val label: LabelAssignment
    )

    /** Converts one joined database row into an internal assignment row. */
    private val mapper = RowMapper<AssignmentRow> { rs, _ ->
        AssignmentRow(
            evaluationDataId = rs.getLong("evaluation_data_id"),
            elementId = rs.getString("element_id"),
            label = LabelAssignment(
                id = rs.getLong("id"),
                name = rs.getString("name"),
                description = rs.getString("description"),
                predefined = rs.getBoolean("predefined"),
                deletedAt = rs.getString("deleted_at")
            )
        )
    }
}