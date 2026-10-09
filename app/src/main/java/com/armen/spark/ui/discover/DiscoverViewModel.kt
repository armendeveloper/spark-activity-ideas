package com.armen.spark.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armen.spark.data.FilterStore
import com.armen.spark.data.IdeaRepository
import com.armen.spark.domain.Filters
import com.armen.spark.domain.Idea
import com.armen.spark.domain.IdeaPicker
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val filters: Filters = Filters(),
    val current: Idea? = null,
    val noMatch: Boolean = false,
)

class DiscoverViewModel(
    private val repository: IdeaRepository,
    private val filterStore: FilterStore,
    private val picker: IdeaPicker,
) : ViewModel() {
    private val filters = MutableStateFlow(Filters())
    private val currentId = MutableStateFlow<Long?>(null)
    private val noMatch = MutableStateFlow(false)
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    // The card is resolved against the live list, so edits, favorites and deletions show up immediately.
    val uiState: StateFlow<DiscoverUiState> =
        combine(filters, currentId, noMatch, repository.ideas) { f, id, nm, ideas ->
            DiscoverUiState(filters = f, current = id?.let { wanted -> ideas.firstOrNull { it.id == wanted } }, noMatch = nm)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState())

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
        viewModelScope.launch {
            val saved = filterStore.filters.first()
            filters.compareAndSet(Filters(), saved) // don't clobber a filter the user already tapped
        }
    }

    fun setFilters(newFilters: Filters) {
        filters.value = newFilters
        noMatch.value = false
        viewModelScope.launch { filterStore.setFilters(newFilters) }
    }

    fun resetFilters() = setFilters(Filters())

    fun spark() {
        viewModelScope.launch {
            val idea = picker.pick(
                candidates = repository.allIdeas(),
                filters = filters.value,
                lastDone = repository.lastDoneMap(),
                excludeId = currentId.value,
            )
            currentId.value = idea?.id
            noMatch.value = idea == null
        }
    }

    fun showIdea(id: Long) {
        currentId.value = id
        noMatch.value = false
    }

    fun toggleFavorite() {
        val id = currentId.value ?: return
        viewModelScope.launch { repository.toggleFavorite(id) }
    }

    fun markDone() {
        val id = currentId.value ?: return
        viewModelScope.launch {
            repository.markDone(id)
            _messages.send("Nice! Logged in History ✓")
        }
    }
}
