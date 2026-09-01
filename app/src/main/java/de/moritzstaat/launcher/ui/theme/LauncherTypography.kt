package de.moritzstaat.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File

/**
 * The Material type scale, optionally re-lettered with a font file the user picked.
 *
 * A broken or deleted file falls back to the system font instead of crashing the launcher:
 * the home screen has to come up even when the font is gone.
 */
@Composable
fun rememberLauncherTypography(fontPath: String): Typography = remember(fontPath) {
    val family = loadFontFamily(fontPath) ?: return@remember DEFAULT_TYPOGRAPHY
    DEFAULT_TYPOGRAPHY.withFontFamily(family)
}

private fun loadFontFamily(fontPath: String): FontFamily? {
    if (fontPath.isBlank()) return null
    val file = File(fontPath)
    if (!file.isFile) return null
    return runCatching { FontFamily(Font(file)) }.getOrNull()
}

private fun Typography.withFontFamily(family: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

private val DEFAULT_TYPOGRAPHY = Typography()
