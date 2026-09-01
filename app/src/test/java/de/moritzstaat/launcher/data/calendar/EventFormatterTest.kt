package de.moritzstaat.launcher.data.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class EventFormatterTest {

    private val zone = ZoneId.of("Europe/Berlin")
    private val now: ZonedDateTime = LocalDateTime.of(2026, 9, 1, 14, 0).atZone(zone)

    private fun event(
        begin: ZonedDateTime,
        end: ZonedDateTime = begin.plusHours(1),
        allDay: Boolean = false,
    ) = CalendarEvent(
        id = 1,
        eventId = 1,
        title = "Termin",
        beginMillis = begin.toInstant().toEpochMilli(),
        endMillis = end.toInstant().toEpochMilli(),
        allDay = allDay,
        colorArgb = 0,
        calendarId = 1,
    )

    @Test
    fun `an event within the hour counts down in minutes`() {
        assertEquals("in 20 Min.", EventFormatter.label(event(now.plusMinutes(20)), now))
        assertEquals("in 59 Min.", EventFormatter.label(event(now.plusMinutes(59)), now))
    }

    @Test
    fun `an event starting right now still reads as a minute away`() {
        assertEquals("in 1 Min.", EventFormatter.label(event(now.plusSeconds(10)), now))
    }

    @Test
    fun `a running event shows when it ends`() {
        val running = event(now.minusMinutes(10), now.plusMinutes(50))

        assertTrue(EventFormatter.isRunning(running, now))
        assertEquals("Jetzt bis 14:50", EventFormatter.label(running, now))
    }

    @Test
    fun `later today is just the time`() {
        assertEquals("18:30", EventFormatter.label(event(now.plusHours(4).plusMinutes(30)), now))
    }

    @Test
    fun `tomorrow is named`() {
        val tomorrow = now.plusDays(1).withHour(9).withMinute(0)

        assertEquals("Morgen 09:00", EventFormatter.label(event(tomorrow), now))
    }

    @Test
    fun `further out uses the weekday`() {
        val later = now.plusDays(2).withHour(9).withMinute(0)

        assertEquals("Do. 09:00", EventFormatter.label(event(later), now))
    }

    @Test
    fun `all day events are read in utc, not in the local zone`() {
        // Instances reports midnight UTC for an all day event; in Berlin that is 02:00 local,
        // and reading it locally would still land on the right day - so check the day before,
        // where a naive local reading would say "morgen" instead of "ganztägig".
        val utcMidnight = LocalDateTime.of(2026, 9, 1, 0, 0).atZone(ZoneOffset.UTC)
        val allDay = event(utcMidnight, utcMidnight.plusDays(1), allDay = true)

        assertEquals("Ganztägig", EventFormatter.label(allDay, now))
        assertEquals(0L, EventFormatter.daysUntil(allDay, now))
    }

    @Test
    fun `an all day event tomorrow says so`() {
        val utcMidnight = LocalDateTime.of(2026, 9, 2, 0, 0).atZone(ZoneOffset.UTC)
        val allDay = event(utcMidnight, utcMidnight.plusDays(1), allDay = true)

        assertEquals("Morgen, ganztägig", EventFormatter.label(allDay, now))
    }

    @Test
    fun `finished events are not upcoming and far away ones are out of the window`() {
        val past = event(now.minusHours(3), now.minusHours(2))
        val running = event(now.minusMinutes(5), now.plusMinutes(5))
        val far = event(now.plusDays(3))

        assertFalse(EventFormatter.isUpcoming(past, now))
        assertTrue(EventFormatter.isUpcoming(running, now))
        assertFalse(EventFormatter.isUpcoming(far, now))
    }
}
