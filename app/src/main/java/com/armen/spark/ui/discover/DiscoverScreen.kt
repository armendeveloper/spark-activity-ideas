package com.armen.spark.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Idea
import com.armen.spark.domain.People
import com.armen.spark.domain.Place
import com.armen.spark.ui.components.EmptyState
import com.armen.spark.ui.components.FilterRow
import com.armen.spark.ui.components.tagLine

@Composable
fun DiscoverScreen(viewModel: DiscoverViewModel, snackbarHostState: SnackbarHostState) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    val f = state.filters

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("What should I do?", style = MaterialTheme.typography.headlineMedium)
        FilterRow("Time", listOf(Duration.QUICK, Duration.HOUR), f.duration, ::durationFilterLabel) {
            viewModel.setFilters(f.copy(duration = it))
        }
        FilterRow("Budget", listOf(Cost.FREE, Cost.CHEAP), f.maxCost, Cost::label) {
            viewModel.setFilters(f.copy(maxCost = it))
        }
        FilterRow("Place", listOf(Place.INDOOR, Place.OUTDOOR), f.place, Place::label) {
            viewModel.setFilters(f.copy(place = it))
        }
        FilterRow("People", listOf(People.SOLO, People.GROUP), f.people, People::label) {
            viewModel.setFilters(f.copy(people = it))
        }
        FilterRow("Energy", Energy.entries, f.energy, Energy::label) {
            viewModel.setFilters(f.copy(energy = it))
        }

        Button(onClick = viewModel::spark, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("✨  Spark me", style = MaterialTheme.typography.titleMedium)
        }

        val current = state.current
        when {
            current != null -> IdeaCard(
                idea = current,
                onFavorite = viewModel::toggleFavorite,
                onDone = viewModel::markDone,
                onAnother = viewModel::spark,
            )
            state.noMatch -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                EmptyState("🤷", "No ideas fit — loosen a filter", "Fewer filters means more surprises.")
                TextButton(onClick = viewModel::resetFilters) { Text("Reset filters") }
            }
            else -> Text(
                "Pick any filters you like — or none — and tap Spark me.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun durationFilterLabel(duration: Duration) = when (duration) {
    Duration.QUICK -> "15 min"
    Duration.HOUR -> "Up to 1 h"
    Duration.LONG -> "Any"
}

@Composable
private fun IdeaCard(idea: Idea, onFavorite: () -> Unit, onDone: () -> Unit, onAnother: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(idea.emoji, fontSize = 44.sp)
                Spacer(Modifier.weight(1f))
                IconToggleButton(checked = idea.isFavorite, onCheckedChange = { onFavorite() }) {
                    if (idea.isFavorite) {
                        Icon(Icons.Filled.Favorite, "Remove from saved", tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Outlined.FavoriteBorder, "Save")
                    }
                }
            }
            Text(idea.title, style = MaterialTheme.typography.titleLarge)
            Text(idea.category, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (idea.description.isNotBlank()) {
                Text(idea.description, style = MaterialTheme.typography.bodyLarge)
            }
            Text(idea.tagLine(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = onDone) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Done")
                }
                FilledTonalButton(onClick = onAnother) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Another")
                }
            }
        }
    }
}
