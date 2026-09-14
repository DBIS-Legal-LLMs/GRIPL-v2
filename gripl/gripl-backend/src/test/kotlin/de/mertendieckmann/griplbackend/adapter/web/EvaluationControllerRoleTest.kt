package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.evaluation.MultiEvaluationRunner
import de.mertendieckmann.griplbackend.model.dto.MultiEvaluationRequest
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_USER_ID_ATTRIBUTE
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ResponseStatusException

/**
 * GRIPL-v2#40 — running evaluations (the "Evaluation" surface) is admin/
 * researcher only. Role check runs before the request is touched at all, so
 * the collaborators below never need real stubbing for the forbidden cases.
 */
class EvaluationControllerRoleTest {

    private val runner = mock<MultiEvaluationRunner>()
    private val env = mock<Environment>()
    private val controller = EvaluationController(runner, env)

    private fun exchangeAs(griplRole: String?) =
        MockServerWebExchange.from(MockServerHttpRequest.post("/gdpr/evaluation/stream")).apply {
            attributes[AUTHENTICATED_USER_ID_ATTRIBUTE] = "user-1"
            if (griplRole != null) attributes[AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE] = griplRole
        }

    private val request = MultiEvaluationRequest(
        defaultEvaluationEndpoint = "endpoint",
        models = emptyList(),
        seed = 1,
        datasets = emptyList(),
    )

    @Test
    fun `dpo cannot run a streamed evaluation`() = runBlocking {
        val ex = assertThrows(ResponseStatusException::class.java) {
            runBlocking { controller.evaluateStream(request, exchangeAs("dpo")) }
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
    }

    @Test
    fun `end-user cannot run a markdown evaluation`() = runBlocking {
        val ex = assertThrows(ResponseStatusException::class.java) {
            runBlocking { controller.evaluate(request, exchangeAs("end-user")) }
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
    }

    @Test
    fun `researcher is allowed through the role gate`() {
        runBlocking {
            whenever(env.resolvePlaceholders(org.mockito.kotlin.any())).thenAnswer { it.arguments[0] }
            whenever(runner.runAll(org.mockito.kotlin.any())).thenReturn(emptyFlow())

            // Should not throw past the role gate.
            controller.evaluateStream(request, exchangeAs("researcher"))
        }
    }
}
