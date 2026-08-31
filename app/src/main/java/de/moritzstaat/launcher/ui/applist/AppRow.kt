package de.moritzstaat.launcher.ui.applist

import android.graphics.Rect
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.icon.IconLoader
import de.moritzstaat.launcher.ui.common.AppIcon

/** Fixed row geometry, shared by the app list, the favourites and the search results. */
object AppRowDefaults {
    val Height = 56.dp
    val IconSize = 40.dp
    val IconGap = 16.dp
    val HorizontalPadding = 24.dp
}

/**
 * One app. The label sits centred while there is nothing to report and slides up as soon as a
 * notification preview arrives, so the row height never changes while scrolling.
 */
@Composable
fun AppRow(
    entry: AppEntry,
    iconLoader: IconLoader,
    onClick: (Rect?) -> Unit,
    modifier: Modifier = Modifier,
    notificationPreview: String? = null,
    onLongClick: (Rect?) -> Unit = {},
) {
    val boundsHolder = remember { BoundsHolder() }

    Row(
        modifier = modifier
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
                onLongClick = { onLongClick(boundsHolder.value) },
            )
            .padding(horizontal = AppRowDefaults.HorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            appKey = entry.key,
            iconLoader = iconLoader,
            size = AppRowDefaults.IconSize,
            contentDescription = entry.label,
        )
        Spacer(Modifier.width(AppRowDefaults.IconGap))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = entry.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (notificationPreview != null) {
                Text(
                    text = notificationPreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Mutable box for the row bounds; deliberately not a State, nothing recomposes on it. */
private class BoundsHolder {
    var value: Rect? = null
}
