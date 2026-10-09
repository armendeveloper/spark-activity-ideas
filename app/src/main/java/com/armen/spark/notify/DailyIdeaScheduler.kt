package com.armen.spark.notify

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Milliseconds from [now] until the next occurrence of [time] (strictly in the future). */
fun dailyInitialDelayMillis(time: LocalTime, now: ZonedDateTime): Long {
    var next = now.toLocalDate().atTime(time).atZone(now.zone)
    if (!next.isAfter(now)) next = next.plusDays(1)
    return java.time.Duration.between(now, next).toMillis()
}

class DailyIdeaScheduler(private val workManager: WorkManager) {
    fun schedule(time: LocalTime) {
        val request = PeriodicWorkRequestBuilder<DailyIdeaWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(dailyInitialDelayMillis(time, ZonedDateTime.now()), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "daily_idea"
    }
}
