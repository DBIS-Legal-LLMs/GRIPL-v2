package de.mertendieckmann.griplbackend.evaluation.metrics

import de.mertendieckmann.griplbackend.model.dto.EvaluationReportSummary
import de.mertendieckmann.griplbackend.model.evaluation.EvaluationMetrics
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import de.mertendieckmann.griplbackend.model.analysis.GdprProcessingClass
import de.mertendieckmann.griplbackend.model.evaluation.ClassMetrics
import de.mertendieckmann.griplbackend.model.dto.PerClassEvaluationMetrics

/**
 * Thread-safe metrics accumulator for all test cases.
 */
class MetricsAccumulator {
    private val lock = Mutex()
    private var total: Int = 0
    private var passed: Int = 0
    private var errors: Int = 0
    private var amountOfRetries: Int = 0
    private var totalTruePositives: Int = 0
    private var totalFalsePositives: Int = 0
    private var totalFalseNegatives: Int = 0
    private var totalTrueNegatives: Int = 0
    private val perClassTotals = GdprProcessingClass.entries.associateWith {
        ClassMetrics()
    }.toMutableMap()

    suspend fun add(metrics: EvaluationMetrics) = lock.withLock {
        total++
        if (metrics.isSuccessful) passed++
        totalTruePositives += metrics.truePositives
        totalFalsePositives += metrics.falsePositives
        totalFalseNegatives += metrics.falseNegatives
        totalTrueNegatives += metrics.trueNegatives
        amountOfRetries += metrics.amountOfRetries ?: 0
        metrics.perClassMetrics.forEach { (gdprClass, classMetrics) ->
            val current = perClassTotals.getValue(gdprClass)

            perClassTotals[gdprClass] = ClassMetrics(
                truePositives = current.truePositives + classMetrics.truePositives,
                falsePositives = current.falsePositives + classMetrics.falsePositives,
                falseNegatives = current.falseNegatives + classMetrics.falseNegatives,
                trueNegatives = current.trueNegatives + classMetrics.trueNegatives
            )
        }
    }

    suspend fun addError() = lock.withLock {
        total++
        errors++
    }

    fun toSummary(): EvaluationReportSummary {
        val precision = if (totalTruePositives + totalFalsePositives > 0)
            totalTruePositives.toDouble() / (totalTruePositives + totalFalsePositives)
        else 0.0

        val recall = if (totalTruePositives + totalFalseNegatives > 0)
            totalTruePositives.toDouble() / (totalTruePositives + totalFalseNegatives)
        else 0.0

        val f1Score = if (precision + recall > 0)
            2 * (precision * recall) / (precision + recall)
        else 0.0

        val accuracy = if (totalTruePositives + totalFalsePositives + totalFalseNegatives + totalTrueNegatives > 0)
            (totalTruePositives + totalTrueNegatives).toDouble() / (totalTruePositives + totalFalsePositives + totalFalseNegatives + totalTrueNegatives)
        else 0.0

        val exactMatchAccuracy = if (total > 0)
            passed.toDouble() / total
        else 0.0

        val perClassMetrics = perClassTotals.mapValues { (_, metrics) ->

            val classPrecision =
                if (metrics.truePositives + metrics.falsePositives > 0)
                    metrics.truePositives.toDouble() /
                        (metrics.truePositives + metrics.falsePositives)
                else 0.0

            val classRecall =
                if (metrics.truePositives + metrics.falseNegatives > 0)
                    metrics.truePositives.toDouble() /
                        (metrics.truePositives + metrics.falseNegatives)
                else 0.0

            val classF1Score =
                if (classPrecision + classRecall > 0)
                    2 * (classPrecision * classRecall) /
                        (classPrecision + classRecall)
                else 0.0

            val totalClassDecisions =
                metrics.truePositives +
                metrics.falsePositives +
                metrics.falseNegatives +
                metrics.trueNegatives

            val classAccuracy =
                if (totalClassDecisions > 0)
                    (metrics.truePositives + metrics.trueNegatives).toDouble() /
                        totalClassDecisions
                else 0.0

            PerClassEvaluationMetrics(
                truePositives = metrics.truePositives,
                falsePositives = metrics.falsePositives,
                falseNegatives = metrics.falseNegatives,
                trueNegatives = metrics.trueNegatives,
                precision = classPrecision,
                recall = classRecall,
                f1Score = classF1Score,
                accuracy = classAccuracy
            )
        }
        return EvaluationReportSummary(
            total = total,
            passed = passed,
            failed = (total - passed - errors).coerceAtLeast(0),
            error = errors,
            amountOfRetries = amountOfRetries,
            precision = precision,
            recall = recall,
            f1Score = f1Score,
            accuracy = accuracy,
            exactMatchAccuracy = exactMatchAccuracy,
            totalTruePositives = totalTruePositives,
            totalFalsePositives = totalFalsePositives,
            totalFalseNegatives = totalFalseNegatives,
            totalTrueNegatives = totalTrueNegatives,
            perClassMetrics = perClassMetrics
        )
    }
}
