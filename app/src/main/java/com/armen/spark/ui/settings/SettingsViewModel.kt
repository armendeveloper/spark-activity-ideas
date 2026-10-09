package com.armen.spark.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armen.spark.data.Settings
import com.armen.spark.data.SettingsRepository
import com.armen.spark.notify.DailyIdeaScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val scheduler: DailyIdeaScheduler,
) : ViewModel() {
    val state: StateFlow<Settings> =
        settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    fun setDailyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setDailyEnabled(enabled)
            if (enabled) scheduler.schedule(settings.settings.first().dailyTime) else scheduler.cancel()
        }
    }

    fun setDailyTime(time: LocalTime) {
        viewModelScope.launch {
            settings.setDailyTime(time)
            if (settings.settings.first().dailyEnabled) scheduler.schedule(time)
        }
    }
}
