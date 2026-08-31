package de.moritzstaat.launcher.ui.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/**
 * The one pop-up component. Its three uses only differ in [content]: the actions of a single
 * app, the apps inside a folder, or a widget that belongs to an app.
 *
 * The card appears where the row was, not in the middle of the screen, so the eye does not
 * have to travel. Tapping beside it, the back gesture and a swipe to the left all close it.
 */
@Composable
fun LauncherPopup(
    anchorTopPx: Int?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxHeightPx = constraints.maxHeight
        val cardMaxHeightPx = (maxHeightPx * MAX_HEIGHT_FRACTION).toInt()
        val rawTop = anchorTopPx ?: ((maxHeightPx - cardMaxHeightPx) / 2)
        val topPx = rawTop.coerceIn(0, (maxHeightPx - cardMaxHeightPx).coerceAtLeast(0))
        val topDp = with(density) { topPx.toDp() }
        val maxCardHeight = with(density) { cardMaxHeightPx.toDp() }

        // Tapping the dimmed area closes; no ripple, this is a scrim and not a button.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HORIZONTAL_MARGIN)
                .offset(y = topDp)
                .heightIn(max = maxCardHeight)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .pointerInput(onDismiss) {
                    var dragged = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragged <= -with(density) { DISMISS_DISTANCE.toPx() }) onDismiss()
                            dragged = 0f
                        },
                        onDragCancel = { dragged = 0f },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragged += amount
                        },
                    )
                }
                .padding(vertical = 8.dp),
        ) {
            content()
        }
    }
}

private const val SCRIM_ALPHA = 0.45f
private const val MAX_HEIGHT_FRACTION = 0.55f
private val HORIZONTAL_MARGIN = 16.dp
private val DISMISS_DISTANCE = 64.dp
