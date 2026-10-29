package de.mertendieckmann.griplbackend.repository

import de.mertendieckmann.griplbackend.model.dto.LabelAssignment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import de.mertendieckmann.griplbackend.model.dto.ExpectedValue

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

    /**
     * Replaces assignments only for elements that explicitly provide labelIds.
     *
     * Expected values without labelIds come from an older client and leave their
     * existing relational assignments unchanged.
     */
    fun replaceProvidedAssignments(
        evaluationDataId: Long,
        expectedValues: List<ExpectedValue>
    ) {
        val valuesWithExplicitLabels = expectedValues.filter {
            it.labelIds != null
        }

        if (valuesWithExplicitLabels.isEmpty()) {
            return
        }

        removeAssignmentsForMissingElements(
            evaluationDataId,
            expectedValues.map { it.value }.toSet()
        )

        valuesWithExplicitLabels.forEach { expectedValue ->
            replaceAssignmentsForElement(
                evaluationDataId,
                expectedValue.value,
                expectedValue.labelIds.orEmpty()
            )
        }
    }

    /**
    * Adds relational assignments for classifications supplied by older clients.
    *
    * Older dataset formats only contain predefined enum classifications and do
    * not provide label IDs. Custom labels are never created by this method.
    */
    fun addLegacyPredefinedAssignments(
        evaluationDataId: Long,
        expectedValues: List<ExpectedValue>
    ) {
        expectedValues
            .filter { it.labelIds == null }
            .forEach { expectedValue ->
                expectedValue.classification.forEach { classification ->
                    jdbc.update(
                        """
                        INSERT INTO evaluation_data_label (
                            evaluation_data_id,
                            element_id,
                            label_id
                        )
                        SELECT ?, ?, labels.id
                        FROM labels
                        WHERE labels.name = ?
                        AND labels.predefined = TRUE
                        ON CONFLICT DO NOTHING
                        """.trimIndent(),
                        evaluationDataId,
                        expectedValue.value,
                        classification.name
                    )
                }
            }
    }

    /**
     * Removes assignments of elements that no longer exist in the saved test case.
     */
    private fun removeAssignmentsForMissingElements(
        evaluationDataId: Long,
        remainingElementIds: Set<String>
    ) {
        if (remainingElementIds.isEmpty()) {
            jdbc.update(
                """
                DELETE FROM evaluation_data_label
                WHERE evaluation_data_id = ?
                """.trimIndent(),
                evaluationDataId
            )
            return
        }

        val placeholders = remainingElementIds.joinToString(",") { "?" }
        val sql = """
            DELETE FROM evaluation_data_label
            WHERE evaluation_data_id = ?
              AND element_id NOT IN ($placeholders)
        """.trimIndent()

        val parameters = arrayOf<Any>(
            evaluationDataId,
            *remainingElementIds.toTypedArray()
        )

        jdbc.update(sql, *parameters)
    }

    /**
     * Updates one BPMN element's assignments.
     *
     * Existing archived labels remain when their IDs are supplied. They can be
     * removed explicitly by omitting them from labelIds.
     */
    private fun replaceAssignmentsForElement(
        evaluationDataId: Long,
        elementId: String,
        labelIds: List<Long>
    ) {
        val distinctLabelIds = labelIds.distinct()

        if (distinctLabelIds.isEmpty()) {
            jdbc.update(
                """
                DELETE FROM evaluation_data_label
                WHERE evaluation_data_id = ?
                  AND element_id = ?
                """.trimIndent(),
                evaluationDataId,
                elementId
            )
            return
        }

        val placeholders = distinctLabelIds.joinToString(",") { "?" }
        val deleteSql = """
            DELETE FROM evaluation_data_label
            WHERE evaluation_data_id = ?
              AND element_id = ?
              AND label_id NOT IN ($placeholders)
        """.trimIndent()

        val deleteParameters = arrayOf<Any>(
            evaluationDataId,
            elementId,
            *distinctLabelIds.toTypedArray()
        )

        jdbc.update(deleteSql, *deleteParameters)

        distinctLabelIds.forEach { labelId ->
            val insertedRows = jdbc.update(
                """
                INSERT INTO evaluation_data_label (
                    evaluation_data_id,
                    element_id,
                    label_id
                )
                SELECT ?, ?, labels.id
                FROM labels
                WHERE labels.id = ?
                  AND labels.deleted_at IS NULL
                ON CONFLICT DO NOTHING
                """.trimIndent(),
                evaluationDataId,
                elementId,
                labelId
            )

            if (insertedRows == 0 &&
                !assignmentAlreadyExists(
                    evaluationDataId,
                    elementId,
                    labelId
                )
            ) {
                throw IllegalArgumentException(
                    "Label $labelId is not active or does not exist."
                )
            }
        }
    }

    /** Returns whether this exact assignment already exists. */
    private fun assignmentAlreadyExists(
        evaluationDataId: Long,
        elementId: String,
        labelId: Long
    ): Boolean {
        return jdbc.queryForObject(
            """
            SELECT EXISTS (
                SELECT 1
                FROM evaluation_data_label
                WHERE evaluation_data_id = ?
                  AND element_id = ?
                  AND label_id = ?
            )
            """.trimIndent(),
            Boolean::class.java,
            evaluationDataId,
            elementId,
            labelId
        ) ?: false
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