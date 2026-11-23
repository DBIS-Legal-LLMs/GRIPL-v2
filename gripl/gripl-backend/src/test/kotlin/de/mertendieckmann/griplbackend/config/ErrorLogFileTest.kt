package de.mertendieckmann.griplbackend.config

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.util.LogbackMDCAdapter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

/** Loads the real logback-spring.xml into a separate logger context and checks what the daily error file gets. */
class ErrorLogFileTest {

    private val context = LoggerContext()

    @AfterEach
    fun stopContext() {
        context.stop()
    }

    private fun configure(logDir: Path?) {
        context.name = "error-log-file-test"
        context.mdcAdapter = LogbackMDCAdapter()
        logDir?.let { context.putProperty("LOG_DIR", it.toString()) }
        JoranConfigurator().apply {
            this.context = this@ErrorLogFileTest.context
            doConfigure(javaClass.classLoader.getResource("logback-spring.xml"))
        }
    }

    private fun todaysFile(dir: Path): Path =
        dir.resolve("gripl-backend-errors-${LocalDate.now()}.log")

    @Test
    fun `the daily file gets warnings and errors with their cause chain, and no info lines`(@TempDir dir: Path) {
        configure(dir)
        val log = context.getLogger("de.mertendieckmann.griplbackend.Test")

        log.info("an info line with a prompt: tell me about the process")
        log.warn("a warning line")
        log.error("Analysis failed [code=LLM_REQUEST_REJECTED, requestId=ab12cd34, processModelId=7]", RuntimeException("outer", IllegalStateException("root cause")))
        context.getLogger("dev.langchain4j.http.client.log.LoggingHttpClient").info("HTTP request: Authorization: Bearer sk-or-v1-abc... body: {prompt}")

        val content = Files.readString(todaysFile(dir))
        assertTrue(content.contains("a warning line"))
        assertTrue(content.contains("requestId=ab12cd34"))
        assertTrue(content.contains("Caused by: java.lang.IllegalStateException: root cause"), content)
        assertFalse(content.contains("an info line"))
        assertFalse(content.contains("HTTP request"), "the language model dumps must not reach the file")
    }

    @Test
    fun `the file is named after the day and holds one line per event`(@TempDir dir: Path) {
        configure(dir)
        context.getLogger("x").warn("one")
        context.getLogger("x").error("two")

        assertEquals(listOf(todaysFile(dir).fileName.toString()), Files.list(dir).use { files -> files.map { it.fileName.toString() }.toList() })
        assertEquals(2, Files.readAllLines(todaysFile(dir)).size)
    }

    @Test
    fun `without LOG_DIR the file goes to the logs folder`() {
        val xml = javaClass.classLoader.getResource("logback-spring.xml")!!.readText()

        assertTrue(xml.contains("\${LOG_DIR:-logs}"))
    }
}
