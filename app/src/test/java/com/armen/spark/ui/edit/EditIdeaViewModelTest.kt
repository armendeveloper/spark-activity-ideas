package com.armen.spark.ui.edit

import com.armen.spark.FakeIdeaRepository
import com.armen.spark.MainDispatcherRule
import com.armen.spark.testIdea
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditIdeaViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun doubleTapOnSaveStoresOneIdea() = runTest {
        val repo = FakeIdeaRepository()
        val vm = EditIdeaViewModel(repo, ideaId = null)
        vm.update { it.copy(title = "Paint a mug") }
        var savedCalls = 0

        vm.save { savedCalls++ }
        vm.save { savedCalls++ }
        advanceUntilIdle()

        assertEquals(1, repo.savedCustom.size)
        assertEquals(1, savedCalls)
    }

    @Test
    fun blankTitleIsNotSaved() = runTest {
        val repo = FakeIdeaRepository()
        val vm = EditIdeaViewModel(repo, ideaId = null)
        vm.save { }
        advanceUntilIdle()
        assertTrue(repo.savedCustom.isEmpty())
    }

    @Test
    fun loadsExistingCustomIdea() = runTest {
        val repo = FakeIdeaRepository(listOf(testIdea(3, title = "Juggle", isCustom = true)))
        val vm = EditIdeaViewModel(repo, ideaId = 3L)
        advanceUntilIdle()
        assertFalse(vm.isNew)
        assertEquals("Juggle", vm.draft.value.title)
        assertEquals(3L, vm.draft.value.id)
    }
}
