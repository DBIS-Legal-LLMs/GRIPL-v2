package de.mertendieckmann.griplbackend.evaluation

import de.mertendieckmann.griplbackend.model.dto.EvaluationData
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportStepInfo
import de.mertendieckmann.griplbackend.model.evaluation.ClassificationSets
import de.mertendieckmann.griplbackend.model.evaluation.ElementTypeCounts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.camunda.bpm.model.bpmn.Bpmn
import org.camunda.bpm.model.bpmn.BpmnModelInstance
import org.camunda.bpm.model.bpmn.instance.Activity
import org.camunda.bpm.model.bpmn.instance.BaseElement
import org.camunda.bpm.model.bpmn.instance.DataObjectReference
import org.camunda.bpm.model.bpmn.instance.DataStoreReference
import org.camunda.bpm.model.bpmn.instance.Event
import org.camunda.bpm.model.bpmn.instance.FlowElement
import org.camunda.bpm.model.bpmn.instance.FlowNode
import org.camunda.bpm.model.bpmn.instance.Gateway
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.floor

suspend fun parseBpmn(xml: String): BpmnModelInstance =
    withContext(Dispatchers.Default) { Bpmn.readModelFromStream(xml.byteInputStream()) }

fun computeClassificationSets(
    expectedIds: List<String>,
    actualIds: List<String>
): ClassificationSets {
    val expected = expectedIds.toSet()
    val actual = actualIds.toSet()
    val truePositiveIds = expected.intersect(actual).toList()
    val falsePositiveIds = (actual - expected).toList()
    val falseNegativeIds = (expected - actual).toList()
    return ClassificationSets(truePositiveIds, falsePositiveIds, falseNegativeIds)
}

fun computeTrueNegativesCount(
    bpmnModel: BpmnModelInstance,
    truePositivesCount: Int,
    falsePositivesCount: Int,
    falseNegativesCount: Int,
    activitiesOnly: Boolean = false
): Int {
    val totalClassifiableElements = classifiableElementIds(bpmnModel, activitiesOnly).size
    return (totalClassifiableElements - truePositivesCount - falsePositivesCount - falseNegativesCount)
        .coerceAtLeast(0)
}

/**
 * IDs of all BPMN elements that the classifier can label as GDPR-relevant:
 * flow nodes (activities, events, gateways) plus data object/store references.
 * Mirrors the element types extracted by BpmnExtractor (text annotations excluded),
 * so the true-negative universe matches what is actually classified.
 * With [activitiesOnly], the universe is restricted to activities.
 */
fun classifiableElementIds(bpmnModel: BpmnModelInstance, activitiesOnly: Boolean = false): Set<String> =
    classifiableElements(bpmnModel, activitiesOnly).map { it.id }.toSet()

private fun classifiableElements(bpmnModel: BpmnModelInstance, activitiesOnly: Boolean = false): List<BaseElement> =
    (bpmnModel.getModelElementsByType(FlowNode::class.java) +
        bpmnModel.getModelElementsByType(DataObjectReference::class.java) +
        bpmnModel.getModelElementsByType(DataStoreReference::class.java))
        .distinctBy { it.id }
        .filter { !activitiesOnly || it is Activity }

/**
 * The element categories metrics are broken down by. The [key] is the stable identifier
 * used in report JSON; [displayName] is what reports and the UI show.
 */
enum class ElementCategory(val key: String, val displayName: String) {
    ACTIVITY("activity", "Activities"),
    EVENT("event", "Events"),
    GATEWAY("gateway", "Gateways"),
    DATA_OBJECT("dataObject", "Data Objects/Stores"),
    OTHER("other", "Other")
}

private fun categorize(element: BaseElement): ElementCategory = when (element) {
    is Activity -> ElementCategory.ACTIVITY
    is Gateway -> ElementCategory.GATEWAY
    is Event -> ElementCategory.EVENT
    is DataObjectReference, is DataStoreReference -> ElementCategory.DATA_OBJECT
    else -> ElementCategory.OTHER
}

/**
 * Breaks the confusion counts of one test case down by element category
 * (activities, events, gateways, data objects/stores), keyed by [ElementCategory.key].
 * True negatives per category are derived from the number of classifiable elements of that
 * category in the diagram. Ids that don't resolve to a classifiable element (e.g. stale
 * ground-truth labels) land in the "other" bucket instead of being silently dropped.
 */
fun computePerElementTypeCounts(
    bpmnModel: BpmnModelInstance,
    classification: ClassificationSets,
    activitiesOnly: Boolean = false
): Map<String, ElementTypeCounts> {
    val categoryById = classifiableElements(bpmnModel, activitiesOnly).associate { it.id to categorize(it) }

    fun byCategory(ids: List<String>): Map<ElementCategory, Int> =
        ids.groupingBy { categoryById[it] ?: ElementCategory.OTHER }.eachCount()

    val tp = byCategory(classification.truePositiveIds)
    val fp = byCategory(classification.falsePositiveIds)
    val fn = byCategory(classification.falseNegativeIds)
    val totals = categoryById.values.groupingBy { it }.eachCount()

    return (tp.keys + fp.keys + fn.keys + totals.keys).sortedBy { it.ordinal }.associate { category ->
        val tpCount = tp[category] ?: 0
        val fpCount = fp[category] ?: 0
        val fnCount = fn[category] ?: 0
        val tnCount = ((totals[category] ?: 0) - tpCount - fpCount - fnCount).coerceAtLeast(0)
        category.key to ElementTypeCounts(tpCount, fpCount, fnCount, tnCount)
    }
}

/**
 * Builds "<name> (<id>)" display strings for the given element ids. Elements are never
 * dropped: the display name falls back from the real BPMN name, to a caller-supplied label
 * (the analysis-resolved name, or a name derived via BpmnElement.derivedNameFromFlows for
 * unnamed gateways/events), to the raw id.
 */
fun getNamesWithIds(
    model: BpmnModelInstance,
    ids: List<String>,
    labelOverrides: Map<String, String> = emptyMap()
): List<String> =
    ids.map { id ->
        val name = model.getModelElementById<FlowElement>(id)?.name?.takeIf { it.isNotBlank() }
            ?: labelOverrides[id]?.takeIf { it.isNotBlank() }
            ?: id
        "$name ($id)"
    }

fun buildPreviewUrl(
    testCaseId: Long,
    correctActivityIds: List<String>,
    falsePositiveIds: List<String>,
    falseNegativeIds: List<String>
): String {
    fun enc(xs: List<String>) = xs.joinToString(",") { URLEncoder.encode(it, StandardCharsets.UTF_8) }
    return buildString {
        append("https://gripl.mertendieckmann.de/api/dataset/testcase/$testCaseId/preview")
        append("?correctIds=${enc(correctActivityIds)}")
        append("&falsePositiveIds=${enc(falsePositiveIds)}")
        append("&falseNegativeIds=${enc(falseNegativeIds)}")
        append("&salt=${floor(Math.random() * 99999)}")
    }
}

fun buildStepInfo(entry: EvaluationData, number: Int, total: Int) =
    EvaluationReportStepInfo(
        currentTestCaseName = entry.name ?: "Test Case ${entry.id}",
        currentTestCaseId = entry.id,
        currentTestCaseNumber = number,
        totalTestCases = total
    )