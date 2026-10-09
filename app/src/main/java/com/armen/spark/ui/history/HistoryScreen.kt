package com.armen.spark.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armen.spark.ui.components.EmptyState
import com.armen.spark.ui.components.IdeaListItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val timeFormat = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }

    Column(Modifier.fillMaxSize()) {
        Text("History", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        if (sections.isEmpty()) {
            EmptyState("🌱", "Nothing done yet", "Go spark something!")
        } else {
            LazyColumn {
                sections.forEach { section ->
                    item(key = "header-${section.label}") {
                        Text(
                            section.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(section.items, key = { it.entryId }) { entry ->
                        val time = Instant.ofEpochMilli(entry.doneAt).atZone(ZoneId.systemDefault()).format(timeFormat)
                        IdeaListItem(entry.idea, supporting = "${entry.idea.category} · $time")
                    }
                }
            }
        }
    }
}
