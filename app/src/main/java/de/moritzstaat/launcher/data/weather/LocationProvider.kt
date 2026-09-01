package de.moritzstaat.launcher.data.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Where the weather is asked for.
 *
 * Plain `LocationManager`, no Play Services: coarse accuracy is plenty for a temperature, and
 * a cached position from any provider beats waking the GPS for it.
 */
class LocationProvider(private val context: Context) {

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    /** @return a position, or null when the permission is missing or nothing answers in time. */
    suspend fun current(): Location? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null

        val cached = providers(manager)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
        if (cached != null && isFresh(cached)) return cached

        val fresh = providers(manager).firstNotNullOfOrNull { provider ->
            withTimeoutOrNull(REQUEST_TIMEOUT_MS) { requestSingle(manager, provider) }
        }
        return fresh ?: cached
    }

    private suspend fun requestSingle(
        manager: LocationManager,
        provider: String,
    ): Location? = suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }
        runCatching {
            manager.getCurrentLocation(provider, signal, context.mainExecutor) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
        }.onFailure {
            if (continuation.isActive) continuation.resume(null)
        }
    }

    /** Providers worth asking, in the order that costs the least battery. */
    private fun providers(manager: LocationManager): List<String> = listOf(
        LocationManager.FUSED_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
        LocationManager.GPS_PROVIDER,
    ).filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

    private fun isFresh(location: Location): Boolean =
        System.currentTimeMillis() - location.time < MAX_CACHE_AGE_MS

    private companion object {
        const val MAX_CACHE_AGE_MS = 2 * 60 * 60 * 1000L
        const val REQUEST_TIMEOUT_MS = 15_000L
    }
}
