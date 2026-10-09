package com.armen.spark.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armen.spark.data.IdeaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditIdeaViewModel(
    private val repository: IdeaRepository,
    ideaId: Long?,
) : ViewModel() {
    val isNew: Boolean = ideaId == null
    private val _draft = MutableStateFlow(IdeaDraft())
    val draft: StateFlow<IdeaDraft> = _draft.asStateFlow()
    private var saving = false

    init {
        if (ideaId != null) {
            viewModelScope.launch {
                repository.getIdea(ideaId)?.takeIf { it.isCustom }?.let { _draft.value = IdeaDraft.from(it) }
            }
        }
    }

    fun update(transform: (IdeaDraft) -> IdeaDraft) {
        _draft.update(transform)
    }

    fun save(onSaved: () -> Unit) {
        if (saving) return
        val idea = _draft.value.toIdeaOrNull() ?: return
        saving = true
        viewModelScope.launch {
            repository.saveCustom(idea)
            onSaved()
        }
    }
}
