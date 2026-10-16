package de.mertendieckmann.griplbackend.evaluation

import de.mertendieckmann.griplbackend.application.analyzer.AnalysisService
import de.mertendieckmann.griplbackend.evaluation.service.HttpEvaluator
import de.mertendieckmann.griplbackend.model.dto.AnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.CustomAnalysisEndpoint
import de.mertendieckmann.griplbackend.model.dto.CustomAnalysisResponseType
import de.mertendieckmann.griplbackend.model.dto.EvaluationRequest
import de.mertendieckmann.griplbackend.model.dto.RagMode
import de.mertendieckmann.griplbackend.repository.CustomAnalysisEndpointRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * The evaluator used to call this app's own /gdpr/analysis/... endpoints over HTTP with no login
 * token, so the JWT filter rejected every test case with an empty 401. Own endpoints are now run
 * in-process through AnalysisService.
 */
class HttpEvaluatorInProcessTest {

    private val analysisService = mock<AnalysisService>()
    private val customRepo = mock<CustomAnalysisEndpointRepository>()
    private val evaluator = HttpEvaluator(customRepo, analysisService)

    private val response = AnalysisResponse(
        criticalElements = listOf(AnalysisResponse.CriticalElement(id = "T", name = "task", reason = "stores data")),
        amountOfRetries = 1
    )

    private fun request(endpoint: String) = EvaluationRequest(
        evaluationEndpoint = endpoint, datasets = emptyList(), useRag = true, ragMode = RagMode.LOCAL, activitiesOnly = true
    )

    @Test
    fun `prompt-engineering runs through AnalysisService with the request's settings`() = runBlocking<Unit> {
        whenever(analysisService.analyzePromptEngineering(any(), anyOrNullLlm(), eq(true), eq(RagMode.LOCAL), eq(true)))
            .thenReturn(response)

        val result = evaluator.evaluate("<xml/>", request("/gdpr/analysis/prompt-engineering"))

        assertEquals(listOf("T"), result.expectedValues.map { it.value })
        assertEquals(1, result.amountOfRetries)
    }

    @Test
    fun `a custom endpoint uses its own fixed RAG settings, not the request's`() = runBlocking<Unit> {
        whenever(customRepo.getById(7)).thenReturn(
            CustomAnalysisEndpoint(7, "e", "my prompt", CustomAnalysisResponseType.BINARY, false, null, "t", "t")
        )
        whenever(analysisService.analyzeCustomBinary(any(), eq("my prompt"), anyOrNullLlm(), eq(false), eq(RagMode.HYBRID), eq(true)))
            .thenReturn(response)

        evaluator.evaluate("<xml/>", request("/gdpr/analysis/custom/7"))

        verify(analysisService).analyzeCustomBinary(any(), eq("my prompt"), anyOrNullLlm(), eq(false), eq(RagMode.HYBRID), eq(true))
    }

    @Test
    fun `an unknown own endpoint fails with a readable message instead of a silent 401`() {
        val ex = assertThrows(RuntimeException::class.java) {
            runBlocking { evaluator.evaluate("<xml/>", request("/gdpr/analysis/nope")) }
        }
        assertEquals(true, ex.message!!.contains("Unknown analysis endpoint"))
    }

    private fun anyOrNullLlm() = org.mockito.kotlin.anyOrNull<de.mertendieckmann.griplbackend.config.LlmConfig.Companion.LlmPropsOverride>()
}
