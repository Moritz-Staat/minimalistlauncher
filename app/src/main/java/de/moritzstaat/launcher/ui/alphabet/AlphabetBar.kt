package de.moritzstaat.launcher.ui.alphabet

import android.graphics.Rect
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Geometry of the alphabet bar. */
object AlphabetBarDefaults {
    val Width = 32.dp

    /**
     * Fallback for builds where the system ignores or clamps the gesture exclusion: moving the
     * bar away from the very edge takes it out of the back gesture's grab area.
     */
    val FallbackEdgeInset = 8.dp
}

/**
 * The wave alphabet on the right edge.
 *
 * Only occupied sections are shown. The letter under the finger grows and its neighbours grow
 * less, following a Gaussian in the distance to the touch. Scale and offset are read inside a
 * graphicsLayer lambda, so dragging only repaints; the single recomposition per letter change
 * comes from the derived active index, which drives the haptic tick and the list jump.
 */
@Composable
fun AlphabetBar(
    sections: List<String>,
    onSectionActive: (String) -> Unit,
    modifier: Modifier = Modifier,
    edgeInset: Dp = 0.dp,
    onTouchStart: () -> Unit = {},
    onTouchEnd: () -> Unit = {},
) {
    if (sections.isEmpty()) return

    val view = LocalView.current
    val touchY = remember { mutableFloatStateOf(Float.NaN) }
    val barHeightPx = remember { mutableFloatStateOf(0f) }

    val activeIndex by remember(sections.size) {
        derivedStateOf {
            WaveScale.indexAt(touchY.floatValue, barHeightPx.floatValue, sections.size)
        }
    }

    LaunchedEffect(activeIndex, sections) {
        val index = activeIndex
        if (index !in sections.indices) return@LaunchedEffect
        view.performHapticFeedback(segmentTickConstant())
        onSectionActive(sections[index])
    }

    DisposableEffect(view) {
        onDispose { view.systemGestureExclusionRects = emptyList() }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(end = edgeInset)
            .width(AlphabetBarDefaults.Width)
            .onGloballyPositioned { coordinates ->
                barHeightPx.floatValue = coordinates.size.height.toFloat()
                // Without this the back gesture on the screen edge swallows the drag.
                val bounds = coordinates.boundsInRoot()
                val rect = Rect(
                    bounds.left.toInt(),
                    bounds.top.toInt(),
                    bounds.right.toInt(),
                    bounds.bottom.toInt(),
                )
                if (view.systemGestureExclusionRects.firstOrNull() != rect) {
                    view.systemGestureExclusionRects = listOf(rect)
                }
            }
            .pointerInput(sections) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        touchY.floatValue = down.position.y.coerceIn(0f, size.height.toFloat())
                        onTouchStart()
                        var tracking = true
                        while (tracking) {
                            val change = awaitPointerEvent().changes
                                .firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                tracking = false
                            } else {
                                change.consume()
                                touchY.floatValue =
                                    change.position.y.coerceIn(0f, size.height.toFloat())
                            }
                        }
                        touchY.floatValue = Float.NaN
                        onTouchEnd()
                    }
                }
            },
    ) {
        sections.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer {
                        val slot = size.height
                        if (slot <= 0f) return@graphicsLayer
                        val center = (index + 0.5f) * slot
                        val touch = touchY.floatValue
                        if (touch.isNaN()) {
                            scaleX = 1f
                            scaleY = 1f
                            translationX = 0f
                            alpha = RESTING_ALPHA
                        } else {
                            val distanceSlots = abs(center - touch) / slot
                            val scale = WaveScale.scaleFor(distanceSlots)
                            scaleX = scale
                            scaleY = scale
                            translationX = WaveScale.pullSlotsFor(distanceSlots) * slot
                            alpha = ACTIVE_ALPHA
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** SEGMENT_TICK arrived in API 34; older builds get the closest available tick. */
private fun segmentTickConstant(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.SEGMENT_TICK
    } else {
        HapticFeedbackConstants.CLOCK_TICK
    }

private const val RESTING_ALPHA = 0.55f
private const val ACTIVE_ALPHA = 1f
