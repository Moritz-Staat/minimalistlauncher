package de.moritzstaat.launcher.ui.home

/**
 * German word clock, the way a person would say the time out loud.
 *
 * Rounds to the nearest five minutes and switches to the coming hour from twenty five past,
 * which is the common reading in Germany.
 */
object TextClockFormatter {

    private val HOURS = listOf(
        "zwölf", "eins", "zwei", "drei", "vier", "fünf",
        "sechs", "sieben", "acht", "neun", "zehn", "elf",
    )

    /**
     * @param hour 0..23
     * @param minute 0..59
     */
    fun format(hour: Int, minute: Int): String {
        val slot = ((minute + 2) / 5) % 12
        // Rounding 58 minutes upwards also moves the hour on.
        val hourShift = if (minute >= 58) 1 else 0
        val current = normalise(hour + hourShift)
        val next = normalise(current + 1)

        return when (slot) {
            0 -> "${hourWord(current, withUhr = true)} Uhr"
            1 -> "fünf nach ${hourWord(current)}"
            2 -> "zehn nach ${hourWord(current)}"
            3 -> "viertel nach ${hourWord(current)}"
            4 -> "zwanzig nach ${hourWord(current)}"
            5 -> "fünf vor halb ${hourWord(next)}"
            6 -> "halb ${hourWord(next)}"
            7 -> "fünf nach halb ${hourWord(next)}"
            8 -> "zwanzig vor ${hourWord(next)}"
            9 -> "viertel vor ${hourWord(next)}"
            10 -> "zehn vor ${hourWord(next)}"
            else -> "fünf vor ${hourWord(next)}"
        }
    }

    private fun normalise(hour: Int): Int = ((hour % 24) + 24) % 24

    /** "ein Uhr", but "fünf nach eins": the counting form differs from the o'clock form. */
    private fun hourWord(hour24: Int, withUhr: Boolean = false): String {
        val index = hour24 % 12
        if (index == 1 && withUhr) return "ein"
        return HOURS[index]
    }
}
