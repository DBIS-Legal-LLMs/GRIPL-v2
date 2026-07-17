package de.mertendieckmann.griplbackend.application.analyzer

import de.mertendieckmann.griplbackend.ai.PromptBpmnAnalysisAiServiceFactory
import de.mertendieckmann.griplbackend.ai.SharedChatMemoryProvider
import de.mertendieckmann.griplbackend.application.BpmnExtractor
import de.mertendieckmann.griplbackend.application.SafetyNet
import de.mertendieckmann.griplbackend.adapter.rag.RagApiClient
import de.mertendieckmann.griplbackend.model.BpmnElement
import de.mertendieckmann.griplbackend.model.dto.AnalysisResponse
import de.mertendieckmann.griplbackend.model.dto.RagDocument
import de.mertendieckmann.griplbackend.model.dto.RagElementContext
import de.mertendieckmann.griplbackend.model.dto.RagEntity
import de.mertendieckmann.griplbackend.model.dto.RagMode
import de.mertendieckmann.griplbackend.model.dto.RagRelationship
import dev.langchain4j.model.chat.ChatModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.*

class PromptBpmnAnalyzer(
    private val llm: ChatModel,
    private val ragApiClient: RagApiClient
) : BpmnAnalyzer {

    private val log = KotlinLogging.logger { }
    private val memoryProvider = SharedChatMemoryProvider(50)
    private val safetyNet = SafetyNet(llm, memoryProvider)

    override fun analyzeBpmnForGdpr(bpmnXml: String, useRag: Boolean, ragMode: RagMode, activitiesOnly: Boolean): AnalysisResponse {
        val sessionId = UUID.randomUUID().toString()

        val bpmnElements = BpmnExtractor().extractBpmnElements(bpmnXml)

        if (useRag) {
            // RAG-augmented path
            val bpmnAnalysisAiServiceWithRag = PromptBpmnAnalysisAiServiceFactory.create(llm, memoryProvider, activitiesOnly)
            val ragContextMap = fetchRagContext(bpmnElements, ragMode, activitiesOnly = activitiesOnly)

            val pool = buildDedupedPool(ragContextMap)

            log.debug {
                "Deduped RAG pool: ${pool.entityLines.size} entities | " +
                    "${pool.relationshipLines.size} relationships | ${pool.documentLines.size} documents"
            }

            val result = safetyNet.safeGuardAnalysisResultParsing(sessionId, maxRetries = 3) {
                val formattedPrompt = renderCombinedPrompt(bpmnElements, pool)
                bpmnAnalysisAiServiceWithRag.analyzeWithRagContext(sessionId, formattedPrompt)
            }

            val analysisResult = result.first.resolveActivities(bpmnElements)
            val amountOfRetries = result.second
            val ragContext = parseRagContextForResponse(ragContextMap, bpmnElements)

            log.info { "BPMN Analysis Result (with RAG): $analysisResult" }

            return AnalysisResponse.fromBpmnAnalysisResult(
                analysisResult, bpmnElements, amountOfRetries, ragContext, pool.flatten()
            )
        } else {
            // Original path — unchanged from evaluation baseline
            val bpmnAnalysisAiServiceNoRag = PromptBpmnAnalysisAiServiceFactory.createWithoutRag(llm, memoryProvider, activitiesOnly)
            val result = safetyNet.safeGuardAnalysisResultParsing(sessionId, maxRetries = 3) {
                bpmnAnalysisAiServiceNoRag.analyze(sessionId, bpmnElements)
            }

            val analysisResult = result.first.resolveActivities(bpmnElements)
            val amountOfRetries = result.second

            log.info { "BPMN Analysis Result: $analysisResult" }

            return AnalysisResponse.fromBpmnAnalysisResult(analysisResult, bpmnElements, amountOfRetries)
        }
    }

    // -------------------------------------------------------------------------
    // RAG context retrieval
    // -------------------------------------------------------------------------

    private fun fetchRagContext(
        bpmnElements: Set<BpmnElement>,
        ragMode: RagMode,
        maxConcurrency: Int = 8,
        activitiesOnly: Boolean = false
    ): Map<String, Map<String, Any>> {
        val semaphore = Semaphore(maxConcurrency)

        val result = kotlinx.coroutines.runBlocking {
            bpmnElements
                .map { element ->
                    async {
                        // Retrieve GDPR context for every classifiable element (activities,
                        // events, gateways, data objects/stores) — not just activities. Only
                        // textAnnotation is excluded, as it is never classified.
                        // In activitiesOnly mode, only activities are classified
                        if (element.type.equals("textAnnotation", ignoreCase = true)) return@async null
                        if (activitiesOnly && !element.isActivity) return@async null

                        val flowLabelText = (element.outgoingFlowLabels + element.incomingFlowLabels +
                                element.outgoingMessageFlowLabels + element.incomingMessageFlowLabels)
                            .map { it.label }
                            .filterNot { it.matches(Regex("""(?i)\s*(yes|no|ja|nein|ok|true|false)\s*""")) }
                            .distinct()
                            .joinToString(" - ")

                        val queryText = sequenceOf(
                            element.name, element.documentation, flowLabelText, element.poolName, element.laneName
                        )
                            .filterNotNull()
                            .filter { it.isNotBlank() }
                            .filter { !it.matches(Regex("""[\d/.\-]+""")) }
                            .joinToString(" - ")

                        if (queryText.isNotBlank()) {
                            try {
                                semaphore.withPermit {
                                    val response = ragApiClient.queryRag(queryText, ragMode)
                                    val parsed = response.response
                                    log.debug {
                                        val entities = parsed["entities"] as? List<*> ?: emptyList<Any>()
                                        val rels = parsed["relationships"] as? List<*> ?: emptyList<Any>()
                                        val docs = parsed["documents"] as? List<*> ?: emptyList<Any>()
                                        "RAG for ${element.id} (${element.name}): " +
                                            "${entities.size} entities / ${rels.size} relationships / ${docs.size} documents"
                                    }
                                    element.id to parsed
                                }
                            } catch (e: Exception) {
                                log.error(e) { "RAG query failed for element ${element.id}" }
                                null
                            }
                        } else null
                    }
                }
                .awaitAll()
                .filterNotNull()
                .toMap()
        }

        log.info { "RAG context retrieved for ${result.size} / ${bpmnElements.size} elements" }
        return result
    }

    // -------------------------------------------------------------------------
    // LLM prompt builder
    // -------------------------------------------------------------------------

    private data class DedupedPool(
        val entityLines: List<String>,
        val relationshipLines: List<String>,
        val documentLines: List<String>
    ) {
        fun flatten(): List<String> = entityLines + relationshipLines + documentLines
    }


    private fun normalizeKey(value: String): String = value.trim().lowercase()

    /**
     * Builds the deduplicated+capped pool fed into the analyzer prompt. The same lines
     * are later reused as `retrieved_contexts` for Ragas so Faithfulness measures grounding
     * against exactly what the LLM saw.
     */
    private fun buildDedupedPool(ragContextMap: Map<String, Map<String, Any>>): DedupedPool {
        val bestEntityRank = mutableMapOf<String, Int>()
        val entityByLabel = mutableMapOf<String, Map<String, Any>>()
        val bestRelRank = mutableMapOf<String, Int>()
        val relByKey = mutableMapOf<String, Map<String, Any>>()
        val bestDocRank = mutableMapOf<String, Int>()
        val docById = mutableMapOf<String, Map<String, Any>>()

        ragContextMap.values.forEach { ctx ->
            @Suppress("UNCHECKED_CAST")
            (ctx["entities"] as? List<Map<String, Any>> ?: emptyList()).forEachIndexed { rank, e ->
                val label = (e["label"] as? String) ?: return@forEachIndexed
                val key = normalizeKey(label)
                val prev = bestEntityRank[key]
                if (prev == null || rank < prev) {
                    bestEntityRank[key] = rank
                    entityByLabel[key] = e
                }
            }

            @Suppress("UNCHECKED_CAST")
            (ctx["relationships"] as? List<Map<String, Any>> ?: emptyList()).forEachIndexed { rank, r ->
                val key = normalizeKey("${r["source_label"]}|${r["label"]}|${r["target_label"]}")
                val prev = bestRelRank[key]
                if (prev == null || rank < prev) {
                    bestRelRank[key] = rank
                    relByKey[key] = r
                }
            }

            @Suppress("UNCHECKED_CAST")
            (ctx["documents"] as? List<Map<String, Any>> ?: emptyList()).forEachIndexed { rank, d ->
                val docId = (d["source_document"] as? String) ?: (d["preview"] as? String) ?: return@forEachIndexed
                val prev = bestDocRank[docId]
                if (prev == null || rank < prev) {
                    bestDocRank[docId] = rank
                    docById[docId] = d
                }
            }
        }

        val allEntities = bestEntityRank.entries.sortedBy { it.value }.mapNotNull { entityByLabel[it.key] }
        val allRels = bestRelRank.entries.sortedBy { it.value }.mapNotNull { relByKey[it.key] }
        val allDocs = bestDocRank.entries.sortedBy { it.value }.mapNotNull { docById[it.key] }

        val entityLimit = (allEntities.size / 2).coerceIn(5, 10)
        val relLimit    = (allRels.size / 2).coerceIn(15, 30)
        val docLimit    = (allDocs.size / 2).coerceIn(2, 3)

        val entityLines = allEntities.take(entityLimit).map { e ->
            val label = e["label"] ?: ""
            val type  = e["type"]  ?: ""
            val desc  = (e["description_text"] as? String)?.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""
            "[$type] $label$desc"
        }

        val relLines = allRels.take(relLimit).map { r ->
            val src   = r["source_label"] ?: ""
            val tgt   = r["target_label"] ?: ""
            val label = (r["label"] as? String)?.takeIf { it.isNotBlank() } ?: "relates to"
            // Sentence form (vs. "src → tgt (label)") so the LLM can paraphrase it
            // in reasoning and the Ragas judge can match prose claims against it.
            "Under GDPR, $src $label $tgt."
        }

        val docLines = allDocs.take(docLimit).map { d ->
            val preview = d["content"] ?: d["preview"] ?: ""
            val sourceDoc = d["source_document"] as? String
            if (sourceDoc != null) "[Source: $sourceDoc] \"$preview\"" else "\"$preview\""
        }

        return DedupedPool(entityLines, relLines, docLines)
    }

    /**
     * Renders the prompt the LLM receives: section headers around the deduped pool,
     * followed by the BPMN process elements.
     */
    private fun renderCombinedPrompt(
        bpmnElements: Set<BpmnElement>,
        pool: DedupedPool
    ): String = buildString {
        appendLine("=== RETRIEVED GDPR KNOWLEDGE ===")
        appendLine(
            "The following knowledge was retrieved from a GDPR legal knowledge graph as " +
            "process-wide background. It was gathered across ALL elements of the process and " +
            "is NOT scoped to any single element. Treat it only as reference material that may " +
            "help you reason about elements that genuinely process personal data.\n" +
            "Important: the mere presence of GDPR text below is NOT evidence that any given " +
            "element is critical. Do not assume an element is critical just because related legal " +
            "context was retrieved. Judge each element on whether it itself processes personal " +
            "data."
        )
        appendLine()

        if (pool.entityLines.isNotEmpty()) {
            appendLine("Relevant GDPR Entities:")
            pool.entityLines.forEach { appendLine("  • $it") }
            appendLine()
        }

        if (pool.relationshipLines.isNotEmpty()) {
            appendLine("Key Relationships:")
            pool.relationshipLines.forEach { appendLine("  • $it") }
            appendLine()
        }

        if (pool.documentLines.isNotEmpty()) {
            appendLine("Supporting Legal Excerpts:")
            pool.documentLines.forEach { appendLine("  $it") }
            appendLine()
        }

        if (pool.entityLines.isEmpty() && pool.relationshipLines.isEmpty() && pool.documentLines.isEmpty()) {
            appendLine("  (No GDPR context retrieved.)")
            appendLine()
        }

        appendLine("=== BPMN PROCESS ELEMENTS ===")
        append(bpmnElements.toString())
    }

    // -------------------------------------------------------------------------
    // RAG context → frontend DTO
    // -------------------------------------------------------------------------

    /**
     * Maps the parsed context map into typed [RagElementContext] objects
     * for inclusion in the API response to the frontend.
     */
    private fun parseRagContextForResponse(
        ragContextMap: Map<String, Map<String, Any>>,
        bpmnElements: Set<BpmnElement>
    ): Map<String, RagElementContext> {
        val elementById = bpmnElements.associateBy { it.id }

        return ragContextMap.mapValues { (elementId, ctx) ->
            @Suppress("UNCHECKED_CAST")
            val entities = (ctx["entities"] as? List<Map<String, Any>> ?: emptyList()).map { e ->
                RagEntity(
                    label = (e["label"] as? String) ?: "",
                    type = (e["type"] as? String) ?: "Unknown",
                    description = (e["description_text"] as? String) ?: ""
                )
            }

            @Suppress("UNCHECKED_CAST")
            val relationships = (ctx["relationships"] as? List<Map<String, Any>> ?: emptyList()).map { r ->
                RagRelationship(
                    source = (r["source_label"] as? String) ?: "",
                    target = (r["target_label"] as? String) ?: "",
                    label = (r["label"] as? String) ?: "relates to"
                )
            }

            @Suppress("UNCHECKED_CAST")
            val documents = (ctx["documents"] as? List<Map<String, Any>> ?: emptyList()).map { d ->
                RagDocument(
                    content = (d["content"] as? String) ?: "",
                    preview = (d["preview"] as? String) ?: (d["content"] as? String) ?: "",
                    sourceDocument = (d["source_document"] as? String)
                )
            }

            RagElementContext(
                activityName = elementById[elementId]?.name,
                entities = entities,
                relationships = relationships,
                documents = documents
            )
        }
    }
}