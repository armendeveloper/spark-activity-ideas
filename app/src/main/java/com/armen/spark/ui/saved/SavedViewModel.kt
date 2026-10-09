package com.armen.spark.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armen.spark.data.IdeaRepository
import com.armen.spark.domain.Idea
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedViewModel(private val repository: IdeaRepository) : ViewModel() {
    val ideas: StateFlow<List<Idea>> =
        repository.saved.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteCustom(id) }
    }
}
