package com.armen.spark.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.armen.spark.SparkApp
import com.armen.spark.domain.Filters
import kotlinx.coroutines.flow.first

class DailyIdeaWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as SparkApp).container
        val settings = container.settingsRepository.settings.first()
        if (!settings.dailyEnabled) return Result.success()

        val repository = container.ideaRepository
        repository.seedIfEmpty()
        container.picker.pick(repository.allIdeas(), Filters(), repository.lastDoneMap())
            ?.let { Notifications.showDailyIdea(applicationContext, it) }

        container.scheduler.scheduleNext(settings.dailyTime)
        return Result.success()
    }
}
