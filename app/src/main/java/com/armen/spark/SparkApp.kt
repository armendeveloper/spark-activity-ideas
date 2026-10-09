package com.armen.spark

import android.app.Application
import com.armen.spark.notify.Notifications

class SparkApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannel(this)
    }
}
