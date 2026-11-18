package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.application.LabelService
import de.mertendieckmann.griplbackend.model.dto.CreateLabelRequest
import de.mertendieckmann.griplbackend.model.dto.Label
import de.mertendieckmann.griplbackend.model.dto.UpdateLabelRequest
import de.mertendieckmann.griplbackend.security.authenticatedUserId
import de.mertendieckmann.griplbackend.security.requireGriplRole
import de.mertendieckmann.griplbackend.security.requirePrivilegedGriplRole
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.net.URI

@RestController
@RequestMapping("/labels")
class LabelController(
    private val labelService: LabelService
) {

    /** Lists active labels for researchers and admins. */
    @GetMapping
    fun getLabels(exchange: ServerWebExchange): Mono<List<Label>> {
        exchange.authenticatedUserId()
        exchange.requirePrivilegedGriplRole() //only for researchers and admins

        return Mono.fromCallable {
            labelService.findAllActive() 
        }.subscribeOn(Schedulers.boundedElastic())
    }

    /** Creates a custom label for the authenticated researcher or admin. */
    @PostMapping
    fun createLabel(
        @RequestBody request: CreateLabelRequest,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Label>> {
        val userId = exchange.authenticatedUserId()
        exchange.requirePrivilegedGriplRole()

        return Mono.fromCallable {
            val label = labelService.create(request, userId)

            ResponseEntity
                .created(URI.create("${exchange.request.path.value()}/${label.id}"))
                .body(label)
        }.subscribeOn(Schedulers.boundedElastic())
    }

    /** Updates a label's name and description, only admins are allowed. */
    @PatchMapping("/{id}")
    fun updateLabel(
        @PathVariable("id") id: Long,
        @RequestBody request: UpdateLabelRequest,
        exchange: ServerWebExchange
    ): Mono<Label> {
        val userId = exchange.authenticatedUserId()
        exchange.requireGriplRole(setOf("admin")) //only admins are allowed to patch

        return Mono.fromCallable {
            labelService.update(id, request, userId)
        }.subscribeOn(Schedulers.boundedElastic())
    }

    /** Permanently archives a custom label without deleting its database row. */
    @DeleteMapping("/{id}")
    fun deleteLabel(
        @PathVariable("id") id: Long,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        val userId = exchange.authenticatedUserId()
        exchange.requireGriplRole(setOf("admin"))

        return Mono.fromCallable {
            labelService.softDelete(id, userId)
            ResponseEntity.noContent().build<Void>()
        }.subscribeOn(Schedulers.boundedElastic())
    }
}