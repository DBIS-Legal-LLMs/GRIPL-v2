package de.mertendieckmann.griplbackend.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.Properties

/** The prompts of the users must not end up in the server log by default. */
class LlmRequestLoggingTest {

    private val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    private val clientLog = LoggerFactory.getLogger("dev.langchain4j.http.client.log") as Logger
    private val appender = ListAppender<ILoggingEvent>()
    private var server: HttpServer? = null

    @BeforeEach
    fun capture() {
        appender.start()
        root.addAppender(appender)
    }

    @AfterEach
    fun release() {
        root.detachAppender(appender)
        clientLog.level = null
        server?.stop(0)
    }

    private fun askModel(secretPrompt: String) {
        val stub = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        stub.createContext("/") { exchange ->
            exchange.requestBody.readAllBytes()
            val body = """{"id":"1","object":"chat.completion","model":"m","choices":[{"index":0,"message":{"role":"assistant","content":"{}"},"finish_reason":"stop"}],"usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        stub.start()
        server = stub
        LlmConfig(LlmConfig.Companion.LlmProps()).getStrictJsonOpenAiChatModel(
            LlmConfig.Companion.LlmProps(baseUrl = "http://127.0.0.1:${stub.address.port}/v1", apiKey = "sk-or-v1-secret", timeoutSeconds = 5)
        ).chat(secretPrompt)
    }

    private fun levelFromApplicationProperties(): Level {
        val properties = Properties().apply { this@LlmRequestLoggingTest.javaClass.classLoader.getResourceAsStream("application.properties")!!.use { load(it) } }
        return Level.valueOf(properties.getProperty("logging.level.dev.langchain4j.http.client.log"))
    }

    @Test
    fun `without the setting the client would log the prompt at INFO`() {
        clientLog.level = Level.INFO

        askModel("tell me about customer Smith")

        assertTrue(appender.list.any { it.formattedMessage.contains("customer Smith") }, "control: the dump exists")
    }

    @Test
    fun `with the configured level no prompt is logged`() {
        clientLog.level = levelFromApplicationProperties()

        askModel("tell me about customer Smith")

        assertEquals(emptyList<String>(), appender.list.filter { it.formattedMessage.contains("Smith") || it.formattedMessage.contains("sk-or") }.map { it.formattedMessage })
    }
}
