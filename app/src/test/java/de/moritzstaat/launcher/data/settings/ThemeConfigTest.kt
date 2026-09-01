package de.moritzstaat.launcher.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeConfigTest {

    @Test
    fun `a theme survives encoding and decoding`() {
        val theme = ThemeConfig(
            clockStyle = ClockStyle.TwoLine,
            hourFormat = HourFormat.TwelveHour,
            showDate = false,
            colorMode = ColorMode.ExtraDark,
            accentArgb = 0xFFD71921.toInt(),
            darkTheme = true,
            wallpaperDim = 0.42f,
            wallpaperBlur = 0.25f,
            hideStatusBar = true,
            fontPath = "/data/user/0/de.moritzstaat.launcher/files/fonts/custom-font-1",
        )

        assertEquals(theme, ThemeConfig.decode(theme.encode()))
    }

    @Test
    fun `an empty file gives the defaults`() {
        assertEquals(ThemeConfig(), ThemeConfig.decode("{}"))
        assertEquals(ThemeConfig(), ThemeConfig.decode(""))
    }

    @Test
    fun `unknown enum names fall back instead of throwing`() {
        val text = """{"clockStyle":"Circular","colorMode":"Neon","hourFormat":"Sundial"}"""

        val theme = ThemeConfig.decode(text)

        assertEquals(ThemeConfig().clockStyle, theme.clockStyle)
        assertEquals(ThemeConfig().colorMode, theme.colorMode)
        assertEquals(ThemeConfig().hourFormat, theme.hourFormat)
    }

    @Test
    fun `dim and blur are clamped to the slider range`() {
        val text = """{"wallpaperDim":"4.0","wallpaperBlur":"-3.0"}"""

        val theme = ThemeConfig.decode(text)

        assertEquals(1f, theme.wallpaperDim, 0.001f)
        assertEquals(0f, theme.wallpaperBlur, 0.001f)
    }

    @Test
    fun `every preset is exportable`() {
        ThemePreset.entries.forEach { preset ->
            assertEquals(preset.config, ThemeConfig.decode(preset.config.encode()))
        }
    }
}
