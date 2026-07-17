package de.mertendieckmann.griplbackend.application

import de.mertendieckmann.griplbackend.model.BpmnElement
import de.mertendieckmann.griplbackend.model.BpmnFlowLabel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.model.bpmn.Bpmn
import org.camunda.bpm.model.bpmn.BpmnModelInstance
import org.camunda.bpm.model.bpmn.impl.instance.ProcessImpl
import org.camunda.bpm.model.bpmn.instance.*
import org.camunda.bpm.model.xml.ModelParseException
import org.camunda.bpm.model.xml.ModelValidationException
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class BpmnExtractor {
    private val log = KotlinLogging.logger { }

    /**
     * The human-readable label of a sequence flow: its name, its condition expression, or both.
     * Returns null for unlabeled flows so they can be skipped entirely.
     */
    private fun sequenceFlowLabel(flow: SequenceFlow): String? {
        val name = flow.name?.takeIf { it.isNotBlank() }
        val condition = flow.conditionExpression?.textContent?.takeIf { it.isNotBlank() }
        return when {
            name != null && condition != null && !name.equals(condition, ignoreCase = true) -> "$name [condition: $condition]"
            name != null -> name
            else -> condition
        }
    }

    private fun outgoingFlowLabels(element: FlowNode): List<BpmnFlowLabel> =
        element.outgoing.mapNotNull { flow ->
            sequenceFlowLabel(flow)?.let { BpmnFlowLabel(it, flow.target.id) }
        }

    private fun incomingFlowLabels(element: FlowNode): List<BpmnFlowLabel> =
        element.incoming.mapNotNull { flow ->
            sequenceFlowLabel(flow)?.let { BpmnFlowLabel(it, flow.source.id) }
        }

    private fun outgoingMessageFlowLabels(bpmnModel: BpmnModelInstance, elementId: String): List<BpmnFlowLabel> =
        bpmnModel.getModelElementsByType(MessageFlow::class.java)
            .filter { it.source.id == elementId }
            .mapNotNull { flow -> flow.name?.takeIf { it.isNotBlank() }?.let { BpmnFlowLabel(it, flow.target.id) } }

    private fun incomingMessageFlowLabels(bpmnModel: BpmnModelInstance, elementId: String): List<BpmnFlowLabel> =
        bpmnModel.getModelElementsByType(MessageFlow::class.java)
            .filter { it.target.id == elementId }
            .mapNotNull { flow -> flow.name?.takeIf { it.isNotBlank() }?.let { BpmnFlowLabel(it, flow.source.id) } }

    /**
     * The name of the pool (participant) whose process contains this element.
     *
     * Elements nested inside subprocesses have a SubProcessImpl parent, not a
     * ProcessImpl, so casting parentElement directly would throw. Walk up the
     * XML tree to the nearest enclosing process instead; nested elements belong
     * to the same pool as their top-level process.
     */
    private fun resolvePoolName(bpmnModel: BpmnModelInstance, element: BaseElement): String? {
        var ancestor = element.parentElement
        while (ancestor != null && ancestor !is ProcessImpl) {
            ancestor = ancestor.parentElement
        }
        val processId = (ancestor as? ProcessImpl)?.id ?: return null
        return bpmnModel.getModelElementsByType(Participant::class.java)
            .find { it.getAttributeValue("processRef") == processId }
            ?.name
    }

    fun extractBpmnElements(bpmnXml: String): Set<BpmnElement> {
        val bpmnModel = Bpmn.readModelFromStream(bpmnXml.byteInputStream())
        Bpmn.validateModel(bpmnModel)
        return extractBpmnElements(bpmnModel)
    }

    /** Overload for callers that already hold a parsed model — avoids re-parsing the XML. */
    fun extractBpmnElements(bpmnModel: BpmnModelInstance): Set<BpmnElement> {

        val unsupportedElements = mutableSetOf<String>()

        log.info { "Extracting BPMN elements from XML" }

        val elements = bpmnModel.getModelElementsByType(BaseElement::class.java).mapNotNull { element ->
            when (element) {
                is Activity -> {
                    val bpmnElement = BpmnElement(
                        type = element.elementType.typeName,
                        id = element.id,
                        name = element.name,
                        documentation = element.documentations.joinToString { it.rawTextContent },
                        isActivity = true,
                        poolName = resolvePoolName(bpmnModel, element),
                        laneName = element.parentElement
                            .getChildElementsByType(LaneSet::class.java)
                            .flatMap { it.getChildElementsByType(Lane::class.java) }
                            .firstOrNull { lane -> lane.flowNodeRefs.any { it.id == element.id } }
                            ?.name,
                        incomingFlowElementIds = element.incoming.mapNotNull {
                            bpmnModel.getModelElementById<SequenceFlow>(it.id).getAttributeValue("sourceRef")
                        },
                        outgoingFlowElementIds = element.outgoing.mapNotNull {
                            bpmnModel.getModelElementById<SequenceFlow>(it.id).getAttributeValue("targetRef")
                        },
                        outgoingFlowLabels = outgoingFlowLabels(element),
                        incomingFlowLabels = incomingFlowLabels(element),
                        outgoingMessageFlowsToElementIds = bpmnModel.getModelElementsByType(MessageFlow::class.java).filter { it.source.id == element.id }.map { it.target.id },
                        incomingMessageFlowsFromElementIds = bpmnModel.getModelElementsByType(MessageFlow::class.java).filter { it.target.id == element.id }.map { it.source.id },
                        outgoingMessageFlowLabels = outgoingMessageFlowLabels(bpmnModel, element.id),
                        incomingMessageFlowLabels = incomingMessageFlowLabels(bpmnModel, element.id),
                        incomingDataFromElementIds = element.dataInputAssociations.flatMap { it.sources.mapNotNull { source -> source.id } },
                        outgoingDataToElementIds = element.dataOutputAssociations.map { it.target.id },
                        associatedElementIds = bpmnModel.getModelElementsByType(Association::class.java)
                            .filter { it.getAttributeValue("sourceRef") == element.id }.mapNotNull {
                                it.getAttributeValue("targetRef")
                            }
                    )
                    bpmnElement
                }

                is FlowNode -> {
                    BpmnElement(
                        type = element.elementType.typeName,
                        id = element.id,
                        name = element.name,
                        documentation = element.documentations.joinToString { it.rawTextContent },
                        poolName = resolvePoolName(bpmnModel, element),
                        laneName = element.parentElement
                            .getChildElementsByType(LaneSet::class.java)
                            .flatMap { it.getChildElementsByType(Lane::class.java) }
                            .firstOrNull { lane -> lane.flowNodeRefs.any { it.id == element.id } }
                            ?.name,
                        incomingFlowElementIds = element.incoming.mapNotNull {
                            bpmnModel.getModelElementById<SequenceFlow>(
                                it.id
                            ).getAttributeValue("sourceRef")
                        },
                        outgoingFlowElementIds = element.outgoing.mapNotNull {
                            bpmnModel.getModelElementById<SequenceFlow>(
                                it.id
                            ).getAttributeValue("targetRef")
                        },
                        outgoingFlowLabels = outgoingFlowLabels(element),
                        incomingFlowLabels = incomingFlowLabels(element),
                        outgoingMessageFlowsToElementIds = bpmnModel.getModelElementsByType(MessageFlow::class.java).filter { it.source.id == element.id }.map { it.target.id },
                        incomingMessageFlowsFromElementIds = bpmnModel.getModelElementsByType(MessageFlow::class.java).filter { it.target.id == element.id }.map { it.source.id },
                        outgoingMessageFlowLabels = outgoingMessageFlowLabels(bpmnModel, element.id),
                        incomingMessageFlowLabels = incomingMessageFlowLabels(bpmnModel, element.id),
                        associatedElementIds = bpmnModel.getModelElementsByType(Association::class.java)
                            .filter { it.getAttributeValue("sourceRef") == element.id }
                            .mapNotNull { it.getAttributeValue("targetRef") }
                    )
                }

                is DataStoreReference, is DataObjectReference -> {
                    BpmnElement(
                        type = element.elementType.typeName,
                        id = element.id,
                        name = when (element) {
                            is DataStoreReference -> element.name
                            is DataObjectReference -> element.name
                            else -> null
                        },
                        outgoingDataToElementIds = bpmnModel.getModelElementsByType(DataInputAssociation::class.java)
                            .filter { association -> association.sources.any { it.id == element.id } }
                            .mapNotNull {
                                when (val parent = it.parentElement) {
                                    is FlowElement -> parent.id
                                    else -> null
                                }
                            },
                        incomingDataFromElementIds = bpmnModel.getModelElementsByType(DataOutputAssociation::class.java)
                            .filter { association -> association.getAttributeValue("targetRef") == element.id }
                            .mapNotNull {
                                when (val parent = it.parentElement) {
                                    is FlowElement -> parent.id
                                    else -> null
                                }
                            },
                        associatedElementIds = bpmnModel.getModelElementsByType(Association::class.java)
                            .filter { it.getAttributeValue("sourceRef") == element.id }
                            .mapNotNull { it.getAttributeValue("targetRef") }
                    )
                }

                is TextAnnotation -> {
                    BpmnElement(
                        type = element.elementType.typeName,
                        id = element.id,
                        documentation = element.textContent,
                        associatedElementIds = bpmnModel.getModelElementsByType(Association::class.java)
                            .filter { it.getAttributeValue("sourceRef") == element.id }
                            .mapNotNull { it.getAttributeValue("targetRef") }
                            .plus(
                                bpmnModel.getModelElementsByType(Association::class.java)
                                    .filter { it.getAttributeValue("targetRef") == element.id }
                                    .mapNotNull { it.getAttributeValue("sourceRef") }
                            )
                    )
                }

                else -> {
                    unsupportedElements.add(element.elementType.typeName)
                    null
                }
            }
        }

        log.warn { "Unsupported BPMN elements found: ${unsupportedElements.joinToString(", ")}" }

        return elements.toSet()
    }
}