package de.mertendieckmann.griplbackend.adapter.web.error

import dev.langchain4j.service.output.OutputParsingException
import org.camunda.bpm.model.xml.ModelParseException
import org.camunda.bpm.model.xml.ModelValidationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.MissingRequestValueException
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    // Must stay ahead of the catch-all RuntimeException handler below in
    // intent (Spring picks the most specific match regardless of declaration
    // order, but this is the one that matters): ResponseStatusException
    // (GRIPL-v2#33's ownership 404/400s, GRIPL-v2#40's role 403s, ...) carries
    // its own correct status — without this it was falling through to the
    // generic 500 handler, silently turning every "not found"/"forbidden"
    // into an internal error. Found while testing #40's role gate for real.
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ApiError> =
        ResponseEntity
            .status(ex.statusCode)
            .body(ApiError(code = ex.statusCode.toString(), message = ex.reason))

    @ExceptionHandler(ModelParseException::class)
    fun handleInvalidBpmn(ex: ModelParseException): ResponseEntity<ApiError> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiError(code = "BPMN_PARSE_ERROR", message = "BPMN XMl could not be parsed"))
            .also { ex.printStackTrace() }

    @ExceptionHandler(ModelValidationException::class)
    fun handleValidationError(ex: ModelValidationException): ResponseEntity<ApiError> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiError(code = "BPMN_VALIDATION_ERROR", message = "BPMN XML is not valid"))
            .also { ex.printStackTrace() }

    @ExceptionHandler(org.springframework.web.reactive.resource.NoResourceFoundException::class)
    fun handleNoResourceFoundException(ex: org.springframework.web.reactive.resource.NoResourceFoundException): ResponseEntity<ApiError> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiError(code = "RESOURCE_NOT_FOUND", message = "The requested resource was not found"))
            .also { ex.printStackTrace() }

    @ExceptionHandler(tools.jackson.core.exc.StreamReadException::class)
    fun handleJsonParseException(ex: tools.jackson.core.exc.StreamReadException): ResponseEntity<ApiError> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiError(code = "JSON_PARSE_ERROR", message = ex.message))
            .also { ex.printStackTrace() }

    @ExceptionHandler(dev.langchain4j.exception.InvalidRequestException::class)
    fun handleInterruptedException(ex: dev.langchain4j.exception.InvalidRequestException): ResponseEntity<ApiError> =
        ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError(code = "INTERRUPTED", message = ex.message))
            .also { print(ex) }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<ApiError> =
        ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError(code = "INTERNAL_ERROR", message = "There was an internal error processing your request: ${ex.localizedMessage}"))
            .also { ex.printStackTrace() }

    @ExceptionHandler(MissingRequestValueException::class)
    fun handleMissingRequestValueException(ex: MissingRequestValueException): ResponseEntity<ApiError> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiError(code = "MISSING_REQUEST_VALUE", message = ex.message))
            .also { ex.printStackTrace() }

    @ExceptionHandler(dev.langchain4j.exception.AuthenticationException::class)
    fun handleAuthenticationException(ex: dev.langchain4j.exception.AuthenticationException): ResponseEntity<ApiError> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiError(code = "AUTHENTICATION_ERROR", message = ex.message))
            .also { ex.printStackTrace() }

    @ExceptionHandler(dev.langchain4j.exception.RateLimitException::class)
    fun handleRateLimitException(ex: dev.langchain4j.exception.RateLimitException): ResponseEntity<ApiError> =
        ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ApiError(code = "RATE_LIMIT_EXCEEDED", message = ex.message))
            .also { ex.printStackTrace() }

    @ExceptionHandler(OutputParsingException::class)
    fun handleOutputParsingException(ex: OutputParsingException): ResponseEntity<ApiError> =
        ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError(code = "OUTPUT_PARSING_ERROR", message = "There was an error parsing the output from the AI service: ${ex.localizedMessage}"))
            .also { ex.printStackTrace() }

    data class ApiError(val code: String, val message: String?)
}