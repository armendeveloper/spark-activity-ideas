package com.armen.spark.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object Notifications {
    const val CHANNEL_ID = "daily_idea"
    const val EXTRA_IDEA_ID = "com.armen.spark.IDEA_ID"
    const val DAILY_NOTIFICATION_ID = 1

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Daily idea", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "One activity suggestion a day"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
