package de.moritzstaat.launcher.data.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherParserTest {

    private val body = """
        {
          "latitude": 52.52,
          "longitude": 13.42,
          "current": {
            "time": "2026-09-01T14:00",
            "temperature_2m": 18.4,
            "weather_code": 3,
            "is_day": 1
          },
          "daily": {
            "time": ["2026-09-01"],
            "temperature_2m_max": [21.7],
            "temperature_2m_min": [11.2]
          }
        }
    """.trimIndent()

    @Test
    fun `a full response becomes a snapshot`() {
        val snapshot = WeatherParser.parse(body, TemperatureUnit.Celsius, 1000L, 52.52, 13.42)

        requireNotNull(snapshot)
        assertEquals(18.4, snapshot.temperature, 0.001)
        assertEquals(3, snapshot.code)
        assertTrue(snapshot.isDay)
        assertEquals(21.7, snapshot.high, 0.001)
        assertEquals(11.2, snapshot.low, 0.001)
        assertEquals(1000L, snapshot.fetchedAtMillis)
    }

    @Test
    fun `degrees are rounded for display`() {
        val snapshot = WeatherParser.parse(body, TemperatureUnit.Celsius, 0L, 52.52, 13.42)

        requireNotNull(snapshot)
        assertEquals("18°C", snapshot.temperatureLabel)
        assertEquals("22°C / 11°C", snapshot.highLowLabel)
    }

    @Test
    fun `night is recognised`() {
        val night = body.replace("\"is_day\": 1", "\"is_day\": 0")

        val snapshot = WeatherParser.parse(night, TemperatureUnit.Celsius, 0L, 0.0, 0.0)

        requireNotNull(snapshot)
        assertEquals(false, snapshot.isDay)
    }

    @Test
    fun `a missing daily block falls back to the current temperature`() {
        val withoutDaily = """{"current":{"temperature_2m":9.0,"weather_code":61,"is_day":1}}"""

        val snapshot = WeatherParser.parse(withoutDaily, TemperatureUnit.Celsius, 0L, 0.0, 0.0)

        requireNotNull(snapshot)
        assertEquals(9.0, snapshot.high, 0.001)
        assertEquals(9.0, snapshot.low, 0.001)
    }

    @Test
    fun `garbage and error responses give null`() {
        assertNull(WeatherParser.parse("", TemperatureUnit.Celsius, 0L, 0.0, 0.0))
        assertNull(WeatherParser.parse("<html>", TemperatureUnit.Celsius, 0L, 0.0, 0.0))
        assertNull(
            WeatherParser.parse(
                """{"error":true,"reason":"Latitude must be in range"}""",
                TemperatureUnit.Celsius,
                0L,
                0.0,
                0.0,
            ),
        )
    }

    @Test
    fun `a snapshot survives the cache round trip`() {
        val snapshot = WeatherParser.parse(body, TemperatureUnit.Fahrenheit, 42L, 52.52, 13.42)

        requireNotNull(snapshot)
        assertEquals(snapshot, WeatherSnapshot.decode(snapshot.encode()))
    }

    @Test
    fun `an unreadable cache gives null instead of zeroes`() {
        assertNull(WeatherSnapshot.decode("{}"))
        assertNull(WeatherSnapshot.decode("nonsense"))
    }

    @Test
    fun `the request url uses dots, not commas, for the coordinates`() {
        val url = OpenMeteoClient().buildUrl(52.52, 13.42, TemperatureUnit.Celsius)

        assertTrue(url, url.contains("latitude=52.5200"))
        assertTrue(url, url.contains("longitude=13.4200"))
        assertTrue(url, url.contains("temperature_unit=celsius"))
    }
}
