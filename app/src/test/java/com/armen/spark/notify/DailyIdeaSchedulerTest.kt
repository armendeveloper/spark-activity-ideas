package com.armen.spark.notify

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyIdeaSchedulerTest {
    private val hour = 60L * 60 * 1000
    private val yerevan = ZoneId.of("Asia/Yerevan")

    @Test
    fun laterTodayWhenTimeHasNotPassed() {
        val now = ZonedDateTime.of(2026, 10, 9, 8, 0, 0, 0, yerevan)
        assertEquals(1 * hour, dailyInitialDelayMillis(LocalTime.of(9, 0), now))
    }

    @Test
    fun tomorrowWhenTimeHasPassed() {
        val now = ZonedDateTime.of(2026, 10, 9, 10, 0, 0, 0, yerevan)
        assertEquals(23 * hour, dailyInitialDelayMillis(LocalTime.of(9, 0), now))
    }

    @Test
    fun tomorrowWhenTimeIsExactlyNow() {
        val now = ZonedDateTime.of(2026, 10, 9, 9, 0, 0, 0, yerevan)
        assertEquals(24 * hour, dailyInitialDelayMillis(LocalTime.of(9, 0), now))
    }

    @Test
    fun accountsForDaylightSavingJump() {
        // Clocks in Berlin spring forward at 02:00 on 29 March 2026, so 22:00 → 09:00 is 10 real hours.
        val now = ZonedDateTime.of(2026, 3, 28, 22, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        assertEquals(10 * hour, dailyInitialDelayMillis(LocalTime.of(9, 0), now))
    }
}
