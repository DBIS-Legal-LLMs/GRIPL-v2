package de.mertendieckmann.griplbackend.application

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component

@Component
class PreviewGenerator {

    private val htmlTemplate: String = loadHtmlTemplate()

    // Lazily initialized on first preview request rather than at bean-creation time,
    // so a missing local Playwright browser install doesn't prevent the app from starting.
    private var playwright: Playwright? = null
    private var browser: Browser? = null

    private fun loadHtmlTemplate(): String {
        return javaClass.getResource("/PreviewGeneratorTemplate.html")
            ?.readText(Charsets.UTF_8)
            ?: error("PreviewGeneratorTemplate.html nicht gefunden")
    }

    @Synchronized
    private fun getOrCreateBrowser(): Browser {
        browser?.let { return it }

        val pw = Playwright.create()
        playwright = pw
        val newBrowser = pw.chromium().launch(
            BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(listOf(
                    "--no-sandbox",
                    "--disable-setuid-sandbox"
                ))
        )
        browser = newBrowser
        return newBrowser
    }

    @PreDestroy
    fun shutdown() {
        browser?.close()
        playwright?.close()
    }

    // Playwright objects must not be driven from multiple threads concurrently,
    // so access to the single shared browser is serialized here. This still
    // avoids spawning a new Chromium process per request (the previous behavior).
    @Synchronized
    fun convertXmlToSvg(bpmnXml: String, correctIds: List<String>, falsePositiveIds: List<String>, falseNegativeIds: List<String>, theme: String = "light"): String {
        getOrCreateBrowser().newPage().use { page ->
            page.setContent(htmlTemplate)
            page.waitForFunction("() => typeof window.convertBpmn === 'function'")

            val correctIdsString = correctIds.joinToString(",", "[", "]") { "\"$it\"" }
            val falsePositiveIdsString = falsePositiveIds.joinToString(",", "[", "]") { "\"$it\"" }
            val falseNegativeIdsString = falseNegativeIds.joinToString(",", "[", "]") { "\"$it\"" }

            val result = page.evaluate("""xml => window.convertBpmn(xml, $correctIdsString, $falsePositiveIdsString, $falseNegativeIdsString, "$theme")""", bpmnXml)

            val success = (result as Map<*, *>)["success"] as Boolean
            if (!success) {
                val err = result["error"] ?: "Unbekannter Fehler"
                throw IllegalArgumentException("Ungültiges BPMN XML: $err")
            }
            return (result["svg"] as String)
        }
    }
}
