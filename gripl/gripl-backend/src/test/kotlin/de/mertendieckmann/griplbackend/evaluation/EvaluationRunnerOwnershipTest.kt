package de.mertendieckmann.griplbackend.evaluation

import de.mertendieckmann.griplbackend.evaluation.service.Evaluator
import de.mertendieckmann.griplbackend.evaluation.service.RagasEvaluationService
import de.mertendieckmann.griplbackend.model.dto.EvaluationRequest
import de.mertendieckmann.griplbackend.repository.EvaluationDataRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * GRIPL-v2#40 — dataset/test-case ids in an evaluation request are
 * client-supplied, so [EvaluationRunner] must always resolve them through
 * the owner-scoped repository lookups, never the unscoped ones — otherwise a
 * caller with a privileged GRIPL role (admin/researcher) could run an
 * evaluation against, and read back the contents of, another user's private
 * dataset by id.
 */
class EvaluationRunnerOwnershipTest {

    private val repo = mock<EvaluationDataRepository>()
    private val runner = EvaluationRunner(repo, mock<Evaluator>(), mock<RagasEvaluationService>())

    @Test
    fun `an evaluationDataIds request is resolved through the owner-scoped lookup`() {
        runBlocking {
            whenever(repo.getEvaluationDataByIdsForOwner(eq(listOf(1, 2)), eq("user-1"))).thenReturn(emptyList())

            runner.run(EvaluationRequest(datasets = emptyList(), evaluationDataIds = listOf(1, 2)), "user-1").toList()

            verify(repo).getEvaluationDataByIdsForOwner(listOf(1, 2), "user-1")
        }
    }

    @Test
    fun `a dataset-scoped request is resolved through the owner-scoped lookup`() {
        runBlocking {
            whenever(repo.getEvaluationDataByDatasetIdsOrAllForOwner(eq(listOf(5)), eq("user-1"))).thenReturn(emptyList())

            runner.run(EvaluationRequest(datasets = listOf(5)), "user-1").toList()

            verify(repo).getEvaluationDataByDatasetIdsOrAllForOwner(listOf(5), "user-1")
        }
    }

    @Test
    fun `a different caller only ever resolves under their own id`() {
        runBlocking {
            whenever(repo.getEvaluationDataByIdsForOwner(eq(listOf(99)), any())).thenReturn(emptyList())

            runner.run(EvaluationRequest(datasets = emptyList(), evaluationDataIds = listOf(99)), "attacker").toList()

            verify(repo).getEvaluationDataByIdsForOwner(listOf(99), "attacker")
            verify(repo, never()).getEvaluationDataByIdsForOwner(listOf(99), "victim")
        }
    }
}
