package com.armen.spark.ui.saved

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armen.spark.domain.Idea
import com.armen.spark.ui.components.EmptyState
import com.armen.spark.ui.components.IdeaListItem

@Composable
fun SavedScreen(viewModel: SavedViewModel, onAdd: () -> Unit, onEdit: (Long) -> Unit) {
    val ideas by viewModel.ideas.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Idea?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Text("Saved", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
            if (ideas.isEmpty()) {
                EmptyState("💛", "No saved ideas yet", "Tap ♥ on a suggestion, or + to add your own.")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(ideas, key = { it.id }) { idea ->
                        IdeaListItem(idea, trailing = {
                            Row {
                                if (idea.isCustom) {
                                    IconButton(onClick = { onEdit(idea.id) }) { Icon(Icons.Filled.Edit, "Edit") }
                                    IconButton(onClick = { pendingDelete = idea }) { Icon(Icons.Filled.Delete, "Delete") }
                                }
                                IconButton(onClick = { viewModel.toggleFavorite(idea.id) }) {
                                    if (idea.isFavorite) {
                                        Icon(Icons.Filled.Favorite, "Remove from saved", tint = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Icon(Icons.Outlined.FavoriteBorder, "Save")
                                    }
                                }
                            }
                        })
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "Add your own idea") }
    }

    pendingDelete?.let { idea ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete “${idea.title}”?") },
            text = { Text("This also removes it from your history.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(idea.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}
