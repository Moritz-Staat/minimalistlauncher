package de.moritzstaat.launcher.ui.weather

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.weather.TemperatureUnit
import de.moritzstaat.launcher.data.weather.WeatherSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The weather line: the cached reading plus the pull the user can trigger by tapping it. */
class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as LauncherApplication).services
    private val repository = services.weatherRepository

    /** Null while the weather is off, unavailable or not fetched yet. */
    val snapshot: StateFlow<WeatherSnapshot?> = combine(
        repository.snapshot,
        services.settings.weatherEnabled,
    ) { snapshot, enabled -> snapshot.takeIf { enabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val enabled: StateFlow<Boolean> = services.settings.weatherEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val unit: StateFlow<TemperatureUnit> = services.settings.temperatureUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TemperatureUnit.Celsius)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    fun hasLocationPermission(): Boolean = services.locationProvider.hasPermission()

    /** @param force what a tap on the row does; the automatic refresh respects the interval. */
    fun refresh(force: Boolean = false) {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            try {
                repository.refresh(force)
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            services.settings.setWeatherEnabled(enabled)
            if (enabled) refresh(force = true)
        }
    }

    fun setUnit(unit: TemperatureUnit) {
        viewModelScope.launch {
            services.settings.setTemperatureUnit(unit)
            // The cached reading is in the old unit; converting it here would only guess.
            refresh(force = true)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
