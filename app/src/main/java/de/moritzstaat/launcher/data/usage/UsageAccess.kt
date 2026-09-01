package de.moritzstaat.launcher.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import java.time.LocalDate
import java.time.ZoneId

/**
 * The system's own count of how often an app was brought to the front today.
 *
 * The launcher only sees the launches that went through it; an app opened from a notification
 * or from recents would never be counted. Usage access closes that gap, and it is optional:
 * without it the launcher falls back to its own tally.
 */
object UsageAccess {

    fun settingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * How often each package came to the front since local midnight. Empty when usage access
     * is not granted — the query then simply reports nothing.
     *
     * `ACTIVITY_RESUMED` fires again when the user comes back from another app, so consecutive
     * events of the same package are folded into one: switching back and forth inside one app
     * is one opening, not five.
     */
    fun openCountsToday(context: Context): Map<String, Int> {
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()

        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val counts = mutableMapOf<String, Int>()

        runCatching {
            val events = manager.queryEvents(start, System.currentTimeMillis())
            val event = UsageEvents.Event()
            var previous: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType != UsageEvents.Event.ACTIVITY_RESUMED) continue
                val packageName = event.packageName ?: continue
                if (packageName != previous) {
                    counts[packageName] = (counts[packageName] ?: 0) + 1
                    previous = packageName
                }
            }
        }
        return counts
    }

    /**
     * Whether the access appears to be granted.
     *
     * Every `AppOpsManager` check for it is deprecated on the current SDK, so the data itself
     * is the answer: without the access the query returns nothing, and with it there is always
     * at least the launcher's own resume from this very session.
     */
    fun isGranted(context: Context): Boolean = openCountsToday(context).isNotEmpty()
}
