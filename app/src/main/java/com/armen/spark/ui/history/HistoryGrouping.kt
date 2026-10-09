package com.armen.spark.ui.history

import com.armen.spark.domain.HistoryItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HistorySection(val label: String, val items: List<HistoryItem>)

/** Groups newest-first [items] by local calendar day. */
fun groupHistory(
    items: List<HistoryItem>,
    now: Long,
    zone: ZoneId,
    locale: Locale = Locale.getDefault(),
): List<HistorySection> {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val dateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale)
    return items
        .groupBy { Instant.ofEpochMilli(it.doneAt).atZone(zone).toLocalDate() }
        .map { (date, dayItems) ->
            val label = when (date) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> date.format(dateFormat)
            }
            HistorySection(label, dayItems)
        }
}
