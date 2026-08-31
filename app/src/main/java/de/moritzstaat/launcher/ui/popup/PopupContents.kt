package de.moritzstaat.launcher.ui.popup

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.app.AppFolder
import de.moritzstaat.launcher.data.app.ShortcutRepository
import de.moritzstaat.launcher.data.icon.IconLoader
import de.moritzstaat.launcher.data.notification.NotificationSummary
import de.moritzstaat.launcher.data.widget.WidgetSlot
import de.moritzstaat.launcher.ui.applist.AppRow
import de.moritzstaat.launcher.ui.widget.HomeWidgetSlot

/**
 * App pop-up: whatever the app has to offer right now. A widget the user pinned to this app,
 * its current notifications, and its shortcuts.
 */
@Composable
fun AppPopupContent(
    entry: AppEntry,
    iconLoader: IconLoader,
    shortcuts: List<ShortcutInfo>,
    shortcutRepository: ShortcutRepository,
    notification: NotificationSummary?,
    onLaunch: () -> Unit,
    onShortcut: (ShortcutInfo) -> Unit,
    onNotificationClick: () -> Unit,
    onNotificationDismiss: () -> Unit,
    onOpenActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        AppRow(
            entry = entry,
            iconLoader = iconLoader,
            notification = notification,
            onClick = { onLaunch() },
            onNotificationClick = onNotificationClick.takeIf { notification != null },
            onNotificationDismiss = onNotificationDismiss.takeIf { notification != null },
        )

        HomeWidgetSlot(
            slot = WidgetSlot.Popup,
            ownerKey = entry.key.flatten(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        if (shortcuts.isNotEmpty()) {
            HorizontalDivider()
            shortcuts.forEach { shortcut ->
                PopupRow(
                    label = shortcutRepository.label(shortcut),
                    onClick = { onShortcut(shortcut) },
                )
            }
        }

        HorizontalDivider()
        PopupRow(label = "Weitere Aktionen", onClick = onOpenActions)
    }
}

/** Folder pop-up: just the apps, in the order the user put them in. */
@Composable
fun FolderPopupContent(
    folder: AppFolder,
    iconLoader: IconLoader,
    notifications: Map<String, NotificationSummary>,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Text(
            text = folder.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        HorizontalDivider()
        folder.apps.forEach { entry ->
            AppRow(
                entry = entry,
                iconLoader = iconLoader,
                notification = notifications[entry.key.packageName],
                onClick = { onLaunch(entry) },
                onLongClick = { onLongPress(entry) },
            )
        }
    }
}

@Composable
private fun PopupRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    )
}
