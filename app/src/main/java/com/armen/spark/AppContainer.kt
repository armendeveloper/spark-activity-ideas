package com.armen.spark

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import com.armen.spark.data.IdeaRepository
import com.armen.spark.data.RoomIdeaRepository
import com.armen.spark.data.SeedParser
import com.armen.spark.data.SettingsRepository
import com.armen.spark.data.SparkDatabase
import com.armen.spark.domain.IdeaPicker
import com.armen.spark.notify.DailyIdeaScheduler

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = SparkDatabase.build(appContext)

    val ideaRepository: IdeaRepository = RoomIdeaRepository(
        db = database,
        loadSeed = { SeedParser.parse(appContext.assets.open("activities.json").bufferedReader().use { it.readText() }) },
    )
    val settingsRepository = SettingsRepository(appContext.settingsDataStore)
    val picker = IdeaPicker()
    val scheduler = DailyIdeaScheduler(WorkManager.getInstance(appContext))
}
