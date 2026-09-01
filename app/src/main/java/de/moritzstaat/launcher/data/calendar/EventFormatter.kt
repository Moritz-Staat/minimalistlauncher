package de.moritzstaat.launcher.data.calendar

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Turns an event into the one short line the home screen has room for.
 *
 * Everything is derived from an explicit "now", so the rules are unit testable: a formatter
 * that reads the system clock can only be checked by waiting.
 */
object EventFormatter {

    private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY)
    private val WEEKDAY = DateTimeFormatter.ofPattern("EEE", Locale.GERMANY)

    /** How far ahead the home screen looks. Anything later is not "coming up" any more. */
    val LOOKAHEAD: Duration = Duration.ofHours(36)

    fun label(event: CalendarEvent, now: ZonedDateTime): String {
        val zone = now.zone
        val startsInDays = daysUntil(event, now)

        if (event.allDay) {
            return when (startsInDays) {
                0L -> "Ganztaegig"
                1L -> "Morgen, ganztaegig"
                else -> weekdayOf(event, zone) + ", ganztaegig"
            }
        }

        val begin = Instant.ofEpochMilli(event.beginMillis).atZone(zone)
        val end = Instant.ofEpochMilli(event.endMillis).atZone(zone)
        if (isRunning(event, now)) return "Jetzt bis " + end.format(TIME)

        val minutes = Duration.between(now, begin).toMinutes()
        return when {
            minutes < MINUTES_PER_HOUR -> "in " + maxOf(minutes, 1L) + " Min."
            startsInDays == 0L -> begin.format(TIME)
            startsInDays == 1L -> "Morgen " + begin.format(TIME)
            else -> begin.format(WEEKDAY) + " " + begin.format(TIME)
        }
    }

    fun isRunning(event: CalendarEvent, now: ZonedDateTime): Boolean {
        val millis = now.toInstant().toEpochMilli()
        return millis in event.beginMillis until event.endMillis
    }

    /**
     * Whether the occurrence is still worth showing. A meeting that ended is gone even though
     * the query window still covers it.
     */
    fun isUpcoming(event: CalendarEvent, now: ZonedDateTime): Boolean {
        val millis = now.toInstant().toEpochMilli()
        return event.endMillis > millis && event.beginMillis < millis + LOOKAHEAD.toMillis()
    }

    /**
     * Calendar days between today and the day the event starts on.
     *
     * An all day event carries midnight UTC rather than local midnight, so its date has to be
     * read in UTC — otherwise "today" turns into "yesterday" west of Greenwich.
     */
    fun daysUntil(event: CalendarEvent, now: ZonedDateTime): Long {
        val zone = if (event.allDay) ZoneOffset.UTC else now.zone
        val start = Instant.ofEpochMilli(event.beginMillis).atZone(zone).toLocalDate()
        val today: LocalDate = now.toLocalDate()
        return start.toEpochDay() - today.toEpochDay()
    }

    private fun weekdayOf(event: CalendarEvent, zone: ZoneId): String {
        val calendarZone = if (event.allDay) ZoneOffset.UTC else zone
        return Instant.ofEpochMilli(event.beginMillis).atZone(calendarZone).format(WEEKDAY)
    }

    private const val MINUTES_PER_HOUR = 60L
}
