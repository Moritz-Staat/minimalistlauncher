package de.moritzstaat.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** Minimal dark base theme; the full theming engine arrives in stage 12. */
@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content,
    )
}
