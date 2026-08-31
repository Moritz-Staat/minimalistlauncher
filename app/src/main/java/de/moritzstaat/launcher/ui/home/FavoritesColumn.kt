package de.moritzstaat.launcher.ui.home

import android.graphics.Rect
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.app.AppKey
import de.moritzstaat.launcher.data.icon.IconLoader
import de.moritzstaat.launcher.ui.applist.AppRow
import de.moritzstaat.launcher.ui.applist.AppRowDefaults

/**
 * The favourites in the user's own order, reorderable by long pressing a row and dragging it.
 *
 * A plain Column, not a list: at most eight rows of a known height, so the drag can work out
 * the new position by arithmetic instead of by measuring anything.
 */
@Composable
fun FavoritesColumn(
    favorites: List<AppEntry>,
    iconLoader: IconLoader,
    onLaunch: (AppKey, Rect?) -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (AppEntry, Rect?) -> Unit = { _, _ -> },
    onReorder: (List<String>) -> Unit = {},
    notificationPreviews: Map<String, String> = emptyMap(),
) {
    // Local working copy so the rows can move while the finger is down; the database order
    // wins again as soon as the flow re-emits after the commit.
    var order by remember(favorites) { mutableStateOf(favorites) }
    var draggedIndex by remember(favorites) { mutableIntStateOf(NO_DRAG) }
    val dragOffset = remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { AppRowDefaults.Height.toPx() }

    Column(modifier = modifier.fillMaxWidth()) {
        order.forEachIndexed { index, entry ->
            val isDragged = index == draggedIndex
            AppRow(
                entry = entry,
                iconLoader = iconLoader,
                notificationPreview = notificationPreviews[entry.key.flatten()],
                onClick = { bounds -> onLaunch(entry.key, bounds) },
                onLongClick = null,
                modifier = Modifier
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer {
                        if (index == draggedIndex) {
                            translationY = dragOffset.floatValue
                            scaleX = DRAG_SCALE
                            scaleY = DRAG_SCALE
                            alpha = DRAG_ALPHA
                        }
                    }
                    .pointerInput(entry.key, order.size) {
                        var moved = false
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedIndex = order.indexOf(entry)
                                dragOffset.floatValue = 0f
                                moved = false
                            },
                            onDragEnd = {
                                if (moved) onReorder(order.map { it.key.flatten() })
                                else onLongPress(entry, null)
                                draggedIndex = NO_DRAG
                                dragOffset.floatValue = 0f
                            },
                            onDragCancel = {
                                draggedIndex = NO_DRAG
                                dragOffset.floatValue = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                moved = true
                                dragOffset.floatValue += amount.y
                                val result = applyDrag(
                                    order = order,
                                    draggedIndex = draggedIndex,
                                    offsetPx = dragOffset.floatValue,
                                    rowHeightPx = rowHeightPx,
                                )
                                order = result.order
                                draggedIndex = result.draggedIndex
                                dragOffset.floatValue = result.offsetPx
                            },
                        )
                    },
            )
        }
    }
}

/** Result of moving a dragged row past its neighbours. */
data class DragResult<T>(
    val order: List<T>,
    val draggedIndex: Int,
    val offsetPx: Float,
)

/**
 * Moves the dragged row past as many neighbours as the accumulated offset covers and keeps the
 * remainder, so the row stays glued to the finger. Pure, so the reordering can be unit tested.
 */
fun <T> applyDrag(
    order: List<T>,
    draggedIndex: Int,
    offsetPx: Float,
    rowHeightPx: Float,
): DragResult<T> {
    if (draggedIndex !in order.indices || rowHeightPx <= 0f) {
        return DragResult(order, draggedIndex, offsetPx)
    }
    var current = order
    var index = draggedIndex
    var offset = offsetPx

    while (offset >= rowHeightPx && index < current.lastIndex) {
        current = current.moved(index, index + 1)
        index += 1
        offset -= rowHeightPx
    }
    while (offset <= -rowHeightPx && index > 0) {
        current = current.moved(index, index - 1)
        index -= 1
        offset += rowHeightPx
    }
    return DragResult(current, index, offset)
}

private fun <T> List<T>.moved(from: Int, to: Int): List<T> =
    toMutableList().apply { add(to, removeAt(from)) }

private const val NO_DRAG = -1
private const val DRAG_SCALE = 1.03f
private const val DRAG_ALPHA = 0.9f
