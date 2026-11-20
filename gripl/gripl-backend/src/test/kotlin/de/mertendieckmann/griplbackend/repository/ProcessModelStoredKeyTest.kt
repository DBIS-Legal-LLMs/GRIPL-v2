package de.mertendieckmann.griplbackend.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.transaction.annotation.Transactional

/** GRIPL#62: keys stored by older versions are removed by V20, and queued/running rows are reset on restart. */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:postgresql://localhost:5432/gripl_labels_test",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres"
    ]
)
@Transactional
@Rollback
class ProcessModelStoredKeyTest(
    @Autowired private val repository: ProcessModelRepository,
    @Autowired private val jdbc: JdbcTemplate
) {

    private fun insertModel(status: String, options: String?): Long = jdbc.queryForObject(
        "insert into process_model (name, bpmn_xml, owner_user_id, status, analysis_options) values ('m', '<bpmn/>', 'u', ?, ?::jsonb) returning id",
        Long::class.java, status, options
    )!!

    private fun optionsOf(id: Long): String? =
        jdbc.queryForObject("select analysis_options::text from process_model where id = ?", String::class.java, id)

    @Test
    fun `the migration removes stored api keys and keeps the other options`() {
        val withKey = insertModel("DONE", """{"llmProps":{"modelName":"some/model","apiKey":"sk-or-v1-old"},"useRag":true}""")
        val withoutKey = insertModel("DONE", """{"llmProps":{"modelName":"some/model"},"useRag":false}""")
        val noOptions = insertModel("PENDING", null)

        jdbc.execute(ClassPathResource("db/migration/V20__remove_stored_openrouter_keys.sql").getContentAsString(Charsets.UTF_8))

        val cleaned = optionsOf(withKey)!!
        assertEquals(false, cleaned.contains("sk-or-v1-old"), cleaned)
        assertEquals(true, cleaned.contains("some/model") && cleaned.contains("useRag"), cleaned)
        assertEquals(true, optionsOf(withoutKey)!!.contains("some/model"))
        assertNull(optionsOf(noOptions))
    }

    @Test
    fun `a restart sets queued and running models to error and leaves the others alone`() {
        val queued = insertModel("QUEUED", """{"useRag":false}""")
        val running = insertModel("RUNNING", null)
        val done = insertModel("DONE", null)

        assertEquals(true, repository.resetInterruptedToError("again") >= 2)

        fun status(id: Long) = jdbc.queryForObject("select status from process_model where id = ?", String::class.java, id)
        assertEquals("ERROR", status(queued))
        assertEquals("ERROR", status(running))
        assertEquals("DONE", status(done))
    }
}
