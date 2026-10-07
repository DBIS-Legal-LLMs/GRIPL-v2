package de.mertendieckmann.griplbackend.model.dto

/**
 * A user's saved Process Analysis settings. The API key is deliberately not
 * part of this — it lives (encrypted) in auth-service, see AuthServiceClient.
 *
 * [configured] is only meaningful in responses: true once the user has saved
 * settings at least once. It is ignored on PUT.
 */
data class UserAnalysisSettings(
    val llmBaseUrl: String? = null,
    val modelName: String? = null,
    val seed: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val useRag: Boolean = false,
    val ragMode: RagMode = RagMode.HYBRID,
    val configured: Boolean = false
)
