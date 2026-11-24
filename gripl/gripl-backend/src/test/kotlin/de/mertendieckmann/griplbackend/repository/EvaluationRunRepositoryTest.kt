package de.mertendieckmann.griplbackend.repository

import de.mertendieckmann.griplbackend.model.dto.EvaluationRunStatus
import de.mertendieckmann.griplbackend.model.dto.RunModelKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.junit.jupiter.api.AfterEach
import java.util.UUID

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:postgresql://localhost:5432/gripl_labels_test",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres"
    ]
)
// No @Transactional rollback: Postgres aborts a transaction after a unique violation, and these tests
// check exactly that. Every test uses its own users and deletes its runs afterwards.
class EvaluationRunRepositoryTest(
    @Autowired private val repository: EvaluationRunRepository,
    @Autowired private val jdbc: JdbcTemplate
) {
    private val prefix = "test-${UUID.randomUUID()}-"
    private fun user(n: Int) = "$prefix$n"

    @AfterEach
    fun cleanUp() {
        jdbc.update("delete from evaluation_run where owner_user_id like ?", "$prefix%")
    }

    private val request = """{"models":[{"label":"a"}],"seed":1}"""
    private val metrics = """{"truePositives":1,"falsePositives":0,"falseNegatives":0,"trueNegatives":2,"isSuccessful":true}"""


    private fun testCase(runId: Long, runNumber: Int, label: String, testCaseId: Long) =
        repository.insertEvent(runId, runNumber, label, "testCase", testCaseId, """{"type":"testCase","testCaseId":$testCaseId}""", metrics)

    private fun error(runId: Long, runNumber: Int, label: String, testCaseId: Long) =
        repository.insertEvent(runId, runNumber, label, "error", testCaseId, """{"type":"error","testCaseId":$testCaseId}""", null)

    private fun summary(runId: Long, runNumber: Int, label: String) =
        repository.insertEvent(runId, runNumber, label, "summary", null, """{"type":"summary"}""", null)

    @Test
    fun `a user can only have one active run, other users are not affected`() {
        val first = repository.insertRun(user(1), request)
        assertThrows(DuplicateKeyException::class.java) { repository.insertRun(user(1), request) }
        repository.insertRun(user(2), request)

        repository.markRunning(first)
        repository.markDone(first)
        repository.insertRun(user(1), request)
    }

    @Test
    fun `resuming a stopped run fails while another run of the user is active`() {
        val stopped = repository.insertRun(user(1), request)
        repository.markCancelled(stopped)
        repository.insertRun(user(1), request)

        assertThrows(DuplicateKeyException::class.java) { repository.markQueuedForResume(stopped) }
    }

    @Test
    fun `only stopped runs can be resumed`() {
        val done = repository.insertRun(user(1), request)
        repository.markDone(done)
        assertFalse(repository.markQueuedForResume(done))

        val interrupted = repository.insertRun(user(1), request)
        repository.markInterrupted(interrupted, "restart")
        assertTrue(repository.markQueuedForResume(interrupted))
        val row = repository.getById(interrupted)!!
        assertEquals(EvaluationRunStatus.QUEUED, row.status)
        assertNull(row.errorMessage)
    }

    @Test
    fun `a finished status does not overwrite a newer one`() {
        val id = repository.insertRun(user(1), request)
        repository.markCancelled(id)
        repository.markDone(id)
        assertEquals(EvaluationRunStatus.CANCELLED, repository.getById(id)!!.status)
    }

    @Test
    fun `a restart sets active runs to interrupted and leaves the others alone`() {
        val queued = repository.insertRun(user(1), request)
        val running = repository.insertRun(user(2), request)
        repository.markRunning(running)
        val done = repository.insertRun(user(3), request)
        repository.markDone(done)

        assertTrue(repository.resetActiveToInterrupted("restart") >= 2)

        assertEquals(EvaluationRunStatus.INTERRUPTED, repository.getById(queued)!!.status)
        assertEquals(EvaluationRunStatus.INTERRUPTED, repository.getById(running)!!.status)
        assertEquals("restart", repository.getById(running)!!.errorMessage)
        assertEquals(EvaluationRunStatus.DONE, repository.getById(done)!!.status)
    }

    @Test
    fun `preparing a resume removes errors and their summaries and reports what is finished`() {
        val id = repository.insertRun(user(1), request)
        // Repetition 1, model a: finished without errors
        testCase(id, 1, "a", 1); testCase(id, 1, "a", 2); summary(id, 1, "a")
        // Repetition 1, model b: finished, but one test case failed
        testCase(id, 1, "b", 1); error(id, 1, "b", 2); summary(id, 1, "b")
        // Repetition 2, model a: stopped half-way
        testCase(id, 2, "a", 1)
        repository.markCancelled(id)

        val state = repository.prepareResume(id)

        assertEquals(true, state.getValue(RunModelKey(1, "a")).finished)
        val b = state.getValue(RunModelKey(1, "b"))
        assertEquals(false, b.finished)
        assertEquals(setOf(1L), b.completedTestCaseIds)
        assertEquals(1, b.completedMetrics.single().truePositives)
        val secondRun = state.getValue(RunModelKey(2, "a"))
        assertEquals(false, secondRun.finished)
        assertEquals(setOf(1L), secondRun.completedTestCaseIds)

        fun count(type: String) = jdbc.queryForObject(
            "select count(*) from evaluation_run_event where run_id = ? and type = ?", Int::class.java, id, type
        )
        assertEquals(0, count("error"))
        assertEquals(1, count("summary"))
        assertEquals(4, count("testCase"))
        // completed = testCase + error events
        assertEquals(4, repository.getById(id)!!.completedTestCases)
    }

    @Test
    fun `runs and their events are only visible to their owner`() {
        val id = repository.insertRun(user(1), request)
        testCase(id, 1, "a", 1)
        testCase(id, 1, "a", 2)

        assertEquals(listOf(id), repository.listByOwner(user(1)).map { it.id }.filter { it == id })
        assertNull(repository.getByIdAndOwner(id, user(2)))
        assertTrue(repository.eventsAfter(id, user(2), 0, 100).isEmpty())

        val events = repository.eventsAfter(id, user(1), 0, 100)
        assertEquals(2, events.size)
        assertEquals(2L, events[1].report.path("testCaseId").asLong())
        assertEquals(1, repository.eventsAfter(id, user(1), events[0].id, 100).size)
        assertEquals(1, repository.eventsAfter(id, user(1), 0, 1).size)
    }

    @Test
    fun `active runs cannot be deleted, deleting removes the events`() {
        val id = repository.insertRun(user(1), request)
        testCase(id, 1, "a", 1)
        assertFalse(repository.deleteIfNotActive(id, user(1)))

        repository.markCancelled(id)
        assertFalse(repository.deleteIfNotActive(id, user(2)))
        assertTrue(repository.deleteIfNotActive(id, user(1)))
        assertEquals(
            0,
            jdbc.queryForObject("select count(*) from evaluation_run_event where run_id = ?", Int::class.java, id)
        )
    }
}
