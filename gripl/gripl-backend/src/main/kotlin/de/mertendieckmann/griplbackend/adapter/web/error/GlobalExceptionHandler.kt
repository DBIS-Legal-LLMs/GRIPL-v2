package de.mertendieckmann.griplbackend.adapter.web.error

import de.mertendieckmann.griplbackend.application.dataset.DatasetImportException
import de.mertendieckmann.griplbackend.application.failure.FailureMapper
import de.mertendieckmann.griplbackend.application.failure.logFailure
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_USER_ID_ATTRIBUTE
import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.model.xml.ModelParseException
import org.camunda.bpm.model.xml.ModelValidationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.MissingRequestValueException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.reactive.resource.NoResourceFoundException
import tools.jackson.core.exc.StreamReadException

/**
 * Turns every exception that leaves a controller into the same JSON body, [ApiError]:
 * the HTTP status, a stable `code`, a short `message` that is safe to show to the user,
 * and a `requestId` that is also in the log line, so an operator can find the cause.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = KotlinLogging.logger {}

    // ResponseStatusException (GRIPL-v2#33's ownership 404/400s, GRIPL-v2#40's role 403s, ...) carries
    // its own correct status and a reason written for the user.
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException, exchange: ServerWebExchange): ResponseEntity<ApiError> {
        val status = HttpStatus.resolve(ex.statusCode.value())
        val code = status?.name ?: ex.statusCode.value().toString()
        val message = ex.reason ?: status?.reasonPhrase
        if (ex.statusCode.is5xxServerError) {
            val requestId = FailureMapper.newRequestId()
            log.error(ex) { "${describe(exchange)} failed [code=$code, requestId=$requestId]: $message" }
            return respond(ex.statusCode.value(), code, message, requestId)
        }
        log.debug { "${describe(exchange)} rejected with ${ex.statusCode.value()}: $message" }
        return respond(ex.statusCode.value(), code, message)
    }

    @ExceptionHandler(ModelParseException::class)
    fun handleInvalidBpmn(ex: ModelParseException, exchange: ServerWebExchange): ResponseEntity<ApiError> =
        clientError(exchange, ex, HttpStatus.BAD_REQUEST, "BPMN_PARSE_ERROR", "BPMN XML could not be parsed")

    @ExceptionHandler(ModelValidationException::class)
    fun handleValidationError(ex: ModelValidationException, exchange: ServerWebExchange): ResponseEntity<ApiError> =
        clientError(exchange, ex, HttpStatus.BAD_REQUEST, "BPMN_VALIDATION_ERROR", "BPMN XML is not valid")

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(ex: NoResourceFoundException, exchange: ServerWebExchange): ResponseEntity<ApiError> =
        clientError(exchange, ex, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "The requested resource was not found")

    @ExceptionHandler(StreamReadException::class)
    fun handleJsonParseException(ex: StreamReadException, exchange: ServerWebExchange): ResponseEntity<ApiError> =
        clientError(exchange, ex, HttpStatus.BAD_REQUEST, "JSON_PARSE_ERROR", "The request body is not valid JSON")

    @ExceptionHandler(DatasetImportException::class)
    fun handleDatasetImportException(ex: DatasetImportException, exchange: ServerWebExchange): ResponseEntity<ApiError> =
        clientError(exchange, ex, HttpStatus.BAD_REQUEST, "DATASET_IMPORT_ERROR", ex.message)

    @ExceptionHandler(MissingRequestValueException::class)
    fun handleMissingRequestValueException(ex: MissingRequestValueException, exchange: ServerWebExchange): ResponseEntity<ApiError> =
        clientError(exchange, ex, HttpStatus.BAD_REQUEST, "MISSING_REQUEST_VALUE", ex.reason ?: "A required part of the request is missing")

    // Everything else, including problems with the language model provider, the knowledge graph service
    // and auth-service: FailureMapper knows the provider's own explanation and what is safe to show.
    @ExceptionHandler(Exception::class)
    fun handleFailure(ex: Exception, exchange: ServerWebExchange): ResponseEntity<ApiError> {
        val requestId = FailureMapper.newRequestId()
        val failure = log.logFailure(
            operation = describe(exchange),
            e = ex,
            context = mapOf("userId" to exchange.getAttribute<String>(AUTHENTICATED_USER_ID_ATTRIBUTE)),
            requestId = requestId
        )
        return respond(failure.status, failure.code, failure.message, requestId)
    }

    private fun clientError(
        exchange: ServerWebExchange,
        ex: Exception,
        status: HttpStatus,
        code: String,
        message: String?
    ): ResponseEntity<ApiError> {
        log.warn { "${describe(exchange)} rejected with ${status.value()} $code: ${ex.javaClass.simpleName}: ${ex.logMessage()?.let { FailureMapper.sanitize(it) }}" }
        return respond(status.value(), code, message)
    }

    // Jackson's message quotes the start of the request body, which is the user's data
    private fun Exception.logMessage(): String? = (this as? StreamReadException)?.originalMessage ?: message

    private fun respond(status: Int, code: String, message: String?, requestId: String? = null): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(ApiError(status = status, code = code, message = message, requestId = requestId))

    private fun describe(exchange: ServerWebExchange): String =
        "${exchange.request.method} ${exchange.request.path.value()}"

    /** `requestId` is only set for failures that are logged as errors, i.e. the ones an operator may have to look into. */
    data class ApiError(val status: Int, val code: String, val message: String?, val requestId: String? = null)
}
