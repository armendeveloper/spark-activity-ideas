package com.armen.spark.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Filters
import com.armen.spark.domain.People
import com.armen.spark.domain.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalTime

interface FilterStore {
    val filters: Flow<Filters>
    suspend fun setFilters(filters: Filters)
}

data class Settings(
    val dailyEnabled: Boolean = false,
    val dailyTime: LocalTime = LocalTime.of(9, 0),
)

class SettingsRepository(private val store: DataStore<Preferences>) : FilterStore {
    private val prefs: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: Flow<Settings> = prefs.map { p ->
        Settings(
            dailyEnabled = p[DAILY_ENABLED] ?: false,
            dailyTime = LocalTime.of(p[DAILY_HOUR] ?: 9, p[DAILY_MINUTE] ?: 0),
        )
    }

    override val filters: Flow<Filters> = prefs.map { p ->
        Filters(
            duration = p[FILTER_DURATION].toEnumOrNull<Duration>(),
            maxCost = p[FILTER_COST].toEnumOrNull<Cost>(),
            place = p[FILTER_PLACE].toEnumOrNull<Place>(),
            people = p[FILTER_PEOPLE].toEnumOrNull<People>(),
            energy = p[FILTER_ENERGY].toEnumOrNull<Energy>(),
        )
    }

    suspend fun setDailyEnabled(enabled: Boolean) {
        store.edit { it[DAILY_ENABLED] = enabled }
    }

    suspend fun setDailyTime(time: LocalTime) {
        store.edit {
            it[DAILY_HOUR] = time.hour
            it[DAILY_MINUTE] = time.minute
        }
    }

    override suspend fun setFilters(filters: Filters) {
        store.edit { p ->
            p.putOrRemove(FILTER_DURATION, filters.duration?.name)
            p.putOrRemove(FILTER_COST, filters.maxCost?.name)
            p.putOrRemove(FILTER_PLACE, filters.place?.name)
            p.putOrRemove(FILTER_PEOPLE, filters.people?.name)
            p.putOrRemove(FILTER_ENERGY, filters.energy?.name)
        }
    }

    private fun MutablePreferences.putOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value == null) remove(key) else this[key] = value
    }

    private companion object {
        val DAILY_ENABLED = booleanPreferencesKey("daily_enabled")
        val DAILY_HOUR = intPreferencesKey("daily_hour")
        val DAILY_MINUTE = intPreferencesKey("daily_minute")
        val FILTER_DURATION = stringPreferencesKey("filter_duration")
        val FILTER_COST = stringPreferencesKey("filter_cost")
        val FILTER_PLACE = stringPreferencesKey("filter_place")
        val FILTER_PEOPLE = stringPreferencesKey("filter_people")
        val FILTER_ENERGY = stringPreferencesKey("filter_energy")
    }
}

private inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
    this?.let { value -> enumValues<E>().firstOrNull { it.name == value } }
