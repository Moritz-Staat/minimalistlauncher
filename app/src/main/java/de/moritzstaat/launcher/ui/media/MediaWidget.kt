package de.moritzstaat.launcher.ui.media

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import de.moritzstaat.launcher.data.media.AccentPicker
import de.moritzstaat.launcher.data.media.MediaCustomAction
import de.moritzstaat.launcher.data.media.MediaProgress
import de.moritzstaat.launcher.data.media.MediaState
import de.moritzstaat.launcher.ui.theme.AccentPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Media controls as a plain composable, not an app widget.
 *
 * An AppWidgetHostView would cost a process hop, would style itself and could not pick up the
 * launcher's theme.
 *
 * The layout follows the system's own media card, because that is the shape people already read
 * without thinking: output on top, then the track, a large play button on the right, and the
 * transport row over a progress bar. The app's own extra actions - shuffle, "save" - sit next to
 * the transport buttons with the icons that app ships.
 */
@Composable
fun MediaWidget(
    state: MediaState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onOpenApp: () -> Unit,
    modifier: Modifier = Modifier,
    outputName: String? = null,
    customActionIcon: suspend (String, Int) -> Bitmap? = { _, _ -> null },
    onCustomAction: (String) -> Unit = {},
) {
    val background = MaterialTheme.colorScheme.surface
    val fallback = MaterialTheme.colorScheme.primary
    val accent = rememberAccentColor(state.artwork, background.toArgb(), fallback.toArgb())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CARD_CORNER))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = CARD_ALPHA))
            .padding(CARD_PADDING),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CoverArt(state.artwork, accent, onOpenApp)
            if (outputName != null) {
                OutputChip(name = outputName, accent = accent)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onOpenApp),
                )
                if (state.artist.isNotBlank()) {
                    Text(
                        text = state.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            PlayPauseButton(isPlaying = state.isPlaying, accent = accent, onClick = onPlayPause)
        }

        ProgressBar(state = state, accent = accent, modifier = Modifier.padding(top = 12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TransportButton("⏮", state.canSkipPrevious, accent, onSkipPrevious)
            TransportButton("⏭", state.canSkipNext, accent, onSkipNext)
            state.customActions.take(MAX_CUSTOM_ACTIONS).forEach { action ->
                CustomActionButton(
                    action = action,
                    packageName = state.packageName,
                    accent = accent,
                    loadIcon = customActionIcon,
                    onClick = { onCustomAction(action.action) },
                )
            }
        }
    }
}

/**
 * The bar advances on its own between session updates: a session reports its position once and
 * then goes quiet, so ticking locally is the only way the bar moves at all.
 */
@Composable
private fun ProgressBar(state: MediaState, accent: Color, modifier: Modifier = Modifier) {
    val elapsed by produceState(state.positionMs, state) {
        while (true) {
            value = MediaProgress.elapsedMs(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                updatedAtMs = state.positionUpdatedAtMs,
                nowMs = SystemClock.elapsedRealtime(),
                speed = state.playbackSpeed,
                isPlaying = state.isPlaying,
            )
            if (!state.isPlaying) break
            delay(TICK_MS)
        }
    }
    val fraction = MediaProgress.fraction(elapsed, state.durationMs) ?: return

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(BAR_HEIGHT)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TimeLabel(MediaProgress.label(elapsed))
            TimeLabel(MediaProgress.label(state.durationMs))
        }
    }
}

@Composable
private fun TimeLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Where the sound is going, as the system's media card shows it. */
@Composable
private fun OutputChip(name: String, accent: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "🎧", style = MaterialTheme.typography.labelMedium)
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = CHIP_MAX_WIDTH),
        )
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(PLAY_BUTTON_SIZE)
            .clip(CircleShape)
            .background(accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isPlaying) "⏸" else "▶",
            style = MaterialTheme.typography.titleLarge,
            color = Color(AccentPalette.contentColorFor(accent.toArgb())),
        )
    }
}

@Composable
private fun CoverArt(artwork: Bitmap?, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    if (artwork == null) {
        Box(
            modifier = Modifier
                .size(COVER_SIZE)
                .clip(shape)
                .background(accent.copy(alpha = 0.25f))
                .clickable(onClick = onClick),
        )
    } else {
        Image(
            bitmap = artwork.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(COVER_SIZE)
                .clip(shape)
                .clickable(onClick = onClick),
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
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** An action the app defined; drawn with that app's own icon or not at all. */
@Composable
private fun CustomActionButton(
    action: MediaCustomAction,
    packageName: String,
    accent: Color,
    loadIcon: suspend (String, Int) -> Bitmap?,
    onClick: () -> Unit,
) {
    val icon by produceState<Bitmap?>(null, packageName, action.iconResId) {
        value = loadIcon(packageName, action.iconResId)
    }
    val bitmap = icon ?: return

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = action.name.takeIf { it.isNotBlank() },
        colorFilter = ColorFilter.tint(accent),
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(6.dp)
            .size(ACTION_ICON_SIZE),
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

private val COVER_SIZE = 32.dp
private val PLAY_BUTTON_SIZE = 52.dp
private val ACTION_ICON_SIZE = 20.dp
private val BAR_HEIGHT = 4.dp
private val CARD_CORNER = 20.dp
private val CARD_PADDING = 14.dp
private val CHIP_MAX_WIDTH = 140.dp

private const val CARD_ALPHA = 0.82f
private const val MAX_CUSTOM_ACTIONS = 2
private const val TICK_MS = 1_000L
