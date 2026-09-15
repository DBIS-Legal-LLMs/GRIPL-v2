package de.mertendieckmann.griplbackend.model.dto

/**
 * A read-only view of one of the built-in analyzers' prompt text, so users can inspect what a
 * built-in endpoint actually does (e.g. before writing a custom one) without being able to edit it.
 */
data class DefaultAnalysisPrompt(
    val name: String,
    val responseType: CustomAnalysisResponseType,
    val promptText: String
)
