package de.mertendieckmann.griplbackend.application

import dev.langchain4j.service.output.OutputParsingException
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Generic over the result type [T] so both the binary (`BpmnAnalysisResult`) and multiclass
 * (`BpmnMulticlassAnalysisResult`) analyzers can share the same retry loop — only the "fix"
 * step differs, since langchain4j needs a distinct typed AiService per result shape to derive
 * its structured-output schema (see `JsonFixAiService`/`MulticlassJsonFixAiService`).
 */
class SafetyNet<T>(
    private val fix: (sessionId: String, error: String) -> T
) {
    private val log = KotlinLogging.logger { }

    /**
     * Tries to execute the given [block] that produces a [T].
     * If an [OutputParsingException] occurs, it will attempt to fix the JSON output using [fix]
     * and retry up to [maxRetries] times. The [sessionId] is used to maintain context in the AI service.
     * Returns a pair of the successfully parsed [T] and the number of retries it took.
     */
    fun safeGuardResultParsing(
        sessionId: String,
        maxRetries: Int,
        block: () -> T
    ): Pair<T, Int> {
        require(maxRetries >= 0) { "maxRetries must be >= 0" }

        return try {
            Pair(block(), 0)
        } catch (original: OutputParsingException) {
            var lastError: Throwable = original

            repeat(maxRetries) {
                log.warn(lastError) { "Parsing failed. Attempting to fix JSON and retry... (Attempt ${it + 1} of $maxRetries)" }
                try {
                    // Reuses the same sessionId as the original request so the fix call has
                    // access to the same conversation memory.
                    return Pair(fix(sessionId, lastError.stackTraceToString()), it + 1)
                } catch (fixError: Throwable) {
                    lastError = fixError
                }
            }

            throw lastError
        }
    }
}
