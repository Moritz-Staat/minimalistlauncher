package de.moritzstaat.launcher.data.usage

import android.content.Context
import de.moritzstaat.launcher.data.db.LauncherDatabase
import de.moritzstaat.launcher.data.settings.LauncherSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Counts app openings and answers the one question the launcher asks before starting an app:
 * has this one been opened often enough today to be worth a moment's thought?
 *
 * The configuration and today's counts are held as state, so the check at launch time is a map
 * lookup and not a database round trip — nothing may sit between the tap and the app.
 */
class UsageRepository(
    private val context: Context,
    private val database: LauncherDatabase,
    settings: LauncherSettings,
    private val scope: CoroutineScope,
) {

    val config: StateFlow<UsageBreakerConfig> =
        settings.usageBreaker.stateIn(scope, SharingStarted.Eagerly, UsageBreakerConfig())

    private val _ownCounts = MutableStateFlow<Map<String, Int>>(emptyMap())

    /** The launcher's own tally for today, keyed by package. */
    val ownCounts: StateFlow<Map<String, Int>> = _ownCounts.asStateFlow()

    private val _systemCounts = MutableStateFlow<Map<String, Int>>(emptyMap())

    /** The system's tally, empty while usage access is not granted. */
    val systemCounts: StateFlow<Map<String, Int>> = _systemCounts.asStateFlow()

    private val _usageAccess = MutableStateFlow(false)

    /** Whether the system's own counts are available; re-read on every [refresh]. */
    val usageAccessGranted: StateFlow<Boolean> = _usageAccess.asStateFlow()

    /**
     * Reads both tallies again. Called whenever the launcher comes to the front, which is also
     * what makes the counters roll over at midnight without a timer for it.
     */
    fun refresh() {
        scope.launch {
            val day = LocalDate.now().toEpochDay()
            val own = withContext(Dispatchers.IO) {
                database.appOpenDao().forDay(day).associate { it.packageName to it.count }
            }
            _ownCounts.value = own
            val system = withContext(Dispatchers.IO) { UsageAccess.openCountsToday(context) }
            _systemCounts.value = system
            _usageAccess.value = system.isNotEmpty()
        }
    }

    /** The better of the two numbers: the system knows about openings the launcher never saw. */
    fun opensToday(packageName: String): Int =
        _systemCounts.value[packageName] ?: _ownCounts.value[packageName] ?: 0

    fun shouldPause(packageName: String): Boolean =
        UsageBreaker.shouldPause(config.value, packageName, opensToday(packageName))

    /** Records one opening and drops the counts nobody will look at again. */
    fun registerOpen(packageName: String) {
        _ownCounts.update { counts -> counts + (packageName to (counts[packageName] ?: 0) + 1) }
        scope.launch {
            val day = LocalDate.now().toEpochDay()
            withContext(Dispatchers.IO) {
                database.appOpenDao().increment(packageName, day)
                database.appOpenDao().deleteBefore(day - KEEP_DAYS)
            }
        }
    }

    private companion object {
        /** A week is enough history for a counter that only ever reports today. */
        const val KEEP_DAYS = 7L
    }
}
