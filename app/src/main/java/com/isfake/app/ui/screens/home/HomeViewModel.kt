package com.isfake.app.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isfake.app.R
import com.isfake.app.data.model.FakeLocation
import com.isfake.app.data.repository.LocationRepository
import com.isfake.app.service.MockLocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isServiceActive: Boolean = false,
    val selectedLat: Double? = null,
    val selectedLng: Double? = null,
    val latInput: String = "",
    val lngInput: String = "",
    val selectedTabIndex: Int = 0, // 0=Map, 1=Coords, 2=Presets
    val showSavePresetDialog: Boolean = false,
    val presetNameInput: String = "",
    val showOnboarding: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val zoomInTrigger: Int = 0,
    val zoomOutTrigger: Int = 0,
    val centerTrigger: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LocationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val presets = repository.presets.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val updateIntervalMs = repository.updateIntervalMs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        1000L
    )

    val accuracyMeters = repository.accuracyMeters.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        1.0f
    )

    val altitudeMeters = repository.altitudeMeters.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        10.0f
    )

    init {
        val isServiceActiveInitial = MockLocationService.isServiceActive(context)
        val savedLocation = MockLocationService.getSavedLocation(context)

        if (isServiceActiveInitial && savedLocation != null) {
            _uiState.update {
                it.copy(
                    isServiceActive = true,
                    selectedLat = it.selectedLat ?: savedLocation.first,
                    selectedLng = it.selectedLng ?: savedLocation.second,
                    latInput = it.latInput.ifBlank { "%.6f".format(savedLocation.first) },
                    lngInput = it.lngInput.ifBlank { "%.6f".format(savedLocation.second) }
                )
            }
        } else if (isServiceActiveInitial) {
            _uiState.update { it.copy(isServiceActive = true) }
        }

        viewModelScope.launch {
            MockLocationService.isRunning.collect { running ->
                _uiState.update { it.copy(isServiceActive = running) }
            }
        }

        viewModelScope.launch {
            repository.isOnboardingShown.collect { shown ->
                if (!shown) {
                    _uiState.update { it.copy(showOnboarding = true) }
                }
            }
        }
    }

    fun onMapLocationSelected(lat: Double, lng: Double) {
        _uiState.update {
            it.copy(
                selectedLat = lat,
                selectedLng = lng,
                latInput = "%.6f".format(lat),
                lngInput = "%.6f".format(lng),
                errorMessage = null
            )
        }
    }

    fun onLatInputChanged(value: String) {
        _uiState.update { it.copy(latInput = value, errorMessage = null) }
        value.toDoubleOrNull()?.let { lat ->
            _uiState.value.lngInput.toDoubleOrNull()?.let { lng ->
                _uiState.update { it.copy(selectedLat = lat, selectedLng = lng) }
            }
        }
    }

    fun onLngInputChanged(value: String) {
        _uiState.update { it.copy(lngInput = value, errorMessage = null) }
        value.toDoubleOrNull()?.let { lng ->
            _uiState.value.latInput.toDoubleOrNull()?.let { lat ->
                _uiState.update { it.copy(selectedLat = lat, selectedLng = lng) }
            }
        }
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun zoomIn() {
        _uiState.update { it.copy(zoomInTrigger = it.zoomInTrigger + 1) }
    }

    fun zoomOut() {
        _uiState.update { it.copy(zoomOutTrigger = it.zoomOutTrigger + 1) }
    }

    fun centerLocation() {
        _uiState.update { it.copy(centerTrigger = it.centerTrigger + 1) }
    }

    fun startService(): Boolean {
        val lat = _uiState.value.selectedLat
        val lng = _uiState.value.selectedLng
        if (lat == null || lng == null) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.error_select_location)) }
            return false
        }
        _uiState.update { it.copy(isServiceActive = true, errorMessage = null) }
        return true
    }

    fun stopService() {
        _uiState.update { it.copy(isServiceActive = false) }
    }

    fun showSavePresetDialog() {
        if (_uiState.value.selectedLat == null || _uiState.value.selectedLng == null) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.error_select_point_to_save)) }
            return
        }
        _uiState.update { it.copy(showSavePresetDialog = true, presetNameInput = "") }
    }

    fun dismissSavePresetDialog() {
        _uiState.update { it.copy(showSavePresetDialog = false) }
    }

    fun onPresetNameChanged(name: String) {
        _uiState.update { it.copy(presetNameInput = name) }
    }

    fun saveCurrentAsPreset() {
        val lat = _uiState.value.selectedLat ?: return
        val lng = _uiState.value.selectedLng ?: return
        val name = _uiState.value.presetNameInput.trim()
        if (name.isEmpty()) return

        viewModelScope.launch {
            repository.savePreset(
                FakeLocation(
                    name = name,
                    latitude = lat,
                    longitude = lng,
                    altitude = altitudeMeters.value.toDouble(),
                    accuracy = accuracyMeters.value
                )
            )
            _uiState.update { it.copy(showSavePresetDialog = false) }
        }
    }

    fun usePreset(preset: FakeLocation) {
        _uiState.update {
            it.copy(
                selectedLat = preset.latitude,
                selectedLng = preset.longitude,
                latInput = preset.latFormatted(),
                lngInput = preset.lngFormatted(),
                selectedTabIndex = 0,
                centerTrigger = it.centerTrigger + 1
            )
        }
    }

    fun deletePreset(preset: FakeLocation) {
        viewModelScope.launch {
            repository.deletePreset(preset.id)
        }
    }

    fun showSettings() {
        _uiState.update { it.copy(showSettingsDialog = true) }
    }

    fun dismissSettings() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun updateSettings(intervalMs: Long, accuracy: Float, altitude: Float) {
        viewModelScope.launch {
            repository.setUpdateInterval(intervalMs)
            repository.setAccuracy(accuracy)
            repository.setAltitude(altitude)
        }
    }

    fun showOnboarding() {
        _uiState.update { it.copy(showOnboarding = true) }
    }

    fun dismissOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingShown()
            _uiState.update { it.copy(showOnboarding = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
