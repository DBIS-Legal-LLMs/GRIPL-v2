package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.adapter.web.utils.ControllerUtils
import de.mertendieckmann.griplbackend.evaluation.MultiEvaluationRunner
import de.mertendieckmann.griplbackend.model.dto.EvaluationReportStepInfo
import de.mertendieckmann.griplbackend.model.dto.ModelReportEnvelope
import de.mertendieckmann.griplbackend.model.dto.MultiEvaluationRequest
import de.mertendieckmann.griplbackend.security.requirePrivilegedGriplRole
import io.swagger.v3.oas.annotations.Operation
import kotlinx.coroutines.flow.Flow
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange

// GRIPL-v2#40: running evaluations is part of the "Evaluation" surface,
// admin/researcher only — dpo and end-user only get the sandbox.
@RestController
@RequestMapping("/gdpr/evaluation")
class EvaluationController(
    private val multiEvaluationRunner: MultiEvaluationRunner,
    private val env: Environment
) {

    @Operation(summary = "Evaluates the classification algorithm against the dataset (markdown)")
    @PostMapping("/markdown", produces = [MediaType.TEXT_MARKDOWN_VALUE])
    suspend fun evaluate(@RequestBody request: MultiEvaluationRequest, exchange: ServerWebExchange): String {
        exchange.requirePrivilegedGriplRole()
        val sb = StringBuilder()
        var currentLabel: String? = null
        val resolvedRequest = ControllerUtils.resolveEnvironmentVariables(request, env)
            ?: throw IllegalArgumentException("Invalid request after resolving environment variables.")

        multiEvaluationRunner.runAll(resolvedRequest).collect { envelope ->
            val (label, report) = envelope

            if (currentLabel != label) {
                if (currentLabel != null) sb.appendLine()
                sb.appendLine("# Modell: $label").appendLine()
                currentLabel = label
            }

            if (report !is EvaluationReportStepInfo) {
                val md = report.toMarkdown()
                if (md.isNotBlank()) {
                    sb.appendLine(md).appendLine()
                }
            }
        }

        return sb.toString().trimEnd()
    }

    @Operation(summary = "Evaluates the classification algorithm against the dataset (NDJSON stream)")
    @PostMapping("/stream", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    suspend fun evaluateStream(@RequestBody request: MultiEvaluationRequest, exchange: ServerWebExchange): Flow<ModelReportEnvelope> {
        exchange.requirePrivilegedGriplRole()
        val resolvedRequest = ControllerUtils.resolveEnvironmentVariables(request, env)
            ?: throw IllegalArgumentException("Invalid request after resolving environment variables.")
        return multiEvaluationRunner.runAll(resolvedRequest)
    }
}
