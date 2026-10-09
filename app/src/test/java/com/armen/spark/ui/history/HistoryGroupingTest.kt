package com.armen.spark.ui.history

import com.armen.spark.domain.HistoryItem
import com.armen.spark.testIdea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class HistoryGroupingTest {
    private val zone = ZoneId.of("Asia/Yerevan") // UTC+4: local midnight is 20:00 UTC

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    private fun item(id: Long, doneAt: Long) = HistoryItem(entryId = id, idea = testIdea(id), doneAt = doneAt)

    @Test
    fun labelsTodayYesterdayAndOlderDatesInLocalTime() {
        val now = at(2026, 10, 9, 0, 30)
        val items = listOf(
            item(1, at(2026, 10, 9, 0, 10)),   // 20 min ago, after local midnight
            item(2, at(2026, 10, 8, 23, 50)),  // 40 min ago, before local midnight
            item(3, at(2026, 10, 8, 9, 0)),
            item(4, at(2026, 10, 1, 12, 0)),
        )

        val sections = groupHistory(items, now, zone, Locale.ENGLISH)

        assertEquals(listOf("Today", "Yesterday", "Thu, 1 Oct 2026"), sections.map { it.label })
        assertEquals(listOf(1L), sections[0].items.map { it.entryId })
        assertEquals(listOf(2L, 3L), sections[1].items.map { it.entryId })
    }

    @Test
    fun emptyHistoryHasNoSections() {
        assertTrue(groupHistory(emptyList(), at(2026, 10, 9, 12, 0), zone, Locale.ENGLISH).isEmpty())
    }
}
