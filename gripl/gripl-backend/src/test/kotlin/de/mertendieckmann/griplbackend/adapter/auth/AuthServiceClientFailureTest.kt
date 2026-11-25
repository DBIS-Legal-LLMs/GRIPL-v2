package de.mertendieckmann.griplbackend.adapter.auth

import com.sun.net.httpserver.HttpServer
import de.mertendieckmann.griplbackend.application.failure.ServiceFailureException
import de.mertendieckmann.griplbackend.config.AuthServiceProperties
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import java.net.InetSocketAddress

/** A login service that rejects the token must not look like one that is down. */
class AuthServiceClientFailureTest {

    private var server: HttpServer? = null

    @AfterEach
    fun stopServer() {
        server?.stop(0)
    }

    private fun clientAnswering(status: Int): AuthServiceClient {
        val stub = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        stub.createContext("/") { exchange ->
            exchange.sendResponseHeaders(status, -1)
            exchange.close()
        }
        stub.start()
        server = stub
        return AuthServiceClient(WebClient.builder(), AuthServiceProperties().apply { baseUrl = "http://127.0.0.1:${stub.address.port}" })
    }

    private fun lookUp(client: AuthServiceClient) =
        assertThrows(ServiceFailureException::class.java) { runBlocking { client.getOpenRouterApiKey("a-token") } }

    @Test
    fun `a rejected token is a 401 so that the frontend renews the session`() {
        val failure = lookUp(clientAnswering(401)).failure

        assertEquals("AUTH_SERVICE_REJECTED_TOKEN", failure.code)
        assertEquals(401, failure.status)
    }

    @Test
    fun `an auth-service error is reported as unavailable and not as the user's fault`() {
        val failure = lookUp(clientAnswering(500)).failure

        assertEquals("AUTH_SERVICE_UNAVAILABLE", failure.code)
        assertEquals(502, failure.status)
        assertEquals(500, failure.upstreamStatus)
        assertFalse(failure.message.contains("a-token"))
    }
}
