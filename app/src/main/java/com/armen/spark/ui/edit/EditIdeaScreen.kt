package com.armen.spark.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armen.spark.domain.CATEGORIES
import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.People
import com.armen.spark.domain.Place
import com.armen.spark.ui.components.FilterRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIdeaScreen(viewModel: EditIdeaViewModel, onBack: () -> Unit) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().imePadding()) {
        TopAppBar(
            title = { Text(if (viewModel.isNew) "New idea" else "Edit idea") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.emoji,
                    onValueChange = { v -> viewModel.update { it.copy(emoji = v) } },
                    label = { Text("Emoji") },
                    singleLine = true,
                    modifier = Modifier.width(96.dp),
                )
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { v -> viewModel.update { it.copy(title = v) } },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = draft.description,
                onValueChange = { v -> viewModel.update { it.copy(description = v) } },
                label = { Text("Description (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            // Tag rows are single-choice: tapping the selected chip (null) keeps the current value.
            FilterRow("Category", CATEGORIES, draft.category, { it }) { v ->
                v?.let { c -> viewModel.update { it.copy(category = c) } }
            }
            FilterRow("Time", Duration.entries, draft.duration, Duration::label) { v ->
                v?.let { d -> viewModel.update { it.copy(duration = d) } }
            }
            FilterRow("Cost", Cost.entries, draft.cost, Cost::label) { v ->
                v?.let { c -> viewModel.update { it.copy(cost = c) } }
            }
            FilterRow("Place", Place.entries, draft.place, Place::label) { v ->
                v?.let { p -> viewModel.update { it.copy(place = p) } }
            }
            FilterRow("People", People.entries, draft.people, People::label) { v ->
                v?.let { p -> viewModel.update { it.copy(people = p) } }
            }
            FilterRow("Energy", Energy.entries, draft.energy, Energy::label) { v ->
                v?.let { e -> viewModel.update { it.copy(energy = e) } }
            }
        }
        Button(
            onClick = { viewModel.save(onSaved = onBack) },
            enabled = draft.canSave,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { Text("Save") }
    }
}
