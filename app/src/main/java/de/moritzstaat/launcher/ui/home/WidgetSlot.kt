package de.moritzstaat.launcher.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Reserved space between the clock and the favourites.
 *
 * Stage 8 puts the media widget here and stage 9 the app widget host, so the home screen
 * already lays out with the slot in place and nothing shifts when it starts filling.
 */
@Composable
fun WidgetSlot(modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    Box(modifier = modifier.fillMaxWidth()) { content() }
}
