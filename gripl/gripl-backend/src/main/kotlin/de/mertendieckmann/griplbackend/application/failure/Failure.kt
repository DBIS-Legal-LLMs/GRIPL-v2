package de.mertendieckmann.griplbackend.application.failure

import dev.langchain4j.exception.AuthenticationException
import dev.langchain4j.exception.ContentFilteredException
import dev.langchain4j.exception.HttpException
import dev.langchain4j.exception.InternalServerException
import dev.langchain4j.exception.InvalidRequestException
import dev.langchain4j.exception.ModelNotFoundException
import dev.langchain4j.exception.RateLimitException
import dev.langchain4j.exception.TimeoutException
import dev.langchain4j.exception.UnresolvedModelServerException
import dev.langchain4j.service.output.OutputParsingException
import io.github.oshai.kotlinlogging.KLogger
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import org.camunda.bpm.model.xml.ModelParseException
import org.camunda.bpm.model.xml.ModelValidationException
import java.net.ConnectException
import java.net.UnknownHostException
import java.net.http.HttpTimeoutException
import java.util.UUID

/** Stable machine-readable codes the frontend can switch on. Existing codes of the API (e.g. BPMN_PARSE_ERROR) are unchanged. */
enum class FailureCode(val httpStatus: Int) {
    ANALYSIS_ENDPOINT_INVALID(400),
    EVALUATION_ENDPOINT_FAILED(502),
    EVALUATION_TIMEOUT(504),
    LLM_REQUEST_REJECTED(400),
    LLM_CREDITS_EXHAUSTED(402),

    // Not 401/403: those make the frontend think the user's session expired.
    LLM_AUTH_FAILED(502),
    LLM_RATE_LIMITED(429),
    LLM_UNAVAILABLE(502),
    LLM_TIMEOUT(504),
    LLM_BAD_OUTPUT(502),
    RAG_UNAVAILABLE(502),
    AUTH_SERVICE_UNAVAILABLE(502),
    INTERNAL_ERROR(500)
}

/**
 * A failure described in terms that are safe to show to the user: [message] never contains a
 * stack trace, a secret or an internal detail. [upstreamStatus] and [upstreamBody] are for the
 * log only (already sanitized and truncated).
 */
data class Failure(
    val code: String,
    val status: Int,
    val message: String,
    val upstreamStatus: Int? = null,
    val upstreamBody: String? = null
) {
    constructor(
        code: FailureCode,
        message: String,
        upstreamStatus: Int? = null,
        upstreamBody: String? = null
    ) : this(code.name, code.httpStatus, message, upstreamStatus, upstreamBody)
}

/** Thrown by clients of other services (RAG, auth-service) with the failure already described. */
class ServiceFailureException(val failure: Failure, cause: Throwable? = null) :
    RuntimeException(failure.message, cause)

object FailureMapper {

    private const val PROVIDER = "OpenRouter"
    private const val MAX_MESSAGE_LENGTH = 300
    private const val MAX_BODY_LENGTH = 1000
    private const val MAX_CHAIN_DEPTH = 12

    private val objectMapper = jacksonObjectMapper()

    private val secretPatterns = listOf(
        Regex("""sk-[A-Za-z0-9_\-]{8,}"""),
        Regex("""(?i)bearer\s+[A-Za-z0-9._\-]{8,}"""),
        Regex("""eyJ[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]*"""),
        Regex("""(?i)(api[_-]?key|token|secret|password|authorization)(["']?\s*[:=]\s*["']?)[^\s"',}]+""")
    )

    /** Describes [e] for the user; the whole cause chain is searched for a known failure. */
    fun describe(e: Throwable): Failure {
        val chain = causeChain(e)
        chain.firstNotNullOfOrNull { (it as? ServiceFailureException)?.failure }?.let { return it }

        val http = chain.firstNotNullOfOrNull { it as? HttpException }
        val httpStatus = http?.statusCode()

        // langchain4j has no exception type of its own for 402
        if (httpStatus == 402) return fromStatus(httpStatus, upstreamDetail(http), bodyOf(http))

        for (t in chain) {
            // the provider's explanation is the message of the exception that was recognized, not of a wrapper around it
            fromException(t, httpStatus, upstreamDetail(t), bodyOf(t))?.let { return it }
        }
        if (httpStatus != null) return fromStatus(httpStatus, upstreamDetail(http), bodyOf(http))

        return Failure(
            FailureCode.INTERNAL_ERROR,
            "An internal error occurred. Please try again; if it keeps happening, contact the operator and mention the request id."
        )
    }

    /** Removes anything that looks like a credential, collapses whitespace and shortens the text. */
    fun sanitize(text: String, maxLength: Int = MAX_MESSAGE_LENGTH): String {
        var cleaned = text
        secretPatterns.forEach { pattern ->
            cleaned = pattern.replace(cleaned) { match ->
                if (match.groupValues.size > 2) "${match.groupValues[1]}${match.groupValues[2]}[redacted]" else "[redacted]"
            }
        }
        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()
        return if (cleaned.length > maxLength) cleaned.take(maxLength - 1).trimEnd() + "…" else cleaned
    }

    fun newRequestId(): String = UUID.randomUUID().toString().replace("-", "").take(8)

    private fun causeChain(e: Throwable): List<Throwable> {
        val chain = mutableListOf<Throwable>()
        var current: Throwable? = e
        while (current != null && chain.size < MAX_CHAIN_DEPTH && chain.none { it === current }) {
            chain.add(current)
            current = current.cause
        }
        return chain
    }

    private fun fromException(t: Throwable, httpStatus: Int?, detail: String?, body: String?): Failure? = when (t) {
        is InvalidRequestException -> Failure(
            FailureCode.LLM_REQUEST_REJECTED, "$PROVIDER rejected the request" + suffix(detail), httpStatus ?: 400, body
        )
        is ModelNotFoundException -> Failure(
            FailureCode.LLM_REQUEST_REJECTED, "$PROVIDER could not find the requested model" + suffix(detail), httpStatus ?: 404, body
        )
        is ContentFilteredException -> Failure(
            FailureCode.LLM_REQUEST_REJECTED, "$PROVIDER blocked the request with its content filter" + suffix(detail), httpStatus, body
        )
        is AuthenticationException -> authFailed(httpStatus, body)
        is RateLimitException -> rateLimited(httpStatus, body)
        is InternalServerException -> unavailable(httpStatus, detail, body)
        is TimeoutException, is HttpTimeoutException -> timeout(httpStatus, body)
        is UnresolvedModelServerException, is ConnectException, is UnknownHostException -> Failure(
            FailureCode.LLM_UNAVAILABLE, "$PROVIDER could not be reached. Please try again in a moment.", httpStatus, body
        )
        is ModelParseException -> Failure("BPMN_PARSE_ERROR", 400, "The BPMN XML could not be parsed.")
        is ModelValidationException -> Failure("BPMN_VALIDATION_ERROR", 400, "The BPMN XML is not a valid BPMN model.")
        is OutputParsingException -> Failure(
            FailureCode.LLM_BAD_OUTPUT,
            "The model's answer could not be read as the expected JSON. Try again or choose another model.",
            httpStatus, body
        )
        else -> null
    }

    private fun fromStatus(status: Int, detail: String?, body: String?): Failure = when {
        status == 402 -> Failure(
            FailureCode.LLM_CREDITS_EXHAUSTED,
            "$PROVIDER reports that the account has no credits left. Add credits and try again.", status, body
        )
        status == 401 || status == 403 -> authFailed(status, body)
        status == 408 -> timeout(status, body)
        status == 429 -> rateLimited(status, body)
        status >= 500 -> unavailable(status, detail, body)
        else -> Failure(FailureCode.LLM_REQUEST_REJECTED, "$PROVIDER rejected the request" + suffix(detail), status, body)
    }

    private fun authFailed(status: Int?, body: String?) = Failure(
        FailureCode.LLM_AUTH_FAILED,
        "$PROVIDER did not accept the API key. Check the key in your account settings.", status, body
    )

    private fun rateLimited(status: Int?, body: String?) = Failure(
        FailureCode.LLM_RATE_LIMITED,
        "$PROVIDER is rate limiting requests. Wait a moment and try again, or lower the number of concurrent requests.", status, body
    )

    private fun timeout(status: Int?, body: String?) = Failure(
        FailureCode.LLM_TIMEOUT, "$PROVIDER did not answer in time. Please try again.", status, body
    )

    private fun unavailable(status: Int?, detail: String?, body: String?) = Failure(
        FailureCode.LLM_UNAVAILABLE, "$PROVIDER had a problem on its side" + suffix(detail) + ". Please try again later.", status, body
    )

    private fun suffix(detail: String?): String = if (detail.isNullOrBlank()) "" else ": $detail"

    private fun bodyOf(t: Throwable?): String? = t?.message?.let { sanitize(it, MAX_BODY_LENGTH) }

    /** The provider's own explanation: the `error.message` of its JSON body, or the plain text of the exception. */
    private fun upstreamDetail(t: Throwable?): String? {
        val raw = t?.message?.takeIf { it.isNotBlank() } ?: return null
        val text = jsonErrorMessage(raw) ?: raw
        return sanitize(text).trimEnd('.').takeIf { it.isNotBlank() }
    }

    private fun jsonErrorMessage(raw: String): String? {
        val start = raw.indexOf('{')
        if (start < 0) return null
        val node = runCatching { objectMapper.readTree(raw.substring(start)) }.getOrNull() ?: return null
        val error: JsonNode = node.get("error") ?: node
        val message = if (error.isString) error.asString() else error.get("message")?.takeIf { it.isString }?.asString()
        return message?.takeIf { it.isNotBlank() }
    }
}

/**
 * Logs one error line for a failed [operation] and returns the user-safe [Failure].
 * The line carries the request id and the given context (user id, job or run id, ...); the
 * attached exception brings the whole cause chain. Never pass secrets as context.
 */
fun KLogger.logFailure(
    operation: String,
    e: Throwable,
    context: Map<String, Any?> = emptyMap(),
    requestId: String? = null
): Failure {
    val failure = FailureMapper.describe(e)
    val fields = buildList {
        add("code=${failure.code}")
        requestId?.let { add("requestId=$it") }
        context.forEach { (key, value) -> if (value != null) add("$key=$value") }
        failure.upstreamStatus?.let { add("upstreamStatus=$it") }
        failure.upstreamBody?.let { add("upstreamBody=\"$it\"") }
    }
    error(e) { "$operation failed [${fields.joinToString(", ")}]: ${failure.message}" }
    return failure
}
