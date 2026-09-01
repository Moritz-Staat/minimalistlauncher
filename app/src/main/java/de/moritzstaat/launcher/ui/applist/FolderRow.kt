package de.moritzstaat.launcher.ui.applist

import android.graphics.Rect
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.moritzstaat.launcher.data.app.AppFolder
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moritzstaat.launcher.data.icon.IconLoader
import de.moritzstaat.launcher.data.icon.IconStyle
import de.moritzstaat.launcher.ui.common.AppIcon
import de.moritzstaat.launcher.ui.common.SwipeableRow

/**
 * A folder in the app list. Same geometry as an app row, but the icon slot holds the first
 * four member icons so the folder is recognisable without opening it.
 */
@Composable
fun FolderRow(
    folder: AppFolder,
    iconLoader: IconLoader,
    onOpen: (Rect?) -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: ((Rect?) -> Unit)? = null,
) {
    val bounds = remember { FolderBounds() }
    val iconConfig by iconLoader.config.collectAsStateWithLifecycle()
    val showIcon = iconConfig.style != IconStyle.None

    SwipeableRow(
        modifier = modifier.fillMaxWidth(),
        onSwipeRight = { onOpen(bounds.value) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppRowDefaults.Height)
                .onGloballyPositioned { coordinates ->
                    val rect = coordinates.boundsInWindow()
                    bounds.value = Rect(
                        rect.left.toInt(),
                        rect.top.toInt(),
                        rect.right.toInt(),
                        rect.bottom.toInt(),
                    )
                }
                .combinedClickable(
                    onClick = { onOpen(bounds.value) },
                    onLongClick = onLongClick?.let { callback -> { callback(bounds.value) } },
                )
                .padding(horizontal = AppRowDefaults.HorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showIcon) {
                FolderIcon(folder, iconLoader)
                Spacer(Modifier.width(AppRowDefaults.IconGap))
            }
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FolderIcon(folder: AppFolder, iconLoader: IconLoader) {
    val members = folder.apps.take(MAX_PREVIEW_ICONS)
    Box(
        modifier = Modifier.size(AppRowDefaults.IconSize),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            members.chunked(2).forEach { rowMembers ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    rowMembers.forEach { entry ->
                        AppIcon(
                            appKey = entry.key,
                            iconLoader = iconLoader,
                            size = MINI_ICON_SIZE,
                        )
                    }
                }
            }
        }
    }
}

private class FolderBounds {
    var value: Rect? = null
}

private const val MAX_PREVIEW_ICONS = 4
private val MINI_ICON_SIZE = 18.dp
