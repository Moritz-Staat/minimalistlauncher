package de.moritzstaat.launcher.data.usage

/**
 * How often an app may be opened before the launcher asks whether that was on purpose.
 *
 * [packages] holds package names rather than AppKeys: what is being counted is the app, not
 * one of its launcher entries, and a second profile of the same app is the same habit.
 */
data class UsageBreakerConfig(
    val enabled: Boolean = false,
    val packages: Set<String> = emptySet(),
    val threshold: Int = DEFAULT_THRESHOLD,
    val pauseSeconds: Int = DEFAULT_PAUSE_SECONDS,
) {
    companion object {
        const val DEFAULT_THRESHOLD = 10
        const val DEFAULT_PAUSE_SECONDS = 5

        val THRESHOLD_RANGE = 1..50
        val PAUSE_RANGE = 0..30
    }
}

/**
 * The rule itself, kept free of Android so it can be checked without a device.
 *
 * The pause is a question, never a block: the launcher counts, shows the number and waits a
 * few seconds. Whether the app opens is still the user's decision.
 */
object UsageBreaker {

    /**
     * @param opensToday how often the app has been opened today, however that was counted.
     * @return true when the pause screen should come up instead of the app.
     */
    fun shouldPause(config: UsageBreakerConfig, packageName: String, opensToday: Int): Boolean {
        if (!config.enabled) return false
        if (packageName !in config.packages) return false
        return opensToday >= config.threshold
    }

    /** Clamps what the settings may store, so a bad value cannot lock the user out of an app. */
    fun sanitise(config: UsageBreakerConfig): UsageBreakerConfig = config.copy(
        threshold = config.threshold.coerceIn(UsageBreakerConfig.THRESHOLD_RANGE),
        pauseSeconds = config.pauseSeconds.coerceIn(UsageBreakerConfig.PAUSE_RANGE),
    )
}
