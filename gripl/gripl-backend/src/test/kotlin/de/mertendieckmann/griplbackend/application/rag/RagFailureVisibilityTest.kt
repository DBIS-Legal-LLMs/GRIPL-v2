package de.mertendieckmann.griplbackend.application.rag

import com.sun.net.httpserver.HttpServer
import de.mertendieckmann.griplbackend.adapter.rag.RagApiClient
import de.mertendieckmann.griplbackend.adapter.rag.RagasApiClient
import de.mertendieckmann.griplbackend.application.BpmnExtractor
import de.mertendieckmann.griplbackend.application.failure.Failure
import de.mertendieckmann.griplbackend.application.failure.FailureCode
import de.mertendieckmann.griplbackend.application.failure.ServiceFailureException
import de.mertendieckmann.griplbackend.config.RagApiProperties
import de.mertendieckmann.griplbackend.evaluation.service.RagasEvaluationService
import de.mertendieckmann.griplbackend.model.dto.AnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.RagElementContext
import de.mertendieckmann.griplbackend.model.dto.RagMode
import de.mertendieckmann.griplbackend.model.dto.RagResponseWrapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.function.client.WebClient
import java.net.InetSocketAddress

/** A knowledge graph or Ragas problem no longer degrades the result silently: the user gets a note, the log gets the cause. */
class RagFailureVisibilityTest {

    private var server: HttpServer? = null

    @AfterEach
    fun stopServer() {
        server?.stop(0)
    }

    private val unavailable = ServiceFailureException(
        Failure(FailureCode.RAG_UNAVAILABLE, "The GDPR knowledge graph service answered with status 503.")
    )

    private val elements = BpmnExtractor().extractBpmnElements(
        """<?xml version="1.0" encoding="UTF-8"?>
        <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" id="D" targetNamespace="x">
          <bpmn:process id="P">
            <bpmn:task id="A" name="Collect customer data"/>
            <bpmn:task id="B" name="Store invoice"/>
            <bpmn:task id="C" name="Send newsletter"/>
          </bpmn:process>
        </bpmn:definitions>"""
    )

    private fun augmenter(client: RagApiClient) =
        RagContextAugmenter(client, RagApiProperties().apply { url = "http://localhost:1" })

    @Test
    fun `when every query fails the user is told the analysis ran without context`() = runBlocking {
        val client = mock<RagApiClient>()
        whenever(client.queryRag(any(), any())).thenThrow(unavailable)

        val result = augmenter(client).fetchRagContext(elements, RagMode.HYBRID)

        assertTrue(result.context.isEmpty())
        val warning = result.warnings.single()
        assertTrue(warning.contains("ran without GDPR context"), warning)
        assertTrue(warning.contains("answered with status 503"), warning)
        assertFalse(warning.contains("customer"), "the warning must not contain the query text: $warning")
    }

    @Test
    fun `when some queries fail the user is told the context is incomplete`() = runBlocking {
        val client = mock<RagApiClient>()
        whenever(client.queryRag(any(), any())).thenAnswer { invocation ->
            val query = invocation.getArgument<String>(0)
            if (query.contains("newsletter")) throw unavailable
            RagResponseWrapper(query, "hybrid", "success", mapOf("entities" to emptyList<Any>()))
        }

        val result = augmenter(client).fetchRagContext(elements, RagMode.HYBRID)

        assertEquals(2, result.context.size)
        assertTrue(result.warnings.single().contains("1 of 3 queries"), result.warnings.toString())
    }

    @Test
    fun `no warning when every query worked`() = runBlocking {
        val client = mock<RagApiClient>()
        whenever(client.queryRag(any(), any())).thenAnswer {
            RagResponseWrapper(it.getArgument<String>(0), "hybrid", "success", emptyMap())
        }

        assertTrue(augmenter(client).fetchRagContext(elements, RagMode.HYBRID).warnings.isEmpty())
    }

    private fun serve(status: Int, body: String): String {
        val stub = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        stub.createContext("/") { exchange ->
            exchange.requestBody.readAllBytes()
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        stub.start()
        server = stub
        return "http://127.0.0.1:${stub.address.port}"
    }

    @Test
    fun `a failing knowledge graph service is reported with its status and without internals`() {
        val client = RagApiClient(WebClient.builder(), RagApiProperties().apply { url = serve(503, "stack trace at com.internal.Secret") })

        val ex = assertThrows(ServiceFailureException::class.java) { runBlocking { client.queryRag("secret user text", RagMode.HYBRID) } }

        assertEquals("RAG_UNAVAILABLE", ex.failure.code)
        assertEquals(503, ex.failure.upstreamStatus)
        assertEquals("The GDPR knowledge graph service answered with status 503.", ex.message)
    }

    @Test
    fun `an unreachable knowledge graph service is reported as such`() {
        val client = RagApiClient(WebClient.builder(), RagApiProperties().apply { url = "http://127.0.0.1:1" })

        val ex = assertThrows(ServiceFailureException::class.java) { runBlocking { client.queryRag("q", RagMode.HYBRID) } }

        assertEquals("The GDPR knowledge graph service could not be reached.", ex.message)
    }

    @Test
    fun `a failing Ragas service gives a note instead of silently missing metrics`() = runBlocking {
        val ragas = mock<RagasApiClient>()
        whenever(ragas.evaluate(any())).thenThrow(
            ServiceFailureException(Failure(FailureCode.RAGAS_UNAVAILABLE, "The RAG quality scoring service could not be reached."))
        )
        val response = AnalysisResponse(
            criticalElements = listOf(AnalysisResponse.CriticalElement(id = "A", name = "Collect customer data", reason = "personal data")),
            ragContext = mapOf("A" to RagElementContext(activityName = "Collect customer data", entities = emptyList(), relationships = emptyList(), documents = emptyList())),
            ragPromptContext = listOf("Under GDPR, a controller processes data.")
        )

        val score = RagasEvaluationService(ragas).scoreTestCase(response, elements)

        assertNull(score.metrics)
        val warning = score.warning!!
        assertTrue(warning.contains("could not be reached"), warning)
        assertTrue(warning.contains("classification metrics are not affected"), warning)
    }
}
