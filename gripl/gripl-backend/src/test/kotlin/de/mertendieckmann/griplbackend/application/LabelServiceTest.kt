package de.mertendieckmann.griplbackend.application

import de.mertendieckmann.griplbackend.model.dto.CreateLabelRequest
import de.mertendieckmann.griplbackend.model.dto.Label
import de.mertendieckmann.griplbackend.model.dto.UpdateLabelRequest
import de.mertendieckmann.griplbackend.repository.LabelRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/** Tests label validation and archival rules without a database. */
class LabelServiceTest {

    private val repository = mock<LabelRepository>()
    private val service = LabelService(repository)

    private fun label(
        predefined: Boolean = false,
        deletedAt: String? = null
    ) = Label(
        id = 8L,
        name = "Contract review",
        description = "Reviews personal data in contracts",
        predefined = predefined,
        createdBy = "user-1",
        createdAt = "2026-10-28T12:00:00Z",
        updatedBy = "user-1",
        updatedAt = "2026-10-28T12:00:00Z",
        deletedBy = if (deletedAt != null) "admin-1" else null,
        deletedAt = deletedAt
    )

    @Test
    fun `create trims input and passes the authenticated user to the repository`() {
        val stored = label()
        whenever(
            repository.create(
                stored.name,
                stored.description,
                "user-1"
            )
        ).thenReturn(stored)

        val result = service.create(
            CreateLabelRequest(
                name = "  Contract review  ",
                description = "  Reviews personal data in contracts  "
            ),
            "user-1"
        )

        assertEquals(stored, result)
        verify(repository).create(
            stored.name,
            stored.description,
            "user-1"
        )
    }

    @Test
    fun `create rejects a blank name`() {
        val exception = assertThrows(ResponseStatusException::class.java) {
            service.create(
                CreateLabelRequest(" \t\n ", "Description"),
                "user-1"
            )
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        verifyNoInteractions(repository)
    }

    @Test
    fun `create rejects a blank description`() {
        val exception = assertThrows(ResponseStatusException::class.java) {
            service.create(
                CreateLabelRequest("Contract review", "   "),
                "user-1"
            )
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        verifyNoInteractions(repository)
    }

    @Test
    fun `create returns conflict when the database rejects a duplicate name`() {
        whenever(
            repository.create("Contract review", "Description", "user-1")
        ).thenThrow(DuplicateKeyException("Duplicate active label name"))

        val exception = assertThrows(ResponseStatusException::class.java) {
            service.create(
                CreateLabelRequest("Contract review", "Description"),
                "user-1"
            )
        }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        assertEquals(
            "An active label with this name already exists.",
            exception.reason
        )
    }

    @Test
    fun `update trims input and records the editing user`() {
        val updated = label().copy(
            name = "Updated name",
            description = "Updated description",
            updatedBy = "admin-1"
        )

        whenever(repository.findById(8L)).thenReturn(label())

        whenever(
            repository.update(
                8L,
                "Updated name",
                "Updated description",
                "admin-1"
            )
        ).thenReturn(updated)

        val result = service.update(
            8L,
            UpdateLabelRequest("  Updated name  ", "  Updated description  "),
            "admin-1"
        )

        assertEquals(updated, result)
        verify(repository).update(
            8L,
            "Updated name",
            "Updated description",
            "admin-1"
        )
    }

    @Test
    fun `update rejects a blank name or description`() {
        val invalidRequests = listOf(
            UpdateLabelRequest("   ", "Description"),
            UpdateLabelRequest("Name", "   ")
        )

        invalidRequests.forEach { request ->
            val exception = assertThrows(ResponseStatusException::class.java) {
                service.update(8L, request, "admin-1")
            }

            assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        }

        verifyNoInteractions(repository)
    }

    @Test
    fun `update returns not found when no active label matches`() {
        whenever(repository.findById(8L)).thenReturn(label())

        whenever(
            repository.update(8L, "Name", "Description", "admin-1")
        ).thenReturn(null)

        val exception = assertThrows(ResponseStatusException::class.java) {
            service.update(
                8L,
                UpdateLabelRequest("Name", "Description"),
                "admin-1"
            )
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
    }

    @Test
    fun `update returns conflict when the new name is already used`() {
        whenever(repository.findById(8L)).thenReturn(label())

        whenever(
            repository.update(8L, "Name", "Description", "admin-1")
        ).thenThrow(DuplicateKeyException("Duplicate active label name"))

        val exception = assertThrows(ResponseStatusException::class.java) {
            service.update(
                8L,
                UpdateLabelRequest("Name", "Description"),
                "admin-1"
            )
        }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
    }

        @Test
    fun `predefined label names cannot be changed`() {
        whenever(repository.findById(8L))
            .thenReturn(label(predefined = true))

        val exception = assertThrows(ResponseStatusException::class.java) {
            service.update(
                8L,
                UpdateLabelRequest(
                    name = "Renamed collection",
                    description = "Updated description"
                ),
                "admin-1"
            )
        }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        assertEquals(
            "Predefined label names cannot be changed.",
            exception.reason
        )
        verify(repository).findById(8L)
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun `predefined label descriptions can be changed`() {
        val predefined = label(predefined = true)
        val updated = predefined.copy(
            description = "Updated description",
            updatedBy = "admin-1"
        )

        whenever(repository.findById(8L)).thenReturn(predefined)
        whenever(
            repository.update(
                8L,
                predefined.name,
                "Updated description",
                "admin-1"
            )
        ).thenReturn(updated)

        val result = service.update(
            8L,
            UpdateLabelRequest(
                name = predefined.name,
                description = "Updated description"
            ),
            "admin-1"
        )

        assertEquals(updated, result)
        verify(repository).update(
            8L,
            predefined.name,
            "Updated description",
            "admin-1"
        )
    }

    @Test
    fun `predefined labels cannot be archived`() {
        whenever(repository.findById(8L))
            .thenReturn(label(predefined = true))

        val exception = assertThrows(ResponseStatusException::class.java) {
            service.softDelete(8L, "admin-1")
        }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        verify(repository).findById(8L)
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun `active custom labels are archived with the acting user`() {
        whenever(repository.findById(8L)).thenReturn(label())
        whenever(repository.softDelete(8L, "admin-1")).thenReturn(true)

        service.softDelete(8L, "admin-1")

        verify(repository).softDelete(8L, "admin-1")
    }

    @Test
    fun `archiving an already archived label does not overwrite its audit fields`() {
        whenever(repository.findById(8L))
            .thenReturn(label(deletedAt = "2026-10-28T13:00:00Z"))

        service.softDelete(8L, "admin-2")

        verify(repository).findById(8L)
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun `archiving an unknown label returns not found`() {
        whenever(repository.findById(99L)).thenReturn(null)

        val exception = assertThrows(ResponseStatusException::class.java) {
            service.softDelete(99L, "admin-1")
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        verify(repository).findById(99L)
        verifyNoMoreInteractions(repository)
    }
}