package de.moritzstaat.launcher.data.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherCodesTest {

    @Test
    fun `the families map to their german word`() {
        assertEquals("Klar", WeatherCodes.description(0))
        assertEquals("Bedeckt", WeatherCodes.description(3))
        assertEquals("Nebel", WeatherCodes.description(48))
        assertEquals("Niesel", WeatherCodes.description(53))
        assertEquals("Regen", WeatherCodes.description(63))
        assertEquals("Schnee", WeatherCodes.description(73))
        assertEquals("Schauer", WeatherCodes.description(81))
        assertEquals("Gewitter", WeatherCodes.description(95))
    }

    @Test
    fun `an unknown code is labelled, not crashed on`() {
        assertEquals("Unbekannt", WeatherCodes.description(-1))
        assertEquals("Unbekannt", WeatherCodes.description(1234))
    }

    @Test
    fun `every code has a symbol`() {
        (0..99).forEach { code ->
            assertTrue("code $code", WeatherCodes.symbol(code, isDay = true).isNotBlank())
        }
    }

    @Test
    fun `a clear sky looks different by night`() {
        assertNotEquals(
            WeatherCodes.symbol(0, isDay = true),
            WeatherCodes.symbol(0, isDay = false),
        )
    }
}
