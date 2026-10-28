package de.mertendieckmann.griplbackend.adapter.web

import de.mertendieckmann.griplbackend.application.LabelService
import de.mertendieckmann.griplbackend.model.dto.CreateLabelRequest
import de.mertendieckmann.griplbackend.model.dto.Label
import de.mertendieckmann.griplbackend.model.dto.UpdateLabelRequest
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE
import de.mertendieckmann.griplbackend.security.AUTHENTICATED_USER_ID_ATTRIBUTE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ResponseStatusException
import java.time.Duration

/** Tests role restrictions and authenticated user forwarding. */
class LabelControllerTest {

    private val service = mock<LabelService>()
    private val controller = LabelController(service)
    private val timeout = Duration.ofSeconds(5)

    private val createRequest =
        CreateLabelRequest("Contract review", "Reviews personal data")

    private val updateRequest =
        UpdateLabelRequest("Updated name", "Updated description")

    private val storedLabel = Label(
        id = 8L,
        name = "Contract review",
        description = "Reviews personal data",
        predefined = false,
        createdBy = "user-1",
        createdAt = "2026-10-28T12:00:00Z",
        updatedBy = "user-1",
        updatedAt = "2026-10-28T12:00:00Z",
        deletedBy = null,
        deletedAt = null
    )

    /** Simulates identity attributes normally set by the JWT filter. */
    private fun exchangeAs(
        role: String?,
        userId: String? = "user-1"
    ): MockServerWebExchange {
        return MockServerWebExchange.from(
            MockServerHttpRequest.get("/labels")
        ).apply {
            if (userId != null) {
                attributes[AUTHENTICATED_USER_ID_ATTRIBUTE] = userId
            }
            if (role != null) {
                attributes[AUTHENTICATED_GRIPL_ROLE_ATTRIBUTE] = role
            }
        }
    }

    @Test
    fun `researchers and admins can list active labels`() {
        whenever(service.findAllActive()).thenReturn(listOf(storedLabel))

        listOf("researcher", "admin").forEach { role ->
            val result = controller.getLabels(exchangeAs(role)).block(timeout)

            assertEquals(listOf(storedLabel), result)
        }
    }

    @Test
    fun `researchers and admins can create labels`() {
        whenever(service.create(createRequest, "user-1"))
            .thenReturn(storedLabel)

        listOf("researcher", "admin").forEach { role ->
            val response = controller.createLabel(
                createRequest,
                exchangeAs(role)
            ).block(timeout)!!

            assertEquals(HttpStatus.CREATED, response.statusCode)
            assertEquals(storedLabel, response.body)
            assertEquals("/labels/8", response.headers.location.toString())
        }
    }

    @Test
    fun `unprivileged or missing roles cannot list or create labels`() {
        listOf("user", "end-user", "dpo", "unknown", null).forEach { role ->
            val readError = assertThrows(ResponseStatusException::class.java) {
                controller.getLabels(exchangeAs(role)).block(timeout)
            }
            assertEquals(HttpStatus.FORBIDDEN, readError.statusCode)

            val createError = assertThrows(ResponseStatusException::class.java) {
                controller.createLabel(
                    createRequest,
                    exchangeAs(role)
                ).block(timeout)
            }
            assertEquals(HttpStatus.FORBIDDEN, createError.statusCode)
        }

        verifyNoInteractions(service)
    }

    @Test
    fun `admin can update a label and the authenticated user is forwarded`() {
        val updated = storedLabel.copy(
            name = updateRequest.name,
            description = updateRequest.description,
            updatedBy = "admin-1"
        )
        whenever(service.update(8L, updateRequest, "admin-1"))
            .thenReturn(updated)

        val result = controller.updateLabel(
            8L,
            updateRequest,
            exchangeAs("admin", "admin-1")
        ).block(timeout)

        assertEquals(updated, result)
        verify(service).update(8L, updateRequest, "admin-1")
    }

    @Test
    fun `admin can archive a label and receives no content`() {
        val response = controller.deleteLabel(
            8L,
            exchangeAs("admin", "admin-1")
        ).block(timeout)!!

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        verify(service).softDelete(8L, "admin-1")
    }

    @Test
    fun `non-admin roles cannot update or archive labels`() {
        listOf(
            "researcher", "user", "end-user", "dpo", "unknown", null
        ).forEach { role ->
            val updateError = assertThrows(ResponseStatusException::class.java) {
                controller.updateLabel(
                    8L,
                    updateRequest,
                    exchangeAs(role)
                ).block(timeout)
            }
            assertEquals(HttpStatus.FORBIDDEN, updateError.statusCode)

            val deleteError = assertThrows(ResponseStatusException::class.java) {
                controller.deleteLabel(
                    8L,
                    exchangeAs(role)
                ).block(timeout)
            }
            assertEquals(HttpStatus.FORBIDDEN, deleteError.statusCode)
        }

        verifyNoInteractions(service)
    }

    @Test
    fun `missing user identity is rejected by every endpoint`() {
        val exchange = exchangeAs("admin", userId = null)

        val actions: List<() -> Unit> = listOf(
            { controller.getLabels(exchange).block(timeout); Unit },
            {
                controller.createLabel(createRequest, exchange).block(timeout)
                Unit
            },
            {
                controller.updateLabel(8L, updateRequest, exchange).block(timeout)
                Unit
            },
            { controller.deleteLabel(8L, exchange).block(timeout); Unit }
        )

        actions.forEach { action ->
            val exception = assertThrows(ResponseStatusException::class.java) {
                action()
            }
            assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
        }

        verifyNoInteractions(service)
    }
}