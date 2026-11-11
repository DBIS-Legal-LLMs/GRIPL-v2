package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.adapter.web.utils.ControllerUtils.zipResponse
import de.mertendieckmann.griplbackend.application.dataset.CreatedLabelingDataset
import de.mertendieckmann.griplbackend.application.dataset.LabelExportSummary
import de.mertendieckmann.griplbackend.application.dataset.ProcessModelLabelExportService
import de.mertendieckmann.griplbackend.model.dto.CreateLabelingDatasetRequest
import de.mertendieckmann.griplbackend.security.authenticatedUserId
import de.mertendieckmann.griplbackend.security.requirePrivilegedGriplRole
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import tools.jackson.databind.ObjectMapper
import java.net.URLEncoder
import java.time.LocalDate

/**
 * Hands Process Analysis models over to the labeling tool, with the LLM analysis results as
 * pre-filled labels. Shares the `/process-models` path with [ProcessModelController], but is a
 * labeling feature: like the dataset import it is only open to the labeling roles.
 */
@RestController
@RequestMapping("/process-models")
class ProcessModelLabelingController(
    private val labelExportService: ProcessModelLabelExportService,
    private val objectMapper: ObjectMapper
) {

    companion object {
        /** URI-encoded JSON [LabelExportSummary] of a ZIP export (the ZIP itself must stay importable as is). */
        const val EXPORT_SUMMARY_HEADER = "X-Gripl-Export-Summary"
    }

    @Operation(
        summary = "Export process models for labeling",
        description = "Exports the given process models (all of the caller's if **ids** is omitted) as one dataset in " +
            "the labeling import format (ZIP with dataset.csv and evaluation_data.csv). The LLM analysis results become " +
            "the labels; labels for elements no longer in the saved diagram are dropped. What happened to the labels " +
            "is reported in the **$EXPORT_SUMMARY_HEADER** header (URI-encoded JSON)."
    )
    @GetMapping("/export-labels", produces = ["application/zip"])
    fun exportForLabeling(
        @RequestParam("ids", required = false) ids: List<Long>?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ByteArray>> {
        val userId = exchange.authenticatedUserId()
        exchange.requirePrivilegedGriplRole()
        return Mono.fromCallable {
            val export = labelExportService.exportAsZip(userId, ids)
            // Header values must be ASCII, model names may not be; %20 so JS decodeURIComponent reads it back.
            val summary = URLEncoder.encode(objectMapper.writeValueAsString(export.summary), Charsets.UTF_8)
                .replace("+", "%20")
            zipResponse(export.zip, "gripl-llm-prelabels-${LocalDate.now()}.zip", mapOf(EXPORT_SUMMARY_HEADER to summary))
        }.subscribeOn(Schedulers.boundedElastic())
    }

    @Operation(
        summary = "Create a labeling dataset from process models",
        description = "Same content as **/export-labels**, but created directly as a new labeling dataset owned by the " +
            "caller instead of being downloaded as ZIP."
    )
    @PostMapping("/labeling-dataset", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createLabelingDataset(
        @RequestBody request: CreateLabelingDatasetRequest,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<CreatedLabelingDataset>> {
        val userId = exchange.authenticatedUserId()
        exchange.requirePrivilegedGriplRole()
        return Mono.fromCallable { labelExportService.createDataset(userId, request.ids) }
            .subscribeOn(Schedulers.boundedElastic())
            .map { ResponseEntity.status(HttpStatus.CREATED).body(it) }
    }
}
