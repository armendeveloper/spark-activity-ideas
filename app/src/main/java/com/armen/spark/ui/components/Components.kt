package com.armen.spark.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armen.spark.domain.Idea

fun Idea.tagLine(): String = listOf(
    "⏱ ${duration.label}", "💸 ${cost.label}", "📍 ${place.label}", "👥 ${people.label}", "🔋 ${energy.label}",
).joinToString("  ·  ")

/** One row of chips. Tapping the selected chip again reports null (= "any"). */
@Composable
fun <T> FilterRow(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(if (option == selected) null else option) },
                    label = { Text(label(option)) },
                )
            }
        }
    }
}

@Composable
fun IdeaListItem(
    idea: Idea,
    supporting: String = idea.category,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListItem(
        leadingContent = { Text(idea.emoji, fontSize = 28.sp) },
        headlineContent = { Text(idea.title) },
        supportingContent = { Text(if (idea.isCustom) "Mine · $supporting" else supporting) },
        trailingContent = trailing,
    )
}

@Composable
fun EmptyState(emoji: String, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(emoji, fontSize = 44.sp)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
