package de.moritzstaat.launcher.ui.applist

import android.graphics.Rect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.notification.NotificationSummary
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moritzstaat.launcher.data.icon.IconLoader
import de.moritzstaat.launcher.data.icon.IconStyle
import de.moritzstaat.launcher.ui.common.AppIcon
import de.moritzstaat.launcher.ui.common.SwipeableRow

/** Fixed row geometry, shared by the app list, the favourites and the search results. */
object AppRowDefaults {
    val Height = 56.dp
    val IconSize = 40.dp
    val IconGap = 16.dp
    val HorizontalPadding = 24.dp
}

/**
 * One app.
 *
 * The label sits centred while there is nothing to report and moves up as soon as a
 * notification preview arrives, so the row height never changes while scrolling. Tapping the
 * app name starts the app, tapping the preview opens the notification, swiping left dismisses
 * it and swiping right opens the pop-up.
 */
@Composable
fun AppRow(
    entry: AppEntry,
    iconLoader: IconLoader,
    onClick: (Rect?) -> Unit,
    modifier: Modifier = Modifier,
    notification: NotificationSummary? = null,
    highlightIndices: IntArray? = null,
    onLongClick: ((Rect?) -> Unit)? = null,
    onNotificationClick: (() -> Unit)? = null,
    onNotificationDismiss: (() -> Unit)? = null,
    onSwipeRight: ((Rect?) -> Unit)? = null,
) {
    val boundsHolder = remember { BoundsHolder() }
    val iconConfig by iconLoader.config.collectAsStateWithLifecycle()
    val showIcon = iconConfig.style != IconStyle.None

    SwipeableRow(
        modifier = modifier.fillMaxWidth(),
        onSwipeLeft = if (notification != null) onNotificationDismiss else null,
        onSwipeRight = onSwipeRight?.let { callback -> { callback(boundsHolder.value) } },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppRowDefaults.Height)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    boundsHolder.value = Rect(
                        bounds.left.toInt(),
                        bounds.top.toInt(),
                        bounds.right.toInt(),
                        bounds.bottom.toInt(),
                    )
                }
                .combinedClickable(
                    onClick = { onClick(boundsHolder.value) },
                    // Null keeps the long press free for the drag detector of the favourites.
                    onLongClick = onLongClick?.let { callback -> { callback(boundsHolder.value) } },
                )
                .padding(horizontal = AppRowDefaults.HorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showIcon) {
                AppIcon(
                    appKey = entry.key,
                    iconLoader = iconLoader,
                    size = AppRowDefaults.IconSize,
                    contentDescription = entry.label,
                )
                Spacer(Modifier.width(AppRowDefaults.IconGap))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = highlightedLabel(entry.label, highlightIndices),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (notification != null && notification.preview.isNotEmpty()) {
                    Text(
                        text = notification.preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.then(
                            if (onNotificationClick != null) {
                                Modifier.clickable(onClick = onNotificationClick)
                            } else {
                                Modifier
                            },
                        ),
                    )
                }
            }
            if (notification != null && notification.count > 1) {
                Text(
                    text = notification.count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** Marks the characters the search matcher actually used. */
private fun highlightedLabel(label: String, matchedIndices: IntArray?): AnnotatedString {
    if (matchedIndices == null || matchedIndices.isEmpty()) return AnnotatedString(label)
    val highlighted = matchedIndices.toHashSet()
    return buildAnnotatedString {
        label.forEachIndexed { index, char ->
            if (index in highlighted) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(char) }
            } else {
                append(char)
            }
        }
    }
}

/** Mutable box for the row bounds; deliberately not a State, nothing recomposes on it. */
private class BoundsHolder {
    var value: Rect? = null
}
