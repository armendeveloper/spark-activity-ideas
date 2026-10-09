package com.armen.spark.notify

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
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

/**
 * One run aimed at the next wall-clock [time]. A fixed 24 h periodic request would drift by an hour at
 * every DST change and creep later with job latency, so each run schedules the next one instead.
 */
fun dailyWorkRequest(time: LocalTime, now: ZonedDateTime): OneTimeWorkRequest =
    OneTimeWorkRequestBuilder<DailyIdeaWorker>()
        .setInitialDelay(dailyInitialDelayMillis(time, now), TimeUnit.MILLISECONDS)
        .build()

class DailyIdeaScheduler(private val workManager: WorkManager) {
    /** From Settings: replaces any pending run with one at [time]. */
    fun schedule(time: LocalTime) = enqueue(time, ExistingWorkPolicy.REPLACE)

    /** From the worker itself: queues tomorrow's run behind the one that is finishing. */
    fun scheduleNext(time: LocalTime) = enqueue(time, ExistingWorkPolicy.APPEND_OR_REPLACE)

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private fun enqueue(time: LocalTime, policy: ExistingWorkPolicy) {
        workManager.enqueueUniqueWork(WORK_NAME, policy, dailyWorkRequest(time, ZonedDateTime.now()))
    }

    private companion object {
        const val WORK_NAME = "daily_idea"
    }
}
