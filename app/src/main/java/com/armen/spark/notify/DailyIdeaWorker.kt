package com.armen.spark.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.armen.spark.SparkApp
import com.armen.spark.domain.Filters

class DailyIdeaWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as SparkApp).container
        val repository = container.ideaRepository
        repository.seedIfEmpty()
        val idea = container.picker.pick(repository.allIdeas(), Filters(), repository.lastDoneMap())
            ?: return Result.success()
        Notifications.showDailyIdea(applicationContext, idea)
        return Result.success()
    }
}
