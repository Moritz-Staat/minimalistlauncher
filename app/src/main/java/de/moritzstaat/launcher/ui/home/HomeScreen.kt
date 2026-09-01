package de.moritzstaat.launcher.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.provider.AlarmClock
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import de.moritzstaat.launcher.data.gesture.Gesture
import de.moritzstaat.launcher.data.icon.IconLoader
import de.moritzstaat.launcher.data.notification.NotificationSummary
import de.moritzstaat.launcher.ui.applist.AppListSheetState
import de.moritzstaat.launcher.ui.calendar.CalendarSlot
import de.moritzstaat.launcher.ui.weather.WeatherSlot
import de.moritzstaat.launcher.data.widget.WidgetSlot
import de.moritzstaat.launcher.ui.media.MediaSlot
import de.moritzstaat.launcher.ui.widget.HomeWidgetSlot
import de.moritzstaat.launcher.ui.widget.hasClockReplacementWidget

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
    onGesture: (Gesture) -> Unit = {},
    onLongPressFavorite: (AppEntry, Rect?) -> Unit = { _, _ -> },
    onReorderFavorites: (List<String>) -> Unit = {},
    notifications: Map<String, NotificationSummary> = emptyMap(),
    onNotificationClick: (NotificationSummary) -> Unit = {},
    onNotificationDismiss: (NotificationSummary) -> Unit = {},
    onSheetSettled: (Boolean) -> Unit = {},
    widgetSlotContent: @Composable () -> Unit = { MediaSlot() },
) {
    val context = LocalContext.current
    val dragTracker = remember { VelocityTracker() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(sheetState, onGesture) {
                val thresholdPx = SWIPE_THRESHOLD.toPx()
                var travelled = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        dragTracker.resetTracking()
                        travelled = 0f
                    },
                    onDragEnd = {
                        // Pulling down while the list is already closed is a gesture, not a
                        // drag: the sheet has nowhere further to go.
                        if (sheetState.progress == 0f && travelled >= thresholdPx) {
                            onGesture(Gesture.SwipeDown)
                        }
                        sheetState.settle(dragTracker.calculateVelocity().y, onSheetSettled)
                    },
                    onDragCancel = { sheetState.settle(0f, onSheetSettled) },
                    onVerticalDrag = { change, dragAmount ->
                        dragTracker.addPointerInputChange(change)
                        change.consume()
                        travelled += dragAmount
                        sheetState.dragBy(dragAmount)
                    },
                )
            }
            // A second detector rather than one combined one: whichever axis crosses the touch
            // slop first consumes the gesture, which is exactly the wanted behaviour.
            .pointerInput(onGesture) {
                val thresholdPx = SWIPE_THRESHOLD.toPx()
                var travelled = 0f
                detectHorizontalDragGestures(
                    onDragStart = { travelled = 0f },
                    onDragEnd = {
                        when {
                            travelled <= -thresholdPx -> onGesture(Gesture.SwipeLeft)
                            travelled >= thresholdPx -> onGesture(Gesture.SwipeRight)
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        travelled += dragAmount
                    },
                )
            }
            // Taps land here only where nothing else took them: the clock, the favourites and
            // the widgets consume their own, so this is the empty background.
            .pointerInput(onGesture) {
                detectTapGestures(
                    onDoubleTap = { onGesture(Gesture.DoubleTap) },
                    onLongPress = { onGesture(Gesture.LongPress) },
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
            if (hasClockReplacementWidget()) {
                // A widget in that slot takes the clock's place entirely, not just its space.
                HomeWidgetSlot(
                    slot = WidgetSlot.InsteadOfClock,
                    modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
                )
            } else {
                ClockBlock(
                    onClockClick = { openClockApp(context) },
                    modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
                )
            }
            Box(modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING)) {
                Column {
                    // Weather and appointments sit between the clock and the widgets: they
                    // read as part of "what is going on", not as another widget.
                    WeatherSlot()
                    CalendarSlot()
                }
            }
            Spacer(Modifier.height(16.dp))
            HomeWidgetSlot(
                slot = WidgetSlot.UnderClock,
                modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
            )
            Box(modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING)) {
                widgetSlotContent()
            }
            Spacer(Modifier.height(16.dp))
            FavoritesColumn(
                favorites = favorites,
                iconLoader = iconLoader,
                onLaunch = onLaunch,
                onLongPress = onLongPressFavorite,
                onReorder = onReorderFavorites,
                notifications = notifications,
                onNotificationClick = onNotificationClick,
                onNotificationDismiss = onNotificationDismiss,
            )
            // Everything below stays empty on purpose: the app list slides in here, and the
            // gesture detectors on the root pick up whatever happens in it.
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
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

/** How far a finger has to travel before a drag counts as a gesture. */
private val SWIPE_THRESHOLD = 72.dp

private const val CLOCK_TOP_FRACTION = 0.22f
private const val MAX_BLUR_PX = 26f
private const val HOME_FADE = 0.35f
private val HORIZONTAL_PADDING = 24.dp
