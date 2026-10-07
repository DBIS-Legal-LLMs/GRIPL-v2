package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.model.dto.RagMode
import de.mertendieckmann.griplbackend.model.dto.SaveUserAnalysisSettingsRequest
import de.mertendieckmann.griplbackend.model.dto.UserAnalysisSettings
import de.mertendieckmann.griplbackend.repository.UserAnalysisSettingsRepository
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_USER_ID_ATTRIBUTE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

/** Analysis settings are keyed by the caller's id — never by anything in the request. */
class UserAnalysisSettingsControllerTest {

    private val repository = mock<UserAnalysisSettingsRepository>()
    private val controller = UserAnalysisSettingsController(repository)

    private fun exchangeAs(userId: String) =
        MockServerWebExchange.from(MockServerHttpRequest.get("/analysis-settings"))
            .apply { attributes[AUTHENTICATED_USER_ID_ATTRIBUTE] = userId }

    @Test
    fun `a user who never saved settings gets unconfigured defaults`() {
        whenever(repository.findByOwner("user-1")).thenReturn(null)

        val result = controller.get(exchangeAs("user-1"))

        assertFalse(result.configured)
        assertEquals("https://openrouter.ai/api/v1", result.llmBaseUrl)
        assertEquals("openai/gpt-oss-20b", result.modelName)
        assertEquals(1.0, result.temperature)
        assertEquals(1.0, result.topP)
        assertFalse(result.useRag)
        assertEquals(RagMode.HYBRID, result.ragMode)
    }

    @Test
    fun `get only reads the callers own row`() {
        val mine = UserAnalysisSettings(modelName = "my-model", configured = true)
        whenever(repository.findByOwner("user-1")).thenReturn(mine)
        whenever(repository.findByOwner("user-2")).thenReturn(UserAnalysisSettings(modelName = "theirs", configured = true))

        assertEquals("my-model", controller.get(exchangeAs("user-1")).modelName)
    }

    @Test
    fun `save writes under the callers id and returns the stored row as configured`() {
        val incoming = SaveUserAnalysisSettingsRequest(modelName = "m", temperature = 0.2, useRag = true, ragMode = RagMode.LOCAL)
        val stored = incoming.withDefaults()
        whenever(repository.findByOwner("user-1")).thenReturn(stored.copy(configured = true))

        val result = controller.save(incoming, exchangeAs("user-1"))

        verify(repository).upsert("user-1", stored)
        assertTrue(result.configured)
        assertEquals(RagMode.LOCAL, result.ragMode)
    }

    @Test
    fun `empty values are stored as the defaults, not as null`() {
        val request = SaveUserAnalysisSettingsRequest(llmBaseUrl = "  ", modelName = "", temperature = null, topP = null, seed = 7)

        val stored = request.withDefaults()

        assertEquals("https://openrouter.ai/api/v1", stored.llmBaseUrl)
        assertEquals("openai/gpt-oss-20b", stored.modelName)
        assertEquals(1.0, stored.temperature)
        assertEquals(1.0, stored.topP)
        assertEquals(7, stored.seed)
        assertEquals(RagMode.HYBRID, stored.ragMode)
    }

    @Test
    fun `explicit values win over the defaults`() {
        val stored = SaveUserAnalysisSettingsRequest(
            llmBaseUrl = "https://example.org/v1", modelName = "x/y", temperature = 0.0, topP = 0.5
        ).withDefaults()

        assertEquals("https://example.org/v1", stored.llmBaseUrl)
        assertEquals("x/y", stored.modelName)
        assertEquals(0.0, stored.temperature)
        assertEquals(0.5, stored.topP)
    }
}
