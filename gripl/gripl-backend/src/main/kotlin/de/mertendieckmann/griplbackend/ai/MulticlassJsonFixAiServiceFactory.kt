package de.mertendieckmann.griplbackend.ai

import dev.langchain4j.memory.chat.ChatMemoryProvider
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.service.AiServices

object MulticlassJsonFixAiServiceFactory {

    fun create(llm: ChatModel, memoryProvider: ChatMemoryProvider): MulticlassJsonFixAiService {
        return AiServices
            .builder(MulticlassJsonFixAiService::class.java)
            .chatModel(llm)
            .chatMemoryProvider(memoryProvider)
            .systemMessageProvider { getPrompt() }
            .build()
    }

    private fun getPrompt(): String {
        return """
            Your task is to fix the provided JSON to match the desired output format.
            You will receive an error message indicating what is wrong with the JSON.
            Correct the JSON and return only the fixed JSON without any additional text.
            Ensure that the JSON is properly formatted and valid.
        """.trimIndent()
    }
}
