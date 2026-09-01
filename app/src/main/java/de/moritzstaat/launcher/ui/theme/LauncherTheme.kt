package de.moritzstaat.launcher.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import de.moritzstaat.launcher.data.settings.ColorMode
import de.moritzstaat.launcher.data.settings.ThemeConfig

/**
 * The active theme, readable anywhere in the tree. The clock and the wallpaper scrim need it
 * without every composable in between having to pass it down.
 */
val LocalThemeConfig = staticCompositionLocalOf { ThemeConfig() }

/**
 * Builds the Material scheme from [config] and publishes the config itself alongside it.
 *
 * Material You reads the wallpaper colours from the system, the manual mode derives its shades
 * from one accent, and extra dark is the manual mode with a black ground for the OLED panel.
 */
@Composable
fun LauncherTheme(
    config: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = remember(config.colorMode, config.accentArgb, config.darkTheme) {
        when (config.colorMode) {
            ColorMode.MaterialYou -> if (config.isDark) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }

            ColorMode.Manual -> accentScheme(config.accentArgb, dark = config.darkTheme)
            ColorMode.ExtraDark -> extraDarkScheme(config.accentArgb)
        }
    }
    val typography = rememberLauncherTypography(config.fontPath)

    CompositionLocalProvider(LocalThemeConfig provides config) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

/** Manual accent: the roles are shades of the one colour the user picked. */
internal fun accentScheme(accent: Int, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val secondary = AccentPalette.desaturate(accent, SECONDARY_DESATURATION)
    val tertiary = AccentPalette.hueShift(accent, TERTIARY_HUE_SHIFT)

    return base.copy(
        primary = shade(accent, if (dark) TONE_LIGHT else TONE_DARK),
        onPrimary = shade(accent, if (dark) TONE_DARKEST else TONE_LIGHTEST),
        primaryContainer = shade(accent, if (dark) TONE_CONTAINER_DARK else TONE_CONTAINER_LIGHT),
        onPrimaryContainer = shade(accent, if (dark) TONE_LIGHTEST else TONE_DARKEST),
        secondary = shade(secondary, if (dark) TONE_LIGHT else TONE_DARK),
        onSecondary = shade(secondary, if (dark) TONE_DARKEST else TONE_LIGHTEST),
        secondaryContainer = shade(secondary, if (dark) TONE_CONTAINER_DARK else TONE_CONTAINER_LIGHT),
        onSecondaryContainer = shade(secondary, if (dark) TONE_LIGHTEST else TONE_DARKEST),
        tertiary = shade(tertiary, if (dark) TONE_LIGHT else TONE_DARK),
        onTertiary = shade(tertiary, if (dark) TONE_DARKEST else TONE_LIGHTEST),
        tertiaryContainer = shade(tertiary, if (dark) TONE_CONTAINER_DARK else TONE_CONTAINER_LIGHT),
        onTertiaryContainer = shade(tertiary, if (dark) TONE_LIGHTEST else TONE_DARKEST),
    )
}

/**
 * Every surface is pure black, so the panel simply keeps those pixels switched off. Only the
 * accent roles carry colour.
 */
internal fun extraDarkScheme(accent: Int): ColorScheme {
    val black = Color.Black
    return accentScheme(accent, dark = true).copy(
        background = black,
        surface = black,
        surfaceDim = black,
        surfaceBright = shade(accent, TONE_BLACK_LIFT),
        surfaceContainerLowest = black,
        surfaceContainerLow = black,
        surfaceContainer = black,
        surfaceContainerHigh = shade(accent, TONE_BLACK_LIFT),
        surfaceContainerHighest = shade(accent, TONE_BLACK_LIFT),
        surfaceVariant = shade(accent, TONE_BLACK_LIFT),
        onBackground = Color.White,
        onSurface = Color.White,
        outlineVariant = shade(accent, TONE_BLACK_LIFT),
    )
}

private fun shade(argb: Int, lightness: Float): Color = Color(AccentPalette.tone(argb, lightness))

private const val SECONDARY_DESATURATION = 0.55f
private const val TERTIARY_HUE_SHIFT = 60f

private const val TONE_LIGHTEST = 0.92f
private const val TONE_LIGHT = 0.72f
private const val TONE_CONTAINER_DARK = 0.28f
private const val TONE_CONTAINER_LIGHT = 0.86f
private const val TONE_DARK = 0.36f
private const val TONE_DARKEST = 0.12f
private const val TONE_BLACK_LIFT = 0.10f
