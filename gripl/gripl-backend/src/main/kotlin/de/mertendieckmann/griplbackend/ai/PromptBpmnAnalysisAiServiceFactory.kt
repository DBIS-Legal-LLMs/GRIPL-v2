package de.mertendieckmann.griplbackend.ai

import dev.langchain4j.memory.chat.ChatMemoryProvider
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.service.AiServices

object PromptBpmnAnalysisAiServiceFactory {

    val defaultBasePrompt: String = loadResource("prompts/system-prompt-base.txt")
    val defaultActivitiesOnlyBasePrompt: String = loadResource("prompts/system-prompt-base-activities-only.txt")
    private val citationsPrompt: String = loadResource("prompts/system-prompt-citations.txt")

    fun create(llm: ChatModel, memoryProvider: ChatMemoryProvider, systemPrompt: String): PromptBpmnAnalysisAiService =
        build(llm, memoryProvider, "$systemPrompt\n\n$citationsPrompt")

    fun createWithoutRag(llm: ChatModel, memoryProvider: ChatMemoryProvider, systemPrompt: String): PromptBpmnAnalysisAiService =
        build(llm, memoryProvider, systemPrompt)

    private fun build(
        llm: ChatModel,
        memoryProvider: ChatMemoryProvider,
        finalSystemPrompt: String
    ): PromptBpmnAnalysisAiService {
        return AiServices
            .builder(PromptBpmnAnalysisAiService::class.java)
            .chatModel(llm)
            .chatMemoryProvider(memoryProvider)
            .systemMessageProvider { _ -> finalSystemPrompt }
            .build()
    }

    private fun loadResource(path: String): String =
        checkNotNull(
            PromptBpmnAnalysisAiServiceFactory::class.java.classLoader.getResourceAsStream(path)
        ) { "Prompt resource not found: $path" }
            .bufferedReader()
            .readText()
            .trim()
}
