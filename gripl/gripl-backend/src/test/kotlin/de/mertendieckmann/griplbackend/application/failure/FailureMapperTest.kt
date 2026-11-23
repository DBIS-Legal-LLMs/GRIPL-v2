package de.mertendieckmann.griplbackend.application.failure

import com.sun.net.httpserver.HttpServer
import de.mertendieckmann.griplbackend.config.LlmConfig
import dev.langchain4j.exception.AuthenticationException
import dev.langchain4j.exception.RateLimitException
import dev.langchain4j.service.output.OutputParsingException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.concurrent.CompletionException

/** Tests how failures of the language model provider are turned into safe messages, against a stub HTTP server. */
class FailureMapperTest {

    private var server: HttpServer? = null

    @AfterEach
    fun stopServer() {
        server?.stop(0)
    }

    private fun callModel(status: Int, body: String, apiKey: String = "sk-or-v1-secretsecretsecret"): Throwable {
        val stub = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        stub.createContext("/") { exchange ->
            exchange.requestBody.readAllBytes()
            val bytes = body.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        stub.start()
        server = stub

        val model = LlmConfig(LlmConfig.Companion.LlmProps()).getStrictJsonOpenAiChatModel(
            LlmConfig.Companion.LlmProps(
                modelName = "gpt-3.5-turbo",
                baseUrl = "http://127.0.0.1:${stub.address.port}/v1",
                apiKey = apiKey,
                timeoutSeconds = 5
            )
        )
        return runCatching { model.chat("hello") }.exceptionOrNull() ?: error("the stub answered $status, but no exception was thrown")
    }

    @Test
    fun `an invalid model id explains which model was rejected`() {
        val failure = FailureMapper.describe(
            callModel(400, """{"error":{"message":"gpt-3.5-turbo is not a valid model ID","code":400},"user_id":"user_abc"}""")
        )

        assertEquals("LLM_REQUEST_REJECTED", failure.code)
        assertEquals(400, failure.status)
        assertEquals("OpenRouter rejected the request: gpt-3.5-turbo is not a valid model ID", failure.message)
        assertEquals(400, failure.upstreamStatus)
    }

    @Test
    fun `a rejected API key is reported as a provider problem and not as an expired session`() {
        val failure = FailureMapper.describe(callModel(401, """{"error":{"message":"No auth credentials found","code":401}}"""))

        assertEquals("LLM_AUTH_FAILED", failure.code)
        assertEquals(502, failure.status)
        assertTrue(failure.message.contains("API key"))
    }

    @Test
    fun `missing credits are reported`() {
        val failure = FailureMapper.describe(callModel(402, """{"error":{"message":"Insufficient credits","code":402}}"""))

        assertEquals("LLM_CREDITS_EXHAUSTED", failure.code)
        assertEquals(402, failure.status)
    }

    @Test
    fun `rate limiting and server errors get their own codes`() {
        assertEquals("LLM_RATE_LIMITED", FailureMapper.describe(callModel(429, """{"error":{"message":"slow down"}}""")).code)
        server?.stop(0)
        val unavailable = FailureMapper.describe(callModel(503, """{"error":{"message":"upstream overloaded"}}"""))
        assertEquals("LLM_UNAVAILABLE", unavailable.code)
        assertTrue(unavailable.message.contains("upstream overloaded"))
    }

    @Test
    fun `a body that is not JSON is used as plain text`() {
        val failure = FailureMapper.describe(callModel(400, "Bad things happened"))

        assertEquals("OpenRouter rejected the request: Bad things happened", failure.message)
    }

    @Test
    fun `the API key never reaches the message or the logged body`() {
        val failure = FailureMapper.describe(
            callModel(400, """{"error":{"message":"invalid key sk-or-v1-secretsecretsecret and Bearer abcdefghijkl"}}""")
        )

        assertFalse(failure.message.contains("secretsecret"))
        assertFalse(failure.message.contains("abcdefghijkl"))
        assertFalse((failure.upstreamBody ?: "").contains("secretsecret"))
    }

    @Test
    fun `long upstream messages are shortened`() {
        val failure = FailureMapper.describe(callModel(400, """{"error":{"message":"${"x".repeat(2000)}"}}"""))

        assertTrue(failure.message.length < 400, "message was ${failure.message.length} characters long")
        assertTrue(failure.message.endsWith("…"))
    }

    @Test
    fun `a connection failure is reported as unreachable`() {
        val model = LlmConfig(LlmConfig.Companion.LlmProps()).getStrictJsonOpenAiChatModel(
            LlmConfig.Companion.LlmProps(baseUrl = "http://127.0.0.1:1/v1", apiKey = "k", timeoutSeconds = 2)
        )
        val failure = FailureMapper.describe(runCatching { model.chat("hello") }.exceptionOrNull()!!)

        assertEquals("LLM_UNAVAILABLE", failure.code)
    }

    @Test
    fun `the cause chain is searched, also through wrappers`() {
        val wrapped = CompletionException(RuntimeException("outer", RateLimitException("429")))

        assertEquals("LLM_RATE_LIMITED", FailureMapper.describe(wrapped).code)
        assertEquals("LLM_AUTH_FAILED", FailureMapper.describe(IllegalStateException(AuthenticationException("no"))).code)
        assertEquals("LLM_BAD_OUTPUT", FailureMapper.describe(OutputParsingException("x", RuntimeException("y"))).code)
    }

    @Test
    fun `unknown exceptions give a generic message without their text`() {
        val failure = FailureMapper.describe(IllegalStateException("jdbc:postgresql://db/gripl password=hunter2"))

        assertEquals("INTERNAL_ERROR", failure.code)
        assertEquals(500, failure.status)
        assertFalse(failure.message.contains("hunter2"))
        assertFalse(failure.message.contains("jdbc"))
    }

    @Test
    fun `failures of other services pass through unchanged`() {
        val own = Failure(FailureCode.RAG_UNAVAILABLE, "The knowledge graph service did not answer.")

        assertEquals(own, FailureMapper.describe(RuntimeException("wrapper", ServiceFailureException(own))))
    }

    @Test
    fun `request ids are short and differ`() {
        val a = FailureMapper.newRequestId()
        assertEquals(8, a.length)
        assertNotNull(a)
        assertTrue(a != FailureMapper.newRequestId())
    }
}
