package de.mertendieckmann.griplbackend.model

data class BpmnElement(
    val type: String,
    val id: String,
    val name: String? = null,
    val documentation: String? = null,
    val poolName: String? = null,
    val laneName: String? = null,
    val isActivity: Boolean = false,
    val outgoingFlowElementIds: List<String> = emptyList(),
    val incomingFlowElementIds: List<String> = emptyList(),
    val outgoingFlowLabels: List<BpmnFlowLabel> = emptyList(),
    val incomingFlowLabels: List<BpmnFlowLabel> = emptyList(),
    val outgoingMessageFlowsToElementIds: List<String> = emptyList(),
    val incomingMessageFlowsFromElementIds: List<String> = emptyList(),
    val outgoingMessageFlowLabels: List<BpmnFlowLabel> = emptyList(),
    val incomingMessageFlowLabels: List<BpmnFlowLabel> = emptyList(),
    val incomingDataFromElementIds: List<String> = emptyList(),
    val outgoingDataToElementIds: List<String> = emptyList(),
    val associatedElementIds: List<String> = emptyList()
) {
    
    // Deterministic display name for unnamed elements, derived from their labeled flows
    fun derivedNameFromFlows(): String? {
        if (!name.isNullOrBlank()) return null

        // Display only the name part of combined "name [condition: ...]" labels.
        fun displayLabel(flowLabel: BpmnFlowLabel): String =
            flowLabel.label.replace(Regex("""\s*\[condition:.*]$"""), "")

        return when {
            type.contains("gateway", ignoreCase = true) ->
                outgoingFlowLabels.map(::displayLabel).filter { it.isNotBlank() }
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(" / ", prefix = "Gateway: ")

            type.contains("event", ignoreCase = true) -> {
                val sends = outgoingMessageFlowLabels.map(::displayLabel).filter { it.isNotBlank() }
                val receives = incomingMessageFlowLabels.map(::displayLabel).filter { it.isNotBlank() }
                when {
                    sends.isNotEmpty() -> "Event: sends ${sends.joinToString(" / ")}"
                    receives.isNotEmpty() -> "Event: receives ${receives.joinToString(" / ")}"
                    else -> null
                }
            }

            else -> null
        }
    }
}


 // A labeled sequence or message flow attached to an element.
data class BpmnFlowLabel(
    val label: String,
    val otherElementId: String
) {
    override fun toString() = "\"$label\" (connects to $otherElementId)"
}