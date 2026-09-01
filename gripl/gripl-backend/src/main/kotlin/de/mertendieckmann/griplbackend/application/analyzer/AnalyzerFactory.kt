package de.mertendieckmann.griplbackend.application.analyzer

import de.mertendieckmann.griplbackend.adapter.rag.RagApiClient
import de.mertendieckmann.griplbackend.config.RagApiProperties
import dev.langchain4j.model.chat.ChatModel
import org.springframework.stereotype.Component

@Component
class AnalyzerFactory(
    private val ragApiClient: RagApiClient,
    private val ragApiProperties: RagApiProperties
) {
    fun createPromptEngineeringAnalyzer(chatModel: ChatModel): PromptBpmnAnalyzer {
        return PromptBpmnAnalyzer(chatModel, ragApiClient, ragApiProperties)
    }

    fun createBaselineAnalyzer(chatModel: ChatModel): BaselineBpmnAnalyzer {
        return BaselineBpmnAnalyzer(chatModel)
    }
    fun createMulticlassAnalyzer(chatModel: ChatModel): MulticlassBpmnAnalyzer {
        return MulticlassBpmnAnalyzer(chatModel)
}
}