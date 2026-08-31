package de.moritzstaat.launcher.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A row that can be pushed sideways.
 *
 * Left dismisses the notification of that row, right opens its pop-up. Only the directions
 * that actually have a handler can be dragged, so a row without a notification does not wobble
 * to the left. The app list never scrolls horizontally, which is what keeps this gesture free.
 */
@Composable
fun SwipeableRow(
    modifier: Modifier = Modifier,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val thresholdPx = with(LocalDensity.current) { SWIPE_THRESHOLD.toPx() }
    val lowerBound = if (onSwipeLeft != null) -Float.MAX_VALUE else 0f
    val upperBound = if (onSwipeRight != null) Float.MAX_VALUE else 0f

    Box(
        modifier = modifier
            .graphicsLayer { translationX = offset.value }
            .pointerInput(onSwipeLeft, onSwipeRight) {
                if (onSwipeLeft == null && onSwipeRight == null) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val settled = offset.value
                        val width = size.width.toFloat()
                        scope.launch {
                            when {
                                settled <= -thresholdPx && onSwipeLeft != null -> {
                                    offset.animateTo(-width, tween(DISMISS_MS))
                                    onSwipeLeft()
                                    offset.snapTo(0f)
                                }

                                settled >= thresholdPx && onSwipeRight != null -> {
                                    offset.animateTo(0f, springBack())
                                    onSwipeRight()
                                }

                                else -> offset.animateTo(0f, springBack())
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f, springBack()) } },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        val next = (offset.value + amount).coerceIn(lowerBound, upperBound)
                        scope.launch { offset.snapTo(next) }
                    },
                )
            },
    ) {
        content()
    }
}

private fun springBack() = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
)

private val SWIPE_THRESHOLD = 72.dp
private const val DISMISS_MS = 140
