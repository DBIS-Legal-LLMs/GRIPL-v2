package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.model.dto.UserAnalysisSettings
import de.mertendieckmann.griplbackend.repository.UserAnalysisSettingsRepository
import de.mertendieckmann.griplbackend.security.authenticatedUserId
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange

@RestController
@RequestMapping("/analysis-settings")
class UserAnalysisSettingsController(
    private val repository: UserAnalysisSettingsRepository
) {

    @Operation(
        summary = "Get the caller's Process Analysis settings",
        description = "Returns the saved settings, or defaults with configured=false if the user has never saved any."
    )
    @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun get(exchange: ServerWebExchange): UserAnalysisSettings =
        repository.findByOwner(exchange.authenticatedUserId()) ?: UserAnalysisSettings()

    @Operation(summary = "Save the caller's Process Analysis settings")
    @PutMapping("", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun save(@RequestBody settings: UserAnalysisSettings, exchange: ServerWebExchange): UserAnalysisSettings {
        val userId = exchange.authenticatedUserId()
        repository.upsert(userId, settings)
        return repository.findByOwner(userId) ?: settings.copy(configured = true)
    }
}
