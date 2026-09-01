package de.moritzstaat.launcher.data.calendar

/** One calendar the device knows about, as offered in the settings. */
data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val colorArgb: Int,
)

/**
 * One occurrence of an event, not the event itself: a weekly meeting shows up once per week.
 *
 * [beginMillis] and [endMillis] are what `CalendarContract.Instances` returns. For an all day
 * event those are midnight UTC, not local midnight, which is why [allDay] has to travel with
 * them.
 */
data class CalendarEvent(
    val id: Long,
    val eventId: Long,
    val title: String,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val colorArgb: Int,
    val calendarId: Long,
)
