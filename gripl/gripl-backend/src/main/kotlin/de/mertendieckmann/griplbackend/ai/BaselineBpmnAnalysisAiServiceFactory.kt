package de.mertendieckmann.griplbackend.ai

import dev.langchain4j.memory.chat.ChatMemoryProvider
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.service.AiServices

object BaselineBpmnAnalysisAiServiceFactory {

    fun create(llm: ChatModel, memoryProvider: ChatMemoryProvider, activitiesOnly: Boolean = false): BaselineBpmnAnalysisAiService {
        return AiServices
            .builder(BaselineBpmnAnalysisAiService::class.java)
            .chatModel(llm)
            .chatMemoryProvider(memoryProvider)
            .systemMessageProvider { getPrompt(activitiesOnly) }
            .build()
    }

    private fun getPrompt(activitiesOnly: Boolean): String {
        return if (activitiesOnly) {
            """
                Your task is to identify and return a list of the IDs of all Activity (Task) elements that process personal data.
                Ignore all other element types.
            """.trimIndent()
        } else {
            """
                Your task is to identify and return a list of the IDs of all BPMN elements that process personal data.
                Consider activities/tasks, events, gateways, and data objects/data stores; ignore all other element types.
            """.trimIndent()
        }
    }
}