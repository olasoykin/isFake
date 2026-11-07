package com.isfake.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.isfake.app.data.model.FakeLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "isfake_prefs")

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Presets stored as a simple pipe-delimited string: id|name|lat|lng|alt|acc|ts
        private val PRESETS_KEY = stringPreferencesKey("presets")
        private val UPDATE_INTERVAL_KEY = longPreferencesKey("update_interval_ms")
        private val ACCURACY_KEY = floatPreferencesKey("accuracy_meters")
        private val ALTITUDE_KEY = floatPreferencesKey("altitude_meters")
        private val ONBOARDING_SHOWN_KEY = stringPreferencesKey("onboarding_shown")

        private const val PRESET_SEPARATOR = ";;;PRESET;;;"
        private const val FIELD_SEPARATOR = "|||"
    }

    // --- Presets ---

    val presets: Flow<List<FakeLocation>> = context.dataStore.data.map { prefs ->
        val raw = prefs[PRESETS_KEY] ?: return@map emptyList()
        if (raw.isBlank()) return@map emptyList()
        raw.split(PRESET_SEPARATOR).mapNotNull { entry ->
            try {
                val parts = entry.split(FIELD_SEPARATOR)
                FakeLocation(
                    id = parts[0],
                    name = parts[1],
                    latitude = parts[2].toDouble(),
                    longitude = parts[3].toDouble(),
                    altitude = parts[4].toDouble(),
                    accuracy = parts[5].toFloat(),
                    createdAt = parts[6].toLong()
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun savePreset(location: FakeLocation) {
        context.dataStore.edit { prefs ->
            val current = prefs[PRESETS_KEY] ?: ""
            val existing = if (current.isBlank()) emptyList()
            else current.split(PRESET_SEPARATOR).toMutableList()
            val entry = listOf(
                location.id, location.name,
                location.latitude.toString(), location.longitude.toString(),
                location.altitude.toString(), location.accuracy.toString(),
                location.createdAt.toString()
            ).joinToString(FIELD_SEPARATOR)
            val updated = (existing + entry).joinToString(PRESET_SEPARATOR)
            prefs[PRESETS_KEY] = updated
        }
    }

    suspend fun deletePreset(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PRESETS_KEY] ?: return@edit
            if (current.isBlank()) return@edit
            val updated = current.split(PRESET_SEPARATOR)
                .filter { entry ->
                    try { entry.split(FIELD_SEPARATOR)[0] != id } catch (e: Exception) { false }
                }
                .joinToString(PRESET_SEPARATOR)
            prefs[PRESETS_KEY] = updated
        }
    }

    // --- Settings ---

    val updateIntervalMs: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[UPDATE_INTERVAL_KEY] ?: 1000L
    }

    val accuracyMeters: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[ACCURACY_KEY] ?: 1.0f
    }

    val altitudeMeters: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[ALTITUDE_KEY] ?: 10.0f
    }

    suspend fun setUpdateInterval(ms: Long) {
        context.dataStore.edit { it[UPDATE_INTERVAL_KEY] = ms }
    }

    suspend fun setAccuracy(meters: Float) {
        context.dataStore.edit { it[ACCURACY_KEY] = meters }
    }

    suspend fun setAltitude(meters: Float) {
        context.dataStore.edit { it[ALTITUDE_KEY] = meters }
    }

    val isOnboardingShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_SHOWN_KEY] == "true"
    }

    suspend fun setOnboardingShown() {
        context.dataStore.edit { it[ONBOARDING_SHOWN_KEY] = "true" }
    }
}
