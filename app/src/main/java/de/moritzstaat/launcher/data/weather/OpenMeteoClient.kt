package de.moritzstaat.launcher.data.weather

import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * The one network call the launcher makes: the current weather from Open-Meteo.
 *
 * `HttpURLConnection` on purpose — the whole request is one URL and one JSON body, which is
 * not worth an HTTP library. Open-Meteo needs no API key and no account.
 */
class OpenMeteoClient {

    /** @return the reading, or null on any network or parsing trouble. */
    fun fetch(
        latitude: Double,
        longitude: Double,
        unit: TemperatureUnit,
        nowMillis: Long = System.currentTimeMillis(),
    ): WeatherSnapshot? {
        val url = buildUrl(latitude, longitude, unit)
        val connection = runCatching { URL(url).openConnection() as HttpURLConnection }
            .getOrNull() ?: return null

        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.requestMethod = "GET"
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            WeatherParser.parse(body, unit, nowMillis, latitude, longitude)
        } catch (_: Exception) {
            // No network, no DNS, a timeout, a broken body: all the same to the caller.
            null
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Coordinates are formatted with [Locale.US] on purpose: a German locale would write
     * "52,52" and the request would come back as an error.
     */
    fun buildUrl(latitude: Double, longitude: Double, unit: TemperatureUnit): String =
        String.format(
            Locale.US,
            "%s?latitude=%.4f&longitude=%.4f&current=%s&daily=%s" +
                "&forecast_days=1&timezone=auto&temperature_unit=%s",
            BASE_URL,
            latitude,
            longitude,
            CURRENT_FIELDS,
            DAILY_FIELDS,
            unit.apiValue,
        )

    private companion object {
        const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
        const val CURRENT_FIELDS = "temperature_2m,weather_code,is_day"
        const val DAILY_FIELDS = "temperature_2m_max,temperature_2m_min"
        const val TIMEOUT_MS = 10_000
    }
}
