package de.moritzstaat.launcher.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentPickerTest {

    private val darkBackground = 0xFF101014.toInt()
    private val lightBackground = 0xFFFFFFFF.toInt()
    private val fallback = 0xFF8AB4F8.toInt()

    @Test
    fun `white on black has the maximum contrast`() {
        assertEquals(21.0, AccentPicker.contrastRatio(0xFFFFFFFF.toInt(), 0xFF000000.toInt()), 0.05)
    }

    @Test
    fun `a colour has no contrast with itself`() {
        assertEquals(1.0, AccentPicker.contrastRatio(darkBackground, darkBackground), 1e-6)
    }

    @Test
    fun `the first readable candidate wins`() {
        val readable = 0xFFFFC107.toInt()
        val picked = AccentPicker.pick(listOf(readable, 0xFF00FF00.toInt()), darkBackground, fallback)
        assertEquals(readable, picked)
    }

    @Test
    fun `absent swatches are skipped`() {
        val readable = 0xFFFFC107.toInt()
        val picked = AccentPicker.pick(listOf(0, 0, readable), darkBackground, fallback)
        assertEquals(readable, picked)
    }

    @Test
    fun `an unreadable candidate is brightened instead of used as is`() {
        val tooDark = 0xFF14141A.toInt()
        val picked = AccentPicker.pick(listOf(tooDark), darkBackground, fallback)
        assertTrue(
            "picked colour must be readable",
            AccentPicker.contrastRatio(picked, darkBackground) >= AccentPicker.MIN_CONTRAST,
        )
    }

    @Test
    fun `on a light background the accent is darkened`() {
        val tooLight = 0xFFFAFAFA.toInt()
        val picked = AccentPicker.pick(listOf(tooLight), lightBackground, fallback)
        assertTrue(
            "picked colour must be readable",
            AccentPicker.contrastRatio(picked, lightBackground) >= AccentPicker.MIN_CONTRAST,
        )
    }

    @Test
    fun `without any candidate the fallback is used`() {
        assertEquals(fallback, AccentPicker.pick(emptyList(), darkBackground, fallback))
        assertEquals(fallback, AccentPicker.pick(listOf(0, 0), darkBackground, fallback))
    }
}
