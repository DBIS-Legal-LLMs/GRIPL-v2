package de.mertendieckmann.griplbackend.evaluation

import de.mertendieckmann.griplbackend.model.dto.Dataset
import de.mertendieckmann.griplbackend.model.dto.ModelRunConfig
import de.mertendieckmann.griplbackend.model.dto.MultiEvaluationRequest
import de.mertendieckmann.griplbackend.repository.DatasetRepository
import de.mertendieckmann.griplbackend.repository.EvaluationDataRepository
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * GRIPL-v2#40 — both the metadata report and the underlying test-case run
 * triggered by [MultiEvaluationRunner.runAll] must resolve datasets under the
 * caller's own id, not just gate on GRIPL role.
 */
class MultiEvaluationRunnerOwnershipTest {

    private val singleRunner = mock<EvaluationRunner>()
    private val datasetRepository = mock<DatasetRepository>()
    private val evaluationDataRepository = mock<EvaluationDataRepository>()
    private val runner = MultiEvaluationRunner(singleRunner, datasetRepository, evaluationDataRepository)

    @Test
    fun `metadata and the underlying run are both scoped to the caller`() {
        runBlocking {
            whenever(datasetRepository.getDatasetsByIdsAndOwner(eq(listOf(5L)), eq("user-1")))
                .thenReturn(listOf(Dataset(id = 5, name = "mine", description = null, createdAt = "now", updatedAt = "now")))
            whenever(singleRunner.run(any(), eq("user-1"))).thenReturn(emptyFlow())

            val request = MultiEvaluationRequest(
                defaultEvaluationEndpoint = "endpoint",
                models = listOf(ModelRunConfig(label = "m1")),
                seed = 1,
                datasets = listOf(5),
            )

            runner.runAll(request, "user-1").toList()

            verify(datasetRepository).getDatasetsByIdsAndOwner(listOf(5L), "user-1")
            verify(singleRunner).run(any(), eq("user-1"))
        }
    }

    @Test
    fun `a caller can never pull metadata for a dataset scoped to a different owner`() {
        runBlocking {
            // Simulate: dataset 5 exists but is not owned by "attacker" -> the
            // owner-scoped lookup returns nothing for them.
            whenever(datasetRepository.getDatasetsByIdsAndOwner(eq(listOf(5L)), eq("attacker")))
                .thenReturn(emptyList())
            whenever(singleRunner.run(any(), eq("attacker"))).thenReturn(emptyFlow())

            val request = MultiEvaluationRequest(
                defaultEvaluationEndpoint = "endpoint",
                models = listOf(ModelRunConfig(label = "m1")),
                seed = 1,
                datasets = listOf(5),
            )

            val envelopes = runner.runAll(request, "attacker").toList()

            val metadataReport = envelopes.first().report as de.mertendieckmann.griplbackend.model.dto.EvaluationMetadataReport
            org.junit.jupiter.api.Assertions.assertEquals(emptyList<Any>(), metadataReport.datasets)
        }
    }
}
