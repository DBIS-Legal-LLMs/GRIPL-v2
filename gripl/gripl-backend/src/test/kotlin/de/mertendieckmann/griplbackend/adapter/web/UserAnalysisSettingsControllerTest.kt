package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.model.dto.RagMode
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
        val incoming = UserAnalysisSettings(modelName = "m", temperature = 0.2, useRag = true, ragMode = RagMode.LOCAL)
        whenever(repository.findByOwner("user-1")).thenReturn(incoming.copy(configured = true))

        val result = controller.save(incoming, exchangeAs("user-1"))

        verify(repository).upsert("user-1", incoming)
        assertTrue(result.configured)
        assertEquals(RagMode.LOCAL, result.ragMode)
    }
}
