package com.armen.spark.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armen.spark.data.IdeaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

class HistoryViewModel(repository: IdeaRepository) : ViewModel() {
    val sections: StateFlow<List<HistorySection>> = repository.history
        .map { groupHistory(it, System.currentTimeMillis(), ZoneId.systemDefault()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
