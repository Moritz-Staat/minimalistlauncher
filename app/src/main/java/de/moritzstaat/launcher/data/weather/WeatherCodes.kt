package de.moritzstaat.launcher.data.weather

/**
 * WMO weather codes, which is what Open-Meteo reports, as a German word and a symbol.
 *
 * The codes come in families (5x drizzle, 6x rain, 7x snow, 8x showers, 9x thunderstorm), so
 * the ranges below are the table, not a guess.
 */
object WeatherCodes {

    fun description(code: Int): String = when (code) {
        0 -> "Klar"
        1 -> "Ueberwiegend klar"
        2 -> "Teils bewoelkt"
        3 -> "Bedeckt"
        45, 48 -> "Nebel"
        in 51..57 -> "Niesel"
        in 61..65 -> "Regen"
        66, 67 -> "Gefrierender Regen"
        in 71..77 -> "Schnee"
        in 80..82 -> "Schauer"
        85, 86 -> "Schneeschauer"
        95 -> "Gewitter"
        96, 99 -> "Gewitter mit Hagel"
        else -> "Unbekannt"
    }

    fun symbol(code: Int, isDay: Boolean): String = when (code) {
        0 -> if (isDay) "☀" else "☾"
        1, 2 -> if (isDay) "⛅" else "☁"
        3 -> "☁"
        45, 48 -> "🌫"
        in 51..57, in 61..65, in 80..82 -> "🌧"
        66, 67 -> "🌧"
        in 71..77, 85, 86 -> "❄"
        in 95..99 -> "⛈"
        else -> "•"
    }
}
