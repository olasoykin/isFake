package com.isfake.app.data.repository

import com.isfake.app.data.datastore.PreferencesDataStore
import com.isfake.app.data.model.FakeLocation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface LocationRepository {
    val presets: Flow<List<FakeLocation>>
    val updateIntervalMs: Flow<Long>
    val accuracyMeters: Flow<Float>
    val altitudeMeters: Flow<Float>
    val isOnboardingShown: Flow<Boolean>

    suspend fun savePreset(location: FakeLocation)
    suspend fun deletePreset(id: String)
    suspend fun setUpdateInterval(ms: Long)
    suspend fun setAccuracy(meters: Float)
    suspend fun setAltitude(meters: Float)
    suspend fun setOnboardingShown()
}

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val dataStore: PreferencesDataStore
) : LocationRepository {

    override val presets: Flow<List<FakeLocation>> = dataStore.presets
    override val updateIntervalMs: Flow<Long> = dataStore.updateIntervalMs
    override val accuracyMeters: Flow<Float> = dataStore.accuracyMeters
    override val altitudeMeters: Flow<Float> = dataStore.altitudeMeters
    override val isOnboardingShown: Flow<Boolean> = dataStore.isOnboardingShown

    override suspend fun savePreset(location: FakeLocation) = dataStore.savePreset(location)
    override suspend fun deletePreset(id: String) = dataStore.deletePreset(id)
    override suspend fun setUpdateInterval(ms: Long) = dataStore.setUpdateInterval(ms)
    override suspend fun setAccuracy(meters: Float) = dataStore.setAccuracy(meters)
    override suspend fun setAltitude(meters: Float) = dataStore.setAltitude(meters)
    override suspend fun setOnboardingShown() = dataStore.setOnboardingShown()
}
