package de.moritzstaat.launcher.data.weather

import de.moritzstaat.launcher.data.backup.JsonReader
import de.moritzstaat.launcher.data.backup.JsonValue
import de.moritzstaat.launcher.data.backup.asArray
import de.moritzstaat.launcher.data.backup.asObject
import de.moritzstaat.launcher.data.backup.asString

/**
 * Reads an Open-Meteo forecast response.
 *
 * Anything missing gives null instead of a snapshot with holes in it: the home screen then
 * keeps showing the last good reading rather than "0°C".
 */
object WeatherParser {

    fun parse(
        body: String,
        unit: TemperatureUnit,
        fetchedAtMillis: Long,
        latitude: Double,
        longitude: Double,
    ): WeatherSnapshot? {
        val root = JsonReader.read(body)?.asObject() ?: return null
        val current = root["current"]?.asObject() ?: return null
        val daily = root["daily"]?.asObject()

        val temperature = current["temperature_2m"]?.asString()?.toDoubleOrNull() ?: return null
        val code = current["weather_code"]?.asString()?.toDoubleOrNull()?.toInt() ?: return null
        val isDay = current["is_day"]?.asString()?.toDoubleOrNull()?.let { it > 0.5 } ?: true

        // The daily block is a one element array per field; without it the range falls back
        // to the current temperature, which is honest rather than wrong.
        val high = daily?.firstNumberOf("temperature_2m_max") ?: temperature
        val low = daily?.firstNumberOf("temperature_2m_min") ?: temperature

        return WeatherSnapshot(
            temperature = temperature,
            code = code,
            isDay = isDay,
            high = high,
            low = low,
            unit = unit,
            fetchedAtMillis = fetchedAtMillis,
            latitude = latitude,
            longitude = longitude,
        )
    }

    private fun Map<String, JsonValue>.firstNumberOf(key: String): Double? =
        get(key)?.asArray()?.firstOrNull()?.asString()?.toDoubleOrNull()
}
