package de.mertendieckmann.griplbackend.model.evaluation

import de.mertendieckmann.griplbackend.model.analysis.GdprProcessingClass

data class ClassMetrics(
    val truePositives: Int = 0,
    val falsePositives: Int = 0,
    val falseNegatives: Int = 0,
    val trueNegatives: Int = 0
)

data class EvaluationMetrics(
    val truePositives: Int,
    val falsePositives: Int,
    val falseNegatives: Int,
    val trueNegatives: Int,
    val isSuccessful: Boolean,
    val amountOfRetries: Int? = null,
    val perClassMetrics: Map<GdprProcessingClass, ClassMetrics> = emptyMap()
)