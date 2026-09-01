package de.moritzstaat.launcher.data.weather

import de.moritzstaat.launcher.data.settings.LauncherSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The current weather, cached in the settings store.
 *
 * The cache is the source of truth for the UI: the home screen shows the last reading right
 * away and the network call only ever replaces it. A failed request changes nothing.
 */
class WeatherRepository(
    private val settings: LauncherSettings,
    private val location: LocationProvider,
    private val client: OpenMeteoClient,
    scope: CoroutineScope,
) {

    /** Null while nothing has been fetched yet, or when the cache is unreadable. */
    val snapshot: StateFlow<WeatherSnapshot?> = settings.weatherCache
        .map { text -> text.takeIf { it.isNotBlank() }?.let(WeatherSnapshot::decode) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    // One request at a time: the worker and the home screen both ask, often at once.
    private val refreshLock = Mutex()

    /**
     * @param force ignores [MIN_INTERVAL_MS], for the pull the user triggered by hand.
     * @return true when a new reading was stored.
     */
    suspend fun refresh(force: Boolean = false): Boolean = refreshLock.withLock {
        if (!settings.weatherEnabled.first()) return false
        val unit = settings.temperatureUnit.first()
        val cached = snapshot.value

        val expired = cached == null ||
            System.currentTimeMillis() - cached.fetchedAtMillis > MIN_INTERVAL_MS ||
            cached.unit != unit
        if (!force && !expired) return false

        val position = location.current() ?: return false
        val fresh = withContext(Dispatchers.IO) {
            client.fetch(position.latitude, position.longitude, unit)
        } ?: return false

        settings.setWeatherCache(fresh.encode())
        return true
    }

    companion object {
        /** Open-Meteo updates hourly; asking more often only costs battery. */
        const val MIN_INTERVAL_MS = 30 * 60 * 1000L
    }
}
