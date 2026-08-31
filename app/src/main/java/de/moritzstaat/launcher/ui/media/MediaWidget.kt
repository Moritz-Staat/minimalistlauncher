package de.moritzstaat.launcher.ui.media

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import de.moritzstaat.launcher.data.media.AccentPicker
import de.moritzstaat.launcher.data.media.MediaState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Media controls as a plain composable, not an app widget.
 *
 * An AppWidgetHostView would cost a process hop, would style itself and could not pick up the
 * launcher's theme; this needs cover, two lines of text and three buttons.
 */
@Composable
fun MediaWidget(
    state: MediaState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onOpenApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.surface
    val fallback = MaterialTheme.colorScheme.primary
    val accent = rememberAccentColor(state.artwork, background.toArgb(), fallback.toArgb())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpenApp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(state.artwork, accent)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.artist.isNotBlank()) {
                Text(
                    text = state.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TransportButton(
            label = "⏮",
            enabled = state.canSkipPrevious,
            tint = accent,
            onClick = onSkipPrevious,
        )
        TransportButton(
            label = if (state.isPlaying) "⏸" else "▶",
            enabled = true,
            tint = accent,
            onClick = onPlayPause,
        )
        TransportButton(
            label = "⏭",
            enabled = state.canSkipNext,
            tint = accent,
            onClick = onSkipNext,
        )
    }
}

@Composable
private fun CoverArt(artwork: Bitmap?, accent: Color) {
    val shape = MaterialTheme.shapes.medium
    if (artwork == null) {
        Box(
            modifier = Modifier
                .size(COVER_SIZE)
                .clip(shape)
                .background(accent.copy(alpha = 0.25f)),
        )
    } else {
        Image(
            bitmap = artwork.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(COVER_SIZE)
                .clip(shape),
        )
    }
}

@Composable
private fun TransportButton(
    label: String,
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = if (enabled) tint else tint.copy(alpha = 0.3f),
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

/**
 * Accent colour of the cover, worked out off the main thread. Palette is the only thing the
 * launcher uses the library for, and only on cover changes.
 */
@Composable
private fun rememberAccentColor(artwork: Bitmap?, backgroundArgb: Int, fallbackArgb: Int): Color {
    val accent by produceState(initialValue = Color(fallbackArgb), artwork, backgroundArgb) {
        if (artwork == null) {
            value = Color(fallbackArgb)
            return@produceState
        }
        val picked = withContext(Dispatchers.Default) {
            val palette = Palette.from(artwork).clearFilters().generate()
            AccentPicker.pick(
                candidates = listOf(
                    palette.getVibrantColor(0),
                    palette.getLightVibrantColor(0),
                    palette.getMutedColor(0),
                    palette.getLightMutedColor(0),
                    palette.getDominantColor(0),
                ),
                background = backgroundArgb,
                fallback = fallbackArgb,
            )
        }
        value = Color(picked)
    }
    return accent
}

private val COVER_SIZE = 56.dp
