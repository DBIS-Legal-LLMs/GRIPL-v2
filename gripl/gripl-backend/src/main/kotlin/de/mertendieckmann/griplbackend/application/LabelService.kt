package de.mertendieckmann.griplbackend.application

import de.mertendieckmann.griplbackend.model.dto.CreateLabelRequest
import de.mertendieckmann.griplbackend.model.dto.Label
import de.mertendieckmann.griplbackend.model.dto.UpdateLabelRequest
import de.mertendieckmann.griplbackend.repository.LabelRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

private const val DEFAULT_LABEL_DESCRIPTION = "No description provided."

/** Validates label changes and coordinates database access. */
@Service
class LabelService(
    private val labelRepository: LabelRepository
) {

    /** Returns labels available for new assignments. */
    fun findAllActive(): List<Label> =
        labelRepository.findAllActive()

    /** Creates a custom label with a non-blank name and an optional description. */
    fun create(request: CreateLabelRequest, userId: String): Label {
        val name = request.name.trim()
        val description = request.description.trim().ifBlank { DEFAULT_LABEL_DESCRIPTION }
        val color = request.color?.trim()?.ifBlank { null }
        validateName(name)

        return handleDuplicateName {
            labelRepository.create(name, description, color, userId)
        }
    }

    /** Updates an active label, predefined label names remain unchanged. */
    fun update(
        id: Long,
        request: UpdateLabelRequest,
        userId: String
    ): Label {
        val name = request.name.trim()
        val requestedDescription = request.description.trim()
        val color = request.color?.trim()?.ifBlank { null }
        validateName(name)

        val existingLabel = labelRepository.findById(id)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Label not found."
            )

        if (existingLabel.deletedAt != null) {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Active label not found."
            )
        }

        if (existingLabel.predefined && requestedDescription.isBlank()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Label description must not be blank."
            )
        }

        val description = requestedDescription.ifBlank { DEFAULT_LABEL_DESCRIPTION }

        if (existingLabel.predefined && name != existingLabel.name) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Predefined label names cannot be changed."
            )
        }

        return handleDuplicateName {
            labelRepository.update(id, name, description, color, userId)
                ?: throw ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Active label not found."
                )
        }
    }

    /** Soft-deletes a custom label, repeated deletion is harmless. */
    fun softDelete(id: Long, userId: String) {
        val label = labelRepository.findById(id)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Label not found."
            )

        if (label.predefined) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Predefined labels cannot be deleted."
            )
        }

        if (label.deletedAt != null) {
            return
        }

        labelRepository.softDelete(id, userId)
    }

    /** Rejects an empty or whitespace-only name. */
    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Label name must not be blank."
            )
        }
    }

    /**
    * Handles duplicate active label names rejected by the database's unique index
     * and returns HTTP 409 Conflict with a descriptive error message.
     */
    private fun handleDuplicateName(action: () -> Label): Label {
        return try {
            action()
        } catch (exception: DuplicateKeyException) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "An active label with this name already exists.",
                exception
            )
        }
    }
}