package de.mertendieckmann.griplbackend.model.evaluation

data class EvaluationMetrics(
    val truePositives: Int,
    val falsePositives: Int,
    val falseNegatives: Int,
    val trueNegatives: Int,
    val isSuccessful: Boolean,
    val amountOfRetries: Int? = null,
    val ragMetrics: RagMetrics? = null,
    /** Confusion counts broken down by element category (activity, event, gateway, dataObject). */
    val perElementType: Map<String, ElementTypeCounts> = emptyMap()
)

/** Confusion-matrix counts for one element category. */
data class ElementTypeCounts(
    val truePositives: Int = 0,
    val falsePositives: Int = 0,
    val falseNegatives: Int = 0,
    val trueNegatives: Int = 0
) {
    operator fun plus(other: ElementTypeCounts) = ElementTypeCounts(
        truePositives + other.truePositives,
        falsePositives + other.falsePositives,
        falseNegatives + other.falseNegatives,
        trueNegatives + other.trueNegatives
    )
}
