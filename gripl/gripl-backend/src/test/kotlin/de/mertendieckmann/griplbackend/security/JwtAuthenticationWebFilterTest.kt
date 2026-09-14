package de.mertendieckmann.griplbackend.security

import com.sun.net.httpserver.HttpServer
import de.mertendieckmann.griplbackend.config.JwtProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Jwks
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.atomic.AtomicInteger

/**
 * End-to-end unit test for the auth-service JWKS verification path (GRIPL-v2#32):
 * a throwaway HTTP server plays auth-service and publishes a JWKS, and the
 * filter must accept a token signed with the matching private key and reject
 * everything else — with no shared secret anywhere.
 */
class JwtAuthenticationWebFilterTest {

    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val kid = "test-key-1"

    private lateinit var server: HttpServer
    private lateinit var filter: JwtAuthenticationWebFilter
    private val jwksHits = AtomicInteger(0)

    @BeforeEach
    fun setUp() {
        val jwk = Jwks.builder().key(keyPair.public as RSAPublicKey).id(kid).build()
        val jwksJson = """{"keys":[${Jwks.json(jwk)}]}"""

        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/.well-known/jwks.json") { exchange ->
            jwksHits.incrementAndGet()
            val bytes = jwksJson.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()

        val props = JwtProperties().apply {
            jwksUri = "http://127.0.0.1:${server.address.port}/.well-known/jwks.json"
        }
        filter = JwtAuthenticationWebFilter(JwksProvider(props))
    }

    @AfterEach
    fun tearDown() {
        server.stop(0)
    }

    private fun signedToken(
        subject: String?,
        signingKid: String = kid,
        appRoles: Map<String, String>? = null,
    ): String {
        val builder = Jwts.builder().header().keyId(signingKid).and()
        if (subject != null) builder.subject(subject)
        if (appRoles != null) builder.claim("app_roles", appRoles)
        return builder.signWith(keyPair.private, Jwts.SIG.RS256).compact()
    }

    private fun runFilter(authHeader: String?): Pair<MockServerWebExchange, Boolean> {
        val request = MockServerHttpRequest.get("/datasets").apply {
            if (authHeader != null) header(HttpHeaders.AUTHORIZATION, authHeader)
        }.build()
        val exchange = MockServerWebExchange.from(request)
        var chainCalled = false
        val chain = WebFilterChain {
            chainCalled = true
            Mono.empty()
        }
        filter.filter(exchange, chain).block()
        return exchange to chainCalled
    }

    @Test
    fun `accepts a valid auth-service token and exposes the subject`() {
        val (exchange, chainCalled) = runFilter("Bearer ${signedToken("user-123")}")

        assertEquals(true, chainCalled)
        assertEquals("user-123", exchange.getAttribute<String>(AUTHENTICATED_USER_ID_ATTRIBUTE))
        assertNull(exchange.response.statusCode)
    }

    @Test
    fun `rejects a request with no Authorization header`() {
        val (exchange, chainCalled) = runFilter(null)

        assertEquals(false, chainCalled)
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
    }

    @Test
    fun `rejects a token whose signature does not match the JWKS`() {
        val foreignKey = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val forged = Jwts.builder().subject("attacker").header().keyId(kid).and()
            .signWith(foreignKey.private, Jwts.SIG.RS256).compact()

        val (exchange, chainCalled) = runFilter("Bearer $forged")

        assertEquals(false, chainCalled)
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
    }

    @Test
    fun `rejects a token signed with an unknown key id`() {
        val (exchange, chainCalled) = runFilter("Bearer ${signedToken("user-123", signingKid = "rotated-away")}")

        assertEquals(false, chainCalled)
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
    }

    @Test
    fun `rejects a token with no subject`() {
        val (exchange, chainCalled) = runFilter("Bearer ${signedToken(null)}")

        assertEquals(false, chainCalled)
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
    }

    @Test
    fun `exposes the caller's resolved gripl role from app_roles (GRIPL-v2#40)`() {
        val token = signedToken("user-123", appRoles = mapOf("gripl" to "researcher", "ragulate" to "admin"))
        val (exchange, chainCalled) = runFilter("Bearer $token")

        assertEquals(true, chainCalled)
        assertEquals("researcher", exchange.getAttribute<String>(AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE))
    }

    @Test
    fun `no gripl role attribute when app_roles has no gripl entry (or is absent)`() {
        val withoutGripl = signedToken("user-123", appRoles = mapOf("ragulate" to "admin"))
        assertNull(runFilter("Bearer $withoutGripl").first.getAttribute<String>(AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE))

        val noClaimAtAll = signedToken("user-123")
        assertNull(runFilter("Bearer $noClaimAtAll").first.getAttribute<String>(AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE))
    }

    @Test
    fun `caches the JWKS across requests`() {
        runFilter("Bearer ${signedToken("user-1")}")
        runFilter("Bearer ${signedToken("user-2")}")

        assertEquals(1, jwksHits.get())
    }
}
