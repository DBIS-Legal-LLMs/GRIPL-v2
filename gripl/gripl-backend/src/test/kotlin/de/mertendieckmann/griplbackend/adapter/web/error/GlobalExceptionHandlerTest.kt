package de.mertendieckmann.griplbackend.adapter.web.error

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * Regression test: a ResponseStatusException (GRIPL-v2#33's ownership 404s,
 * GRIPL-v2#40's role 403s, ...) must surface its own status code, not fall
 * through to the catch-all RuntimeException handler and become a 500. Found
 * by testing #40's role gate against the real running stack — mocked
 * controller-unit tests can't catch this since they never go through Spring's
 * exception-resolution pipeline.
 */
class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `a ResponseStatusException keeps its own status and reason`() {
        val response = handler.handleResponseStatusException(
            ResponseStatusException(HttpStatus.FORBIDDEN, "Requires one of [admin, researcher]")
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertEquals("Requires one of [admin, researcher]", response.body?.message)
    }

    @Test
    fun `a plain RuntimeException still falls back to 500`() {
        val response = handler.handleRuntimeException(RuntimeException("boom"))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }
}
