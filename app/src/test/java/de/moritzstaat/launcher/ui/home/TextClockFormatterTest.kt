package de.moritzstaat.launcher.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class TextClockFormatterTest {

    @Test
    fun `full hours read as o clock`() {
        assertEquals("drei Uhr", TextClockFormatter.format(15, 0))
        assertEquals("ein Uhr", TextClockFormatter.format(13, 0))
        assertEquals("zwölf Uhr", TextClockFormatter.format(0, 0))
    }

    @Test
    fun `the first half hour counts up from the current hour`() {
        assertEquals("fünf nach drei", TextClockFormatter.format(15, 5))
        assertEquals("zehn nach drei", TextClockFormatter.format(15, 10))
        assertEquals("viertel nach drei", TextClockFormatter.format(15, 15))
        assertEquals("zwanzig nach drei", TextClockFormatter.format(15, 20))
    }

    @Test
    fun `from twenty five past the coming hour is used`() {
        assertEquals("fünf vor halb vier", TextClockFormatter.format(15, 25))
        assertEquals("halb vier", TextClockFormatter.format(15, 30))
        assertEquals("fünf nach halb vier", TextClockFormatter.format(15, 35))
        assertEquals("zwanzig vor vier", TextClockFormatter.format(15, 40))
        assertEquals("viertel vor vier", TextClockFormatter.format(15, 45))
        assertEquals("zehn vor vier", TextClockFormatter.format(15, 50))
        assertEquals("fünf vor vier", TextClockFormatter.format(15, 55))
    }

    @Test
    fun `minutes round to the nearest five`() {
        assertEquals("drei Uhr", TextClockFormatter.format(15, 2))
        assertEquals("fünf nach drei", TextClockFormatter.format(15, 3))
        assertEquals("fünf nach drei", TextClockFormatter.format(15, 7))
        assertEquals("zehn nach drei", TextClockFormatter.format(15, 8))
    }

    @Test
    fun `just before the full hour rounds into the next hour`() {
        assertEquals("vier Uhr", TextClockFormatter.format(15, 58))
        assertEquals("vier Uhr", TextClockFormatter.format(15, 59))
    }

    @Test
    fun `midnight wraps correctly in both directions`() {
        assertEquals("zwölf Uhr", TextClockFormatter.format(23, 59))
        assertEquals("fünf vor zwölf", TextClockFormatter.format(23, 55))
        assertEquals("halb eins", TextClockFormatter.format(0, 30))
    }

    @Test
    fun `the twelve hour reading ignores morning and evening`() {
        assertEquals(TextClockFormatter.format(3, 20), TextClockFormatter.format(15, 20))
    }
}
