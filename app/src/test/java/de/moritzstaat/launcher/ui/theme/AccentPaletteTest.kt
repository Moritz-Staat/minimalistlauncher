package de.moritzstaat.launcher.ui.theme

import de.moritzstaat.launcher.data.media.AccentPicker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentPaletteTest {

    @Test
    fun `hsl round trip keeps the colour`() {
        listOf(0xFF8AB4F8.toInt(), 0xFFD71921.toInt(), 0xFF4CAF50.toInt(), 0xFF123456.toInt())
            .forEach { color ->
                val hsl = AccentPalette.toHsl(color)
                assertEquals(color, AccentPalette.fromHsl(hsl[0], hsl[1], hsl[2]))
            }
    }

    @Test
    fun `grey has no saturation and keeps its lightness`() {
        val hsl = AccentPalette.toHsl(0xFF808080.toInt())
        assertEquals(0f, hsl[1], 0.001f)
        assertEquals(0.502f, hsl[2], 0.005f)
    }

    @Test
    fun `tone sets the lightness and keeps the hue`() {
        val accent = 0xFF8AB4F8.toInt()
        val hue = AccentPalette.toHsl(accent)[0]

        val dark = AccentPalette.tone(accent, 0.2f)
        val light = AccentPalette.tone(accent, 0.9f)

        assertEquals(0.2f, AccentPalette.lightnessOf(dark), 0.01f)
        assertEquals(0.9f, AccentPalette.lightnessOf(light), 0.01f)
        assertEquals(hue, AccentPalette.toHsl(dark)[0], 1f)
        assertEquals(hue, AccentPalette.toHsl(light)[0], 1f)
    }

    @Test
    fun `tone keeps the alpha channel`() {
        val toned = AccentPalette.tone(0x80FF0000.toInt(), 0.4f)
        assertEquals(0x80, (toned ushr 24) and 0xFF)
    }

    @Test
    fun `desaturating fully leaves a grey of the same lightness`() {
        val accent = 0xFFD71921.toInt()
        val grey = AccentPalette.desaturate(accent, 1f)

        assertEquals(0f, AccentPalette.toHsl(grey)[1], 0.001f)
        assertEquals(AccentPalette.lightnessOf(accent), AccentPalette.lightnessOf(grey), 0.01f)
    }

    @Test
    fun `hue shift wraps around the circle`() {
        val accent = 0xFFD71921.toInt()
        val hue = AccentPalette.toHsl(accent)[0]

        val shifted = AccentPalette.hueShift(accent, 400f)

        assertEquals((hue + 40f) % 360f, AccentPalette.toHsl(shifted)[0], 1f)
    }

    @Test
    fun `content colour is the readable one of black and white`() {
        assertEquals(AccentPalette.BLACK, AccentPalette.contentColorFor(0xFFF2B705.toInt()))
        assertEquals(AccentPalette.WHITE, AccentPalette.contentColorFor(0xFF1A237E.toInt()))
    }

    @Test
    fun `the light and dark tones used by the scheme stay readable against each other`() {
        listOf(0xFF8AB4F8.toInt(), 0xFFD71921.toInt(), 0xFFF2B705.toInt(), 0xFFE0E0E0.toInt())
            .forEach { accent ->
                val primary = AccentPalette.tone(accent, 0.72f)
                val onPrimary = AccentPalette.tone(accent, 0.12f)
                val ratio = AccentPicker.contrastRatio(onPrimary, primary)
                assertTrue("$accent only reached $ratio", ratio >= 4.5)
            }
    }
}
