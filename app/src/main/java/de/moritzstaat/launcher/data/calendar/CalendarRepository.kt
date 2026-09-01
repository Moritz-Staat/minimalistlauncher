package de.moritzstaat.launcher.data.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * The next occurrences from the system calendar.
 *
 * `Instances` rather than `Events`, because a weekly meeting is one event but the home screen
 * has to show the next occurrence of it. Without the permission the flow simply stays empty:
 * every caller already has to handle "no events".
 */
class CalendarRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) {

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) = refresh()
    }

    private var observing = false

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CALENDAR,
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * Reads the calendar once and keeps watching it. Safe to call again: the launcher calls it
     * whenever it comes back to the front, because an event that has started needs a new line.
     */
    fun start() {
        if (!hasPermission()) return
        if (!observing) {
            context.contentResolver.registerContentObserver(
                CalendarContract.CONTENT_URI,
                true,
                observer,
            )
            observing = true
        }
        refresh()
    }

    fun refresh() {
        if (!hasPermission()) {
            _events.value = emptyList()
            return
        }
        scope.launch { _events.value = withContext(Dispatchers.IO) { query() } }
    }

    /** The calendars on the device, for the picker in the settings. */
    suspend fun calendars(): List<CalendarInfo> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        val calendars = mutableListOf<CalendarInfo>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                calendars += CalendarInfo(
                    id = cursor.getLong(0),
                    displayName = cursor.getString(1).orEmpty(),
                    accountName = cursor.getString(2).orEmpty(),
                    colorArgb = cursor.getInt(3),
                )
            }
        }
        calendars
    }

    private fun query(): List<CalendarEvent> {
        val now = System.currentTimeMillis()
        val until = now + EventFormatter.LOOKAHEAD.toMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
            // The window is part of the path, not the selection, for the Instances table.
            ContentUris.appendId(this, now - TimeUnit.HOURS.toMillis(RUNNING_GRACE_HOURS))
            ContentUris.appendId(this, until)
        }.build()

        val projection = arrayOf(
            CalendarContract.Instances._ID,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.CALENDAR_ID,
        )

        val events = mutableListOf<CalendarEvent>()
        runCatching {
            context.contentResolver.query(
                uri,
                projection,
                // A declined invitation is not something the user wants on their home screen.
                "${CalendarContract.Instances.SELF_ATTENDEE_STATUS} != " +
                    "${CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED}",
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    events += CalendarEvent(
                        id = cursor.getLong(0),
                        eventId = cursor.getLong(1),
                        title = cursor.getString(2)?.takeIf { it.isNotBlank() } ?: UNTITLED,
                        beginMillis = cursor.getLong(3),
                        endMillis = cursor.getLong(4),
                        allDay = cursor.getInt(5) != 0,
                        colorArgb = cursor.getInt(6),
                        calendarId = cursor.getLong(7),
                    )
                }
            }
        }
        return events
    }

    /** Opens the occurrence in whatever calendar app the user has. */
    fun open(event: CalendarEvent) {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId)
        val intent = Intent(Intent.ACTION_VIEW, uri)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.beginMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endMillis)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private companion object {
        const val UNTITLED = "(ohne Titel)"

        /** Events that started a while ago are still worth showing while they run. */
        const val RUNNING_GRACE_HOURS = 12L
    }
}
