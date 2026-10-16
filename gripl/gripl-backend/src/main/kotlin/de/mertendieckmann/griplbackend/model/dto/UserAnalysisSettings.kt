package de.mertendieckmann.griplbackend.model.dto

/**
 * A user's saved Process Analysis settings. The API key is deliberately not
 * part of this — it lives (encrypted) in auth-service, see AuthServiceClient.
 *
 * Base URL, model, temperature and top-p always carry a concrete value: when
 * the user leaves one empty, the default below is stored instead of NULL, so
 * what the database says is what the analysis runs with. Temperature and top-p
 * default to 1.0, the provider default. [seed] has no default (null = not
 * seeded).
 *
 * [configured] is only meaningful in responses: true once the user has saved
 * settings at least once. It is ignored on PUT.
 */
data class UserAnalysisSettings(
    val llmBaseUrl: String = DEFAULT_LLM_BASE_URL,
    val modelName: String = DEFAULT_MODEL_NAME,
    val seed: Int? = null,
    val temperature: Double = DEFAULT_TEMPERATURE,
    val topP: Double = DEFAULT_TOP_P,
    val useRag: Boolean = false,
    val ragMode: RagMode = RagMode.HYBRID,
    val configured: Boolean = false
) {
    companion object {
        const val DEFAULT_LLM_BASE_URL = "https://openrouter.ai/api/v1"
        const val DEFAULT_MODEL_NAME = "openai/gpt-oss-20b"
        const val DEFAULT_TEMPERATURE = 1.0
        const val DEFAULT_TOP_P = 1.0
    }
}

/**
 * PUT body: every field is optional, and a missing, null or blank value means
 * "use the default" — see [UserAnalysisSettings].
 */
data class SaveUserAnalysisSettingsRequest(
    val llmBaseUrl: String? = null,
    val modelName: String? = null,
    val seed: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val useRag: Boolean = false,
    val ragMode: RagMode? = null
) {
    fun withDefaults() = UserAnalysisSettings(
        llmBaseUrl = llmBaseUrl?.trim()?.takeIf { it.isNotEmpty() } ?: UserAnalysisSettings.DEFAULT_LLM_BASE_URL,
        modelName = modelName?.trim()?.takeIf { it.isNotEmpty() } ?: UserAnalysisSettings.DEFAULT_MODEL_NAME,
        seed = seed,
        temperature = temperature ?: UserAnalysisSettings.DEFAULT_TEMPERATURE,
        topP = topP ?: UserAnalysisSettings.DEFAULT_TOP_P,
        useRag = useRag,
        ragMode = ragMode ?: RagMode.HYBRID
    )
}
