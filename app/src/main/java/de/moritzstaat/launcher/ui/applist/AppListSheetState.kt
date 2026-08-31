package de.moritzstaat.launcher.ui.applist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Open fraction of the app list, 0 = fully hidden, 1 = fully pulled up.
 *
 * Driven directly by the drag so the sheet tracks the finger, and settled with a spring on
 * release. Everything that reads [progress] does so inside a graphicsLayer or draw lambda, so
 * dragging never recomposes the app list itself.
 */
@Stable
class AppListSheetState(private val scope: CoroutineScope) {

    private val animatable = Animatable(0f)

    /** Height of the travel distance in pixels; set by the layout that hosts the sheet. */
    var heightPx by mutableFloatStateOf(1f)

    val progress: Float get() = animatable.value

    val isSettledOpen: Boolean get() = animatable.targetValue > 0.5f

    /** [deltaPx] follows the finger: negative is upwards, which opens the sheet. */
    fun dragBy(deltaPx: Float) {
        val height = heightPx.coerceAtLeast(1f)
        val next = (animatable.value - deltaPx / height).coerceIn(0f, 1f)
        scope.launch { animatable.snapTo(next) }
    }

    /** Decides where the sheet lands after the finger leaves. */
    fun settle(velocityPxPerSecond: Float, onSettled: (Boolean) -> Unit = {}) {
        val target = when {
            velocityPxPerSecond < -FLING_VELOCITY -> 1f
            velocityPxPerSecond > FLING_VELOCITY -> 0f
            animatable.value > POSITIONAL_THRESHOLD -> 1f
            else -> 0f
        }
        animateTo(target, onSettled)
    }

    fun open(onSettled: (Boolean) -> Unit = {}) = animateTo(1f, onSettled)

    fun close(onSettled: (Boolean) -> Unit = {}) = animateTo(0f, onSettled)

    private fun animateTo(target: Float, onSettled: (Boolean) -> Unit) {
        scope.launch {
            animatable.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            onSettled(target > 0.5f)
        }
    }

    private companion object {
        const val FLING_VELOCITY = 900f
        const val POSITIONAL_THRESHOLD = 0.35f
    }
}

@Composable
fun rememberAppListSheetState(): AppListSheetState {
    val scope = rememberCoroutineScope()
    return remember(scope) { AppListSheetState(scope) }
}
