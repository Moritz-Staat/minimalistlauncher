package de.moritzstaat.launcher.data.settings

import de.moritzstaat.launcher.data.backup.JsonReader
import de.moritzstaat.launcher.data.backup.JsonValue
import de.moritzstaat.launcher.data.backup.JsonWriter
import de.moritzstaat.launcher.data.backup.asObject
import de.moritzstaat.launcher.data.backup.asString
import de.moritzstaat.launcher.data.backup.toJson

/** How the clock is drawn. */
enum class ClockStyle {
    Large,
    Narrow,
    TwoLine,
    Text,

    /** Digits painted as a block grid, centred, with the date above them. */
    DotMatrix,
}

/** Where the colours come from. */
enum class ColorMode {
    /** Material You, seeded by the wallpaper. */
    MaterialYou,

    /** A fixed accent the user picked. */
    Manual,

    /** Pure black behind everything, for the OLED panel. */
    ExtraDark,
}

/** 12 or 24 hours, or whatever the system is set to. */
enum class HourFormat { System, TwelveHour, TwentyFourHour }

/**
 * Everything that decides how the launcher looks. Exported as one JSON file and restored from
 * it, which is also what the presets write.
 */
data class ThemeConfig(
    val clockStyle: ClockStyle = ClockStyle.Large,
    val hourFormat: HourFormat = HourFormat.System,
    val showDate: Boolean = true,
    val colorMode: ColorMode = ColorMode.MaterialYou,
    val accentArgb: Int = DEFAULT_ACCENT,
    val darkTheme: Boolean = true,
    val wallpaperDim: Float = 0.15f,
    val wallpaperBlur: Float = 0f,
    val hideStatusBar: Boolean = false,
    val fontPath: String = "",
) {

    /** Extra dark is always dark, whatever the switch says. */
    val isDark: Boolean
        get() = colorMode == ColorMode.ExtraDark || darkTheme

    fun toJson(): JsonValue = JsonValue.Obj(
        linkedMapOf(
            KEY_CLOCK_STYLE to clockStyle.name.toJson(),
            KEY_HOUR_FORMAT to hourFormat.name.toJson(),
            KEY_SHOW_DATE to showDate.toString().toJson(),
            KEY_COLOR_MODE to colorMode.name.toJson(),
            KEY_ACCENT to accentArgb.toString().toJson(),
            KEY_DARK to darkTheme.toString().toJson(),
            KEY_DIM to wallpaperDim.toString().toJson(),
            KEY_BLUR to wallpaperBlur.toString().toJson(),
            KEY_HIDE_STATUS_BAR to hideStatusBar.toString().toJson(),
            KEY_FONT_PATH to fontPath.toJson(),
        ),
    )

    fun encode(): String = JsonWriter.write(toJson())

    companion object {
        const val DEFAULT_ACCENT = 0xFF8AB4F8.toInt()

        const val KEY_CLOCK_STYLE = "clockStyle"
        const val KEY_HOUR_FORMAT = "hourFormat"
        const val KEY_SHOW_DATE = "showDate"
        const val KEY_COLOR_MODE = "colorMode"
        const val KEY_ACCENT = "accentArgb"
        const val KEY_DARK = "darkTheme"
        const val KEY_DIM = "wallpaperDim"
        const val KEY_BLUR = "wallpaperBlur"
        const val KEY_HIDE_STATUS_BAR = "hideStatusBar"
        const val KEY_FONT_PATH = "fontPath"

        /** Unknown or missing fields fall back to the default, never to an exception. */
        fun fromJson(value: JsonValue?): ThemeConfig {
            val entries = value?.asObject() ?: return ThemeConfig()
            val defaults = ThemeConfig()
            return ThemeConfig(
                clockStyle = entries[KEY_CLOCK_STYLE]?.asString()
                    ?.let { name -> ClockStyle.entries.firstOrNull { it.name == name } }
                    ?: defaults.clockStyle,
                hourFormat = entries[KEY_HOUR_FORMAT]?.asString()
                    ?.let { name -> HourFormat.entries.firstOrNull { it.name == name } }
                    ?: defaults.hourFormat,
                showDate = entries[KEY_SHOW_DATE]?.asString()?.toBooleanStrictOrNull()
                    ?: defaults.showDate,
                colorMode = entries[KEY_COLOR_MODE]?.asString()
                    ?.let { name -> ColorMode.entries.firstOrNull { it.name == name } }
                    ?: defaults.colorMode,
                accentArgb = entries[KEY_ACCENT]?.asString()?.toIntOrNull() ?: defaults.accentArgb,
                darkTheme = entries[KEY_DARK]?.asString()?.toBooleanStrictOrNull()
                    ?: defaults.darkTheme,
                wallpaperDim = entries[KEY_DIM]?.asString()?.toFloatOrNull()?.coerceIn(0f, 1f)
                    ?: defaults.wallpaperDim,
                wallpaperBlur = entries[KEY_BLUR]?.asString()?.toFloatOrNull()?.coerceIn(0f, 1f)
                    ?: defaults.wallpaperBlur,
                hideStatusBar = entries[KEY_HIDE_STATUS_BAR]?.asString()?.toBooleanStrictOrNull()
                    ?: defaults.hideStatusBar,
                fontPath = entries[KEY_FONT_PATH]?.asString() ?: defaults.fontPath,
            )
        }

        fun decode(text: String): ThemeConfig = fromJson(JsonReader.read(text))
    }
}

/** The four presets offered in the settings. */
enum class ThemePreset(val label: String, val config: ThemeConfig) {
    LightDefault(
        label = "Standard hell",
        config = ThemeConfig(darkTheme = false, colorMode = ColorMode.MaterialYou, wallpaperDim = 0.1f),
    ),
    DarkDefault(
        label = "Standard dunkel",
        config = ThemeConfig(darkTheme = true, colorMode = ColorMode.MaterialYou, wallpaperDim = 0.2f),
    ),
    MinimalMono(
        label = "Minimal Mono",
        config = ThemeConfig(
            clockStyle = ClockStyle.Narrow,
            darkTheme = true,
            colorMode = ColorMode.Manual,
            accentArgb = 0xFFE0E0E0.toInt(),
            wallpaperDim = 0.45f,
            wallpaperBlur = 0.4f,
            showDate = false,
        ),
    ),
    Nothing(
        label = "Nothing",
        config = ThemeConfig(
            clockStyle = ClockStyle.TwoLine,
            darkTheme = true,
            colorMode = ColorMode.ExtraDark,
            accentArgb = 0xFFD71921.toInt(),
            wallpaperDim = 0.6f,
            hideStatusBar = false,
        ),
    ),
}
