package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.adapter.web.utils.ControllerUtils
import de.mertendieckmann.griplbackend.ai.MulticlassBpmnAnalysisAiServiceFactory
import de.mertendieckmann.griplbackend.ai.PromptBpmnAnalysisAiServiceFactory
import de.mertendieckmann.griplbackend.application.analyzer.AnalysisService
import de.mertendieckmann.griplbackend.config.LlmConfig
import de.mertendieckmann.griplbackend.model.dto.AnalysisEndpoint
import de.mertendieckmann.griplbackend.model.dto.AnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.CustomAnalysisResponseType
import de.mertendieckmann.griplbackend.model.dto.DefaultAnalysisPrompt
import de.mertendieckmann.griplbackend.model.dto.RagMode
import de.mertendieckmann.griplbackend.model.dto.MulticlassAnalysisResponse
import de.mertendieckmann.griplbackend.repository.CustomAnalysisEndpointRepository
import io.swagger.v3.oas.annotations.Operation
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@RestController
@RequestMapping("/gdpr/analysis")
class AnalysisController(
    private val analysisService: AnalysisService,
    private val customAnalysisEndpointRepository: CustomAnalysisEndpointRepository,
    @Qualifier("analysisEndpoints") private val analysisEndpoints: List<AnalysisEndpoint>
) {

    @Operation(
        summary = "Get all available analysis endpoints",
        description = "Returns the built-in analysis endpoints plus every user-uploaded custom endpoint."
    )
    @GetMapping("/endpoints", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAnalysisEndpoints(): Mono<ResponseEntity<List<AnalysisEndpoint>>> {
        return Mono.fromCallable {
            val customEndpoints = customAnalysisEndpointRepository.listAll().map {
                AnalysisEndpoint(
                    name = it.name,
                    endpoint = "/gdpr/analysis/custom/${it.id}",
                    responseType = it.responseType
                )
            }
            analysisEndpoints + customEndpoints
        }.subscribeOn(Schedulers.boundedElastic()).map { ResponseEntity(it, HttpStatus.OK) }
    }

    @Operation(
        summary = "Get the built-in analyzers' default prompts",
        description = "Returns the prompt text each built-in endpoint uses, read-only — lets users inspect what a" +
            " built-in endpoint actually does without being able to edit it."
    )
    @GetMapping("/default-prompts", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getDefaultPrompts(): List<DefaultAnalysisPrompt> {
        return listOf(
            DefaultAnalysisPrompt(
                name = "Zero-shot Baseline",
                responseType = CustomAnalysisResponseType.BINARY,
                promptText = PromptBpmnAnalysisAiServiceFactory.defaultBasePrompt
            ),
            DefaultAnalysisPrompt(
                name = "Multiclass Analysis",
                responseType = CustomAnalysisResponseType.MULTICLASS,
                promptText = MulticlassBpmnAnalysisAiServiceFactory.defaultPrompt
            )
        )
    }

    @Operation(
        summary = "Analyzes BPMN-XML for GDPR relevance with prompt engineering",
        description = "Upload a BPMN XML document (file part **bpmnFile**). The service analyzes it with an LLM, and returns a list"
            + " of GDPR-relevant elements found in the BPMN model, including the reasoning for each element."
    )
    @PostMapping(
        "/prompt-engineering",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun analyzeBpmnForGdprPromptEngineering(
        @RequestPart("bpmnFile") file: FilePart,
        @RequestPart("llmProps", required = false) llmPropsOverrides: LlmConfig.Companion.LlmPropsOverride? = null,
        @RequestPart("useRag", required = false) useRagPart: FormFieldPart?,
        @RequestPart("ragMode", required = false) ragModePart: FormFieldPart?,
        @RequestPart("activitiesOnly", required = false) activitiesOnlyPart: FormFieldPart?
    ): Mono<ResponseEntity<AnalysisResponse>> {

        val useRag = useRagPart?.value()?.toBooleanStrictOrNull() ?: false
        val ragMode = parseRagMode(ragModePart)
        val activitiesOnly = activitiesOnlyPart?.value()?.toBooleanStrictOrNull() ?: false

        val bpmnXmlMono: Mono<String> = ControllerUtils.getBpmnXmlMono(file)

        return bpmnXmlMono.flatMap { bpmnXml ->
            Mono.fromCallable {
                analysisService.analyzePromptEngineering(
                    bpmnXml = bpmnXml,
                    llmPropsOverride = llmPropsOverrides,
                    useRag = useRag,
                    ragMode = ragMode,
                    activitiesOnly = activitiesOnly
                )
            }.subscribeOn(Schedulers.boundedElastic())
        }.map { ResponseEntity.ok(it) }
    }

    @Operation(
        summary = "Analyzes BPMN-XML for GDPR processing classes",
        description = "Upload a BPMN XML document (file part **bpmnFile**). The service analyzes it with an LLM and returns GDPR-relevant activity elements classified into processing classes."
    )
    @PostMapping(
        "/multiclass",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun analyzeBpmnForGdprMulticlass(
        @RequestPart("bpmnFile") file: FilePart,
        @RequestPart("llmProps", required = false) llmPropsOverrides: LlmConfig.Companion.LlmPropsOverride? = null,
        @RequestPart("useRag", required = false) useRagPart: FormFieldPart?,
        @RequestPart("ragMode", required = false) ragModePart: FormFieldPart?,
        @RequestPart("activitiesOnly", required = false) activitiesOnlyPart: FormFieldPart?
    ): Mono<ResponseEntity<MulticlassAnalysisResponse>> {

        val useRag = useRagPart?.value()?.toBooleanStrictOrNull() ?: false
        val ragMode = parseRagMode(ragModePart)
        val activitiesOnly = activitiesOnlyPart?.value()?.toBooleanStrictOrNull() ?: false

        val bpmnXmlMono: Mono<String> = ControllerUtils.getBpmnXmlMono(file)

        return bpmnXmlMono.flatMap { bpmnXml ->
            Mono.fromCallable {
                analysisService.analyzeMulticlass(
                    bpmnXml = bpmnXml,
                    llmPropsOverride = llmPropsOverrides,
                    useRag = useRag,
                    ragMode = ragMode,
                    activitiesOnly = activitiesOnly
                )
            }.subscribeOn(Schedulers.boundedElastic())
        }.map { ResponseEntity.ok(it) }
    }

    @Operation(
        summary = "Analyzes BPMN-XML with a user-uploaded custom endpoint",
        description = "Upload a BPMN XML document (file part **bpmnFile**). Runs it against the stored prompt of the custom" +
            " analysis endpoint identified by [id], returning either a binary or multiclass result depending on how" +
            " that endpoint was configured."
    )
    @PostMapping(
        "/custom/{id}",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun analyzeBpmnForGdprCustom(
        @PathVariable id: Long,
        @RequestPart("bpmnFile") file: FilePart,
        @RequestPart("llmProps", required = false) llmPropsOverrides: LlmConfig.Companion.LlmPropsOverride? = null,
        @RequestPart("activitiesOnly", required = false) activitiesOnlyPart: FormFieldPart?
    ): Mono<ResponseEntity<Any>> {

        val activitiesOnly = activitiesOnlyPart?.value()?.toBooleanStrictOrNull() ?: false

        val bpmnXmlMono: Mono<String> = ControllerUtils.getBpmnXmlMono(file)

        return bpmnXmlMono.flatMap { bpmnXml ->
            Mono.fromCallable {
                val endpoint = customAnalysisEndpointRepository.getById(id)
                    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No custom analysis endpoint found for id $id")

                // RAG usage is a fixed property of the endpoint itself (like responseType),
                // not something re-picked per analysis run.
                when (endpoint.responseType) {
                    CustomAnalysisResponseType.BINARY -> analysisService.analyzeCustomBinary(
                        bpmnXml = bpmnXml,
                        promptText = endpoint.promptText,
                        llmPropsOverride = llmPropsOverrides,
                        useRag = endpoint.ragEnabled,
                        ragMode = endpoint.ragMode ?: RagMode.HYBRID,
                        activitiesOnly = activitiesOnly
                    )
                    CustomAnalysisResponseType.MULTICLASS -> analysisService.analyzeCustomMulticlass(
                        bpmnXml = bpmnXml,
                        promptText = endpoint.promptText,
                        llmPropsOverride = llmPropsOverrides,
                        useRag = endpoint.ragEnabled,
                        ragMode = endpoint.ragMode ?: RagMode.HYBRID,
                        activitiesOnly = activitiesOnly
                    )
                }
            }.subscribeOn(Schedulers.boundedElastic())
        }.map { ResponseEntity.ok(it) }
    }

    private fun parseRagMode(ragModePart: FormFieldPart?): RagMode {
        val raw = ragModePart?.value() ?: return RagMode.HYBRID
        return try {
            RagMode.fromString(raw)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        }
    }
}
