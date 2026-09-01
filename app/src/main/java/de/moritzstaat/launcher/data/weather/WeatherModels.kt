package de.moritzstaat.launcher.data.weather

import de.moritzstaat.launcher.data.backup.JsonReader
import de.moritzstaat.launcher.data.backup.JsonValue
import de.moritzstaat.launcher.data.backup.JsonWriter
import de.moritzstaat.launcher.data.backup.asObject
import de.moritzstaat.launcher.data.backup.asString
import de.moritzstaat.launcher.data.backup.toJson
import kotlin.math.roundToInt

/** What Open-Meteo is asked to deliver, and what the degrees are labelled with. */
enum class TemperatureUnit(val apiValue: String, val suffix: String) {
    Celsius("celsius", "°C"),
    Fahrenheit("fahrenheit", "°F"),
}

/**
 * The current weather, as far as the home screen cares: one temperature, one condition, and
 * today's range.
 *
 * Stored as JSON so the last reading survives a restart; a launcher that shows nothing until
 * the network answers is worse than one showing a reading from an hour ago.
 */
data class WeatherSnapshot(
    val temperature: Double,
    val code: Int,
    val isDay: Boolean,
    val high: Double,
    val low: Double,
    val unit: TemperatureUnit,
    val fetchedAtMillis: Long,
    val latitude: Double,
    val longitude: Double,
) {

    /** e.g. "12°C". Degrees with a decimal are noise at this size. */
    val temperatureLabel: String get() = degrees(temperature)

    val highLowLabel: String get() = degrees(high) + " / " + degrees(low)

    val description: String get() = WeatherCodes.description(code)

    val symbol: String get() = WeatherCodes.symbol(code, isDay)

    private fun degrees(value: Double): String = value.roundToInt().toString() + unit.suffix

    fun toJson(): JsonValue = JsonValue.Obj(
        linkedMapOf(
            KEY_TEMPERATURE to temperature.toString().toJson(),
            KEY_CODE to code.toString().toJson(),
            KEY_IS_DAY to isDay.toString().toJson(),
            KEY_HIGH to high.toString().toJson(),
            KEY_LOW to low.toString().toJson(),
            KEY_UNIT to unit.name.toJson(),
            KEY_FETCHED_AT to fetchedAtMillis.toString().toJson(),
            KEY_LATITUDE to latitude.toString().toJson(),
            KEY_LONGITUDE to longitude.toString().toJson(),
        ),
    )

    fun encode(): String = JsonWriter.write(toJson())

    companion object {
        const val KEY_TEMPERATURE = "temperature"
        const val KEY_CODE = "code"
        const val KEY_IS_DAY = "isDay"
        const val KEY_HIGH = "high"
        const val KEY_LOW = "low"
        const val KEY_UNIT = "unit"
        const val KEY_FETCHED_AT = "fetchedAt"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"

        /** A half written cache file gives null, never a snapshot full of zeroes. */
        fun decode(text: String): WeatherSnapshot? {
            val entries = JsonReader.read(text)?.asObject() ?: return null
            return WeatherSnapshot(
                temperature = entries[KEY_TEMPERATURE]?.asString()?.toDoubleOrNull()
                    ?: return null,
                code = entries[KEY_CODE]?.asString()?.toIntOrNull() ?: return null,
                isDay = entries[KEY_IS_DAY]?.asString()?.toBooleanStrictOrNull() ?: true,
                high = entries[KEY_HIGH]?.asString()?.toDoubleOrNull() ?: return null,
                low = entries[KEY_LOW]?.asString()?.toDoubleOrNull() ?: return null,
                unit = entries[KEY_UNIT]?.asString()
                    ?.let { name -> TemperatureUnit.entries.firstOrNull { it.name == name } }
                    ?: TemperatureUnit.Celsius,
                fetchedAtMillis = entries[KEY_FETCHED_AT]?.asString()?.toLongOrNull() ?: 0L,
                latitude = entries[KEY_LATITUDE]?.asString()?.toDoubleOrNull() ?: 0.0,
                longitude = entries[KEY_LONGITUDE]?.asString()?.toDoubleOrNull() ?: 0.0,
            )
        }
    }
}
