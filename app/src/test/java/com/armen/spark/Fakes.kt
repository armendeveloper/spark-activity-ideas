package com.armen.spark

import com.armen.spark.data.FilterStore
import com.armen.spark.data.IdeaRepository
import com.armen.spark.domain.Filters
import com.armen.spark.domain.HistoryItem
import com.armen.spark.domain.Idea
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.yield

/** [seed] is loaded by [seedIfEmpty] once [seedGate] (if any) completes, like the real first-launch import. */
class FakeIdeaRepository(
    initial: List<Idea> = emptyList(),
    private val seed: List<Idea> = emptyList(),
    private val seedGate: CompletableDeferred<Unit>? = null,
) : IdeaRepository {
    val ideaFlow = MutableStateFlow(initial)
    val historyFlow = MutableStateFlow(emptyList<HistoryItem>())
    val doneIds = mutableListOf<Long>()
    val savedCustom = mutableListOf<Idea>()

    override val ideas: Flow<List<Idea>> = ideaFlow
    override val saved: Flow<List<Idea>> = ideaFlow.map { list -> list.filter { it.isFavorite || it.isCustom } }
    override val history: Flow<List<HistoryItem>> = historyFlow
    override suspend fun allIdeas() = ideaFlow.value
    override suspend fun getIdea(id: Long) = ideaFlow.value.firstOrNull { it.id == id }
    override suspend fun lastDoneMap() = emptyMap<Long, Long>()
    override suspend fun toggleFavorite(id: Long) {
        ideaFlow.update { list -> list.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it } }
    }
    override suspend fun markDone(id: Long) { doneIds += id }
    override suspend fun saveCustom(idea: Idea) {
        yield() // behave like real I/O: give other coroutines a chance to run
        savedCustom += idea
    }
    override suspend fun deleteCustom(id: Long) {
        ideaFlow.update { list -> list.filterNot { it.id == id } }
    }
    override suspend fun seedIfEmpty() {
        seedGate?.await()
        if (ideaFlow.value.isEmpty()) ideaFlow.value = seed
    }
}

class FakeFilterStore(initial: Filters = Filters()) : FilterStore {
    private val state = MutableStateFlow(initial)
    val current: Filters get() = state.value
    override val filters: Flow<Filters> = state
    override suspend fun setFilters(filters: Filters) { state.value = filters }
}
