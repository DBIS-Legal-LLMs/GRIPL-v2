package de.mertendieckmann.griplbackend.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.transaction.annotation.Transactional

/**
 * Tests the labels Flyway migration and repository queries against the separate
 * gripl_labels_test database.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:postgresql://localhost:5432/gripl_labels_test",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres"
    ]
)
@Transactional
@Rollback
class LabelRepositoryTest(
    @Autowired private val repository: LabelRepository,
    @Autowired private val jdbc: JdbcTemplate
) {

    @Test
    fun `Flyway creates the seven predefined GDPR labels`() {
        val labels = repository.findAllPredefined()

        assertEquals(
            setOf(
                "COLLECTION",
                "STORAGE",
                "USAGE",
                "TRANSFERAL",
                "MODIFICATION",
                "DELETION",
                "ACCESS"
            ),
            labels.map { it.name }.toSet()
        )

        assertTrue(labels.all { it.predefined })
        assertTrue(labels.all { it.deletedAt == null })
    }

    @Test
    fun `creates and retrieves an active custom label`() {
        val created = repository.create(
            name = "Contract review",
            description = "Reviews personal data in contracts",
            userId = "researcher-1"
        )

        assertFalse(created.predefined)
        assertEquals("researcher-1", created.createdBy)
        assertEquals("researcher-1", created.updatedBy)
        assertNotNull(created.createdAt)
        assertNotNull(created.updatedAt)

        val activeLabels = repository.findAllActive()

        assertTrue(activeLabels.any { it.id == created.id })
        assertEquals(created, repository.findById(created.id))
    }

    @Test
    fun `active label names are unique ignoring case and outer whitespace`() {
        repository.create(
            name = "Contract review",
            description = "First description",
            userId = "researcher-1"
        )

        assertThrows(DuplicateKeyException::class.java) {
            repository.create(
                name = "  contract REVIEW  ",
                description = "Second description",
                userId = "researcher-2"
            )
        }
    }

    @Test
    fun `updates an active label and records the editing user`() {
        val created = repository.create(
            name = "Contract review",
            description = "Initial description",
            userId = "researcher-1"
        )

        val updated = repository.update(
            id = created.id,
            name = "Updated contract review",
            description = "Updated description",
            userId = "admin-1"
        )

        assertNotNull(updated)
        assertEquals("Updated contract review", updated!!.name)
        assertEquals("Updated description", updated.description)
        assertEquals("admin-1", updated.updatedBy)
        assertEquals(created.createdBy, updated.createdBy)
    }

    @Test
    fun `soft-deleted custom labels remain readable but are not active`() {
        val created = repository.create(
            name = "Contract review",
            description = "Reviews personal data in contracts",
            userId = "researcher-1"
        )

        assertTrue(repository.softDelete(created.id, "admin-1"))

        val archived = repository.findById(created.id)
        assertNotNull(archived)
        assertEquals("admin-1", archived!!.deletedBy)
        assertNotNull(archived.deletedAt)

        assertFalse(repository.findAllActive().any { it.id == created.id })
        assertNull(
            repository.update(
                id = created.id,
                name = "Changed after deletion",
                description = "This must not be saved",
                userId = "admin-1"
            )
        )
    }

    @Test
    fun `database query does not archive predefined labels`() {
        val predefined = repository.findAllPredefined().first()

        assertFalse(repository.softDelete(predefined.id, "admin-1"))

        val unchanged = repository.findById(predefined.id)
        assertNotNull(unchanged)
        assertNull(unchanged!!.deletedAt)
    }

    @Test
    fun `migration rejects blank label names and descriptions`() {
        assertThrows(Exception::class.java) {
            jdbc.update(
                """
                INSERT INTO labels (name, description, predefined)
                VALUES ('   ', 'Valid description', false)
                """.trimIndent()
            )
        }

        assertThrows(Exception::class.java) {
            jdbc.update(
                """
                INSERT INTO labels (name, description, predefined)
                VALUES ('Valid name', '   ', false)
                """.trimIndent()
            )
        }
    }
}