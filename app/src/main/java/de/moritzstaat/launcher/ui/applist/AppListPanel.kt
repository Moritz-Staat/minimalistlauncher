package de.moritzstaat.launcher.ui.applist

import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.app.AppKey
import de.moritzstaat.launcher.data.icon.IconLoader
import de.moritzstaat.launcher.data.notification.NotificationSummary

/**
 * The app list, pulled in from the bottom over the favourites.
 *
 * A plain LazyColumn with stable keys: no grid, no pages, no dock. The list never scrolls
 * horizontally, which keeps the horizontal swipe free for the pop-ups of stage 10.
 */
@Composable
fun AppListPanel(
    apps: List<AppEntry>,
    iconLoader: IconLoader,
    sheetState: AppListSheetState,
    listState: LazyListState,
    onLaunch: (AppKey, Rect?) -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (AppEntry, Rect?) -> Unit = { _, _ -> },
    notifications: Map<String, NotificationSummary> = emptyMap(),
    onNotificationClick: (NotificationSummary) -> Unit = {},
    onNotificationDismiss: (NotificationSummary) -> Unit = {},
    onSettled: (Boolean) -> Unit = {},
    topInset: Dp = 0.dp,
    searchActive: Boolean = false,
    resultsContent: @Composable () -> Unit = {},
    overlayContent: @Composable BoxScope.() -> Unit = {},
) {
    val nestedScroll = remember(sheetState, listState, onSettled) {
        SheetNestedScrollConnection(sheetState, listState, onSettled)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Read in the draw phase: dragging must not recompose the list.
                translationY = (1f - sheetState.progress) * size.height
            },
    ) {
        sheetState.heightPx = constraints.maxHeight.toFloat()

        Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedScroll)) {
            if (searchActive) {
                resultsContent()
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    item(key = KEY_TOP_INSET) { Spacer(Modifier.height(topInset)) }
                    items(items = apps, key = { it.key.flatten() }) { entry ->
                        AppRow(
                            entry = entry,
                            iconLoader = iconLoader,
                            notification = notifications[entry.key.packageName],
                            onClick = { bounds -> onLaunch(entry.key, bounds) },
                            onLongClick = { bounds -> onLongPress(entry, bounds) },
                            onNotificationClick = notifications[entry.key.packageName]
                                ?.let { summary -> { onNotificationClick(summary) } },
                            onNotificationDismiss = notifications[entry.key.packageName]
                                ?.let { summary -> { onNotificationDismiss(summary) } },
                        )
                    }
                    item(key = KEY_BOTTOM_SPACE) { Spacer(Modifier.height(BOTTOM_SPACE)) }
                }
            }
            overlayContent()
        }
    }
}

/** Darkens whatever the sheet covers; the blur itself sits on the home screen's layer. */
@Composable
fun AppListScrim(sheetState: AppListSheetState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = sheetState.progress }
            .background(Color.Black.copy(alpha = SCRIM_ALPHA)),
    )
}

/**
 * Hands downward scroll back to the sheet once the list is at its top, so one continuous
 * gesture can scroll the list and then push it away again.
 */
private class SheetNestedScrollConnection(
    private val sheetState: AppListSheetState,
    private val listState: LazyListState,
    private val onSettled: (Boolean) -> Unit,
) : NestedScrollConnection {

    private val isAtTop: Boolean
        get() = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val stillClosing = sheetState.progress < 1f && available.y != 0f
        val pushingDownAtTop = available.y > 0f && isAtTop
        return if (stillClosing || pushingDownAtTop) {
            sheetState.dragBy(available.y)
            Offset(0f, available.y)
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (sheetState.progress < 1f) {
            sheetState.settle(available.y, onSettled)
            return available
        }
        return Velocity.Zero
    }
}

private const val SCRIM_ALPHA = 0.55f
private const val KEY_TOP_INSET = "app-list-top-inset"
private const val KEY_BOTTOM_SPACE = "app-list-bottom-space"
private val BOTTOM_SPACE = 96.dp
