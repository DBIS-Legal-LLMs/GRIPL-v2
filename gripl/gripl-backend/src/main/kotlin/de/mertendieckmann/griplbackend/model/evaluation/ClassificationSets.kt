package de.mertendieckmann.griplbackend.model.evaluation

data class ClassificationSets<T>(
    val truePositiveIds: List<T>,
    val falsePositiveIds: List<T>,
    val falseNegativeIds: List<T>
)