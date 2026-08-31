package de.moritzstaat.launcher.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.provider.AlarmClock
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.app.AppKey
import de.moritzstaat.launcher.data.icon.IconLoader
import de.moritzstaat.launcher.ui.applist.AppListSheetState

/**
 * Home screen body: clock at roughly a quarter of the screen height, date, widget slot,
 * favourites, then deliberate empty space. Everything is left aligned and single column.
 *
 * The whole block is blurred and dimmed by the app list's open fraction, read inside the
 * graphicsLayer lambda so pulling the list up does not recompose the home screen.
 */
@Composable
fun HomeScreen(
    favorites: List<AppEntry>,
    iconLoader: IconLoader,
    sheetState: AppListSheetState,
    onLaunch: (AppKey, Rect?) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onSheetSettled: (Boolean) -> Unit = {},
    widgetSlotContent: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val dragTracker = remember { VelocityTracker() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(sheetState) {
                detectVerticalDragGestures(
                    onDragStart = { dragTracker.resetTracking() },
                    onDragEnd = {
                        sheetState.settle(dragTracker.calculateVelocity().y, onSheetSettled)
                    },
                    onDragCancel = { sheetState.settle(0f, onSheetSettled) },
                    onVerticalDrag = { change, dragAmount ->
                        dragTracker.addPointerInputChange(change)
                        change.consume()
                        sheetState.dragBy(dragAmount)
                    },
                )
            },
    ) {
        val clockTopPadding = maxHeight * CLOCK_TOP_FRACTION

        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val progress = sheetState.progress
                    if (progress > 0.01f) {
                        val radius = MAX_BLUR_PX * progress
                        renderEffect = BlurEffect(radius, radius, TileMode.Decal)
                    }
                    alpha = 1f - HOME_FADE * progress
                }
                .safeDrawingPadding(),
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.height(clockTopPadding))
            ClockBlock(
                onClockClick = { openClockApp(context) },
                modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
            )
            Spacer(Modifier.height(16.dp))
            WidgetSlot(modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING)) {
                widgetSlotContent()
            }
            Spacer(Modifier.height(16.dp))
            FavoritesColumn(
                favorites = favorites,
                iconLoader = iconLoader,
                onLaunch = onLaunch,
            )
            // Everything below stays empty on purpose: the app list slides in here.
            // Long pressing that empty area is the way into the launcher settings.
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(onOpenSettings) {
                        detectTapGestures(onLongPress = { onOpenSettings() })
                    },
            )
        }
    }
}

private fun openClockApp(context: Context) {
    val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No clock app that answers the alarm intent; silently ignore, nothing to fall back to.
    }
}

private const val CLOCK_TOP_FRACTION = 0.22f
private const val MAX_BLUR_PX = 26f
private const val HOME_FADE = 0.35f
private val HORIZONTAL_PADDING = 24.dp
