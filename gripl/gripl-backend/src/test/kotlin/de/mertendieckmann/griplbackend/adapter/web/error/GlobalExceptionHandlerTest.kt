package de.mertendieckmann.griplbackend.adapter.web.error

import dev.langchain4j.exception.InvalidRequestException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ResponseStatusException

/**
 * A ResponseStatusException (GRIPL-v2#33's ownership 404s, GRIPL-v2#40's role 403s, ...) must surface
 * its own status code, not fall through to the catch-all handler and become a 500; every other failure
 * gets a stable code, a message that is safe to show and a request id.
 */
class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()
    private val exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/evaluation/run").build())

    @Test
    fun `a ResponseStatusException keeps its own status and reason`() {
        val response = handler.handleResponseStatusException(
            ResponseStatusException(HttpStatus.FORBIDDEN, "Requires one of [admin, researcher]"), exchange
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertEquals("Requires one of [admin, researcher]", response.body?.message)
        assertEquals(403, response.body?.status)
        assertEquals("FORBIDDEN", response.body?.code)
        assertNull(response.body?.requestId)
    }

    @Test
    fun `an unknown exception gives a 500 with a generic message and a request id`() {
        val response = handler.handleFailure(RuntimeException("jdbc:postgresql://db password=hunter2"), exchange)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("INTERNAL_ERROR", response.body?.code)
        assertFalse(response.body?.message?.contains("hunter2") ?: true)
        assertNotNull(response.body?.requestId)
    }

    @Test
    fun `an invalid model id is a 400 that names the model`() {
        val response = handler.handleFailure(
            InvalidRequestException("""{"error":{"message":"gpt-3.5-turbo is not a valid model ID","code":400}}"""), exchange
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(400, response.body?.status)
        assertEquals("LLM_REQUEST_REJECTED", response.body?.code)
        assertEquals("OpenRouter rejected the request: gpt-3.5-turbo is not a valid model ID", response.body?.message)
        assertNotNull(response.body?.requestId)
    }

    @Test
    fun `a missing request part is a 400 without a request id`() {
        val response = handler.handleMissingRequestValueException(
            org.springframework.web.server.MissingRequestValueException("bpmnFile", String::class.java, "request part", org.springframework.core.MethodParameter(String::class.java.getMethod("length"), -1)),
            exchange
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("MISSING_REQUEST_VALUE", response.body?.code)
        assertNull(response.body?.requestId)
    }
}
