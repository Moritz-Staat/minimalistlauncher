package de.moritzstaat.launcher.data.usage

/** How often one app was opened over the whole window, and when that last happened. */
data class OpenTally(
    val packageName: String,
    val opens: Int,
    val lastDayEpoch: Long,
)

/**
 * Which apps go above the alphabet.
 *
 * The point of the block is that the list gets shorter to read, not longer: a handful of names
 * at the top and the alphabet untouched below. Everything here is arithmetic on tallies so the
 * rules can be argued about in tests rather than on a device.
 */
object FrequentApps {

    /** Days of history the ranking looks at; the counters are kept exactly this long. */
    const val WINDOW_DAYS = 7L

    /** How many names the block shows at most. More than this and it is a second app list. */
    const val LIMIT = 4

    /**
     * Opens needed before an app is promoted.
     *
     * One tap is an accident - opening something once from the alphabet must not put it on top
     * for the rest of the week.
     */
    const val MIN_OPENS = 2

    /**
     * @return package names, most used first, at most [limit] of them.
     *
     * Ties break towards the app used more recently, and then by name, so the order never
     * wobbles between two equal candidates.
     */
    fun rank(
        tallies: List<OpenTally>,
        limit: Int = LIMIT,
        minOpens: Int = MIN_OPENS,
    ): List<String> = tallies
        .filter { it.opens >= minOpens }
        .sortedWith(
            compareByDescending<OpenTally> { it.opens }
                .thenByDescending { it.lastDayEpoch }
                .thenBy { it.packageName },
        )
        .take(limit)
        .map { it.packageName }
}
