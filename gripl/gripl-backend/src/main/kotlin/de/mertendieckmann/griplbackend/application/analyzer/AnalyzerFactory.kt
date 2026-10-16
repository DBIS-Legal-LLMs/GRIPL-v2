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

    fun createMulticlassAnalyzer(chatModel: ChatModel): MulticlassBpmnAnalyzer {
        return MulticlassBpmnAnalyzer(chatModel, ragApiClient, ragApiProperties)
    }

    /** A user-uploaded custom endpoint whose response shape is binary (critical/not-critical). */
    fun createCustomBinaryAnalyzer(chatModel: ChatModel, promptText: String): PromptBpmnAnalyzer {
        return PromptBpmnAnalyzer(chatModel, ragApiClient, ragApiProperties) { _ -> promptText }
    }

    /** A user-uploaded custom endpoint whose response shape is multiclass (GDPR processing classes). */
    fun createCustomMulticlassAnalyzer(chatModel: ChatModel, promptText: String): MulticlassBpmnAnalyzer {
        return MulticlassBpmnAnalyzer(chatModel, ragApiClient, ragApiProperties, promptText)
    }
}
