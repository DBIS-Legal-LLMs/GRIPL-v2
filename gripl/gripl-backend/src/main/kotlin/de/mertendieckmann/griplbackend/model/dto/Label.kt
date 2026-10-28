package de.mertendieckmann.griplbackend.model.dto
/** A stored global label corresponding to SQL table */
data class Label(
    val id: Long,
    val name: String,
    val description: String,
    val predefined: Boolean,
    val createdBy: String?,
    val createdAt: String,
    val updatedBy: String?,
    val updatedAt: String,
    val deletedBy: String?,
    val deletedAt: String?
)
/** Input for creating a label, the backend sets all system-managed fields. */
data class CreateLabelRequest(
    val name: String,
    val description: String
)
/** Input for updating a label's name and description. */
data class UpdateLabelRequest(
    val name: String,
    val description: String
)