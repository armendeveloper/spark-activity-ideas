package com.armen.spark.ui.discover

import com.armen.spark.FakeFilterStore
import com.armen.spark.FakeIdeaRepository
import com.armen.spark.MainDispatcherRule
import com.armen.spark.domain.Cost
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Filters
import com.armen.spark.domain.IdeaPicker
import com.armen.spark.testIdea
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val chill = testIdea(1, energy = Energy.CHILL)
    private val active = testIdea(2, energy = Energy.ACTIVE)

    private fun TestScope.viewModel(repo: FakeIdeaRepository, store: FakeFilterStore = FakeFilterStore()) =
        DiscoverViewModel(repo, store, IdeaPicker(Random(7))).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        }

    @Test
    fun sparkShowsAnIdeaMatchingTheFiltersAndSavesThem() = runTest {
        val store = FakeFilterStore()
        val vm = viewModel(FakeIdeaRepository(listOf(chill, active)), store)

        vm.setFilters(Filters(energy = Energy.ACTIVE))
        vm.spark()

        assertEquals(2L, vm.uiState.value.current?.id)
        assertEquals(Filters(energy = Energy.ACTIVE), store.current)
    }

    @Test
    fun showsNoMatchAndResetClearsIt() = runTest {
        val vm = viewModel(FakeIdeaRepository(listOf(chill)))

        vm.setFilters(Filters(energy = Energy.ACTIVE))
        vm.spark()
        assertTrue(vm.uiState.value.noMatch)
        assertNull(vm.uiState.value.current)

        vm.resetFilters()
        assertFalse(vm.uiState.value.noMatch)
        assertEquals(Filters(), vm.uiState.value.filters)
    }

    @Test
    fun restoresSavedFilters() = runTest {
        val vm = viewModel(FakeIdeaRepository(listOf(chill)), FakeFilterStore(Filters(maxCost = Cost.FREE)))
        assertEquals(Filters(maxCost = Cost.FREE), vm.uiState.value.filters)
    }

    @Test
    fun cardClearsWhenTheShownIdeaIsDeleted() = runTest {
        val mine = testIdea(5, isCustom = true)
        val repo = FakeIdeaRepository(listOf(mine))
        val vm = viewModel(repo)

        vm.spark()
        assertEquals(5L, vm.uiState.value.current?.id)

        repo.deleteCustom(5L)
        assertNull(vm.uiState.value.current)
    }

    @Test
    fun favoriteToggleIsReflectedOnTheCard() = runTest {
        val vm = viewModel(FakeIdeaRepository(listOf(chill)))
        vm.spark()
        vm.toggleFavorite()
        assertTrue(vm.uiState.value.current!!.isFavorite)
    }

    @Test
    fun markDoneLogsTheIdeaAndConfirms() = runTest {
        val repo = FakeIdeaRepository(listOf(chill))
        val vm = viewModel(repo)
        val messages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.messages.toList(messages) }

        vm.spark()
        vm.markDone()

        assertEquals(listOf(1L), repo.doneIds)
        assertEquals(1, messages.size)
    }

    @Test
    fun sparkOnFirstLaunchWaitsForTheSeedInsteadOfReportingNoMatch() = runTest {
        val seeding = CompletableDeferred<Unit>()
        val vm = viewModel(FakeIdeaRepository(seed = listOf(chill), seedGate = seeding))

        vm.spark() // tapped while the bundled ideas are still being imported
        seeding.complete(Unit)

        assertFalse(vm.uiState.value.noMatch)
        assertEquals(1L, vm.uiState.value.current?.id)
    }

    @Test
    fun showIdeaDisplaysTheRequestedIdea() = runTest {
        val vm = viewModel(FakeIdeaRepository(listOf(chill, active)))
        vm.showIdea(2L)
        assertEquals(2L, vm.uiState.value.current?.id)
    }
}
