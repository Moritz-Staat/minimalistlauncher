package de.moritzstaat.launcher.ui.actions

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.moritzstaat.launcher.data.app.ShortcutRepository

/**
 * Long press menu of one app row: the app's own shortcuts first, then the launcher's actions.
 *
 * Anchored to the row it was opened on rather than centred, so the finger stays where it was.
 */
@Composable
fun AppActionSheet(
    state: AppActionsState,
    shortcutRepository: ShortcutRepository,
    onDismiss: () -> Unit,
    onShortcut: (ShortcutInfo) -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onChangeIcon: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = state.entry.label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )

            if (state.shortcuts.isNotEmpty()) {
                HorizontalDivider()
                state.shortcuts.forEach { shortcut ->
                    ActionRow(
                        label = shortcutRepository.label(shortcut),
                        onClick = { onShortcut(shortcut) },
                    )
                }
            }

            HorizontalDivider()
            ActionRow(
                label = when {
                    state.isFavorite -> "Aus Favoriten entfernen"
                    state.favoritesFull -> "Favoriten sind voll"
                    else -> "Zu Favoriten"
                },
                enabled = state.isFavorite || !state.favoritesFull,
                onClick = onToggleFavorite,
            )
            ActionRow(label = "Umbenennen", onClick = onRename)
            ActionRow(label = "Icon aendern", onClick = onChangeIcon)
            ActionRow(label = "Ausblenden", onClick = onHide)
            ActionRow(label = "App-Info", onClick = onAppInfo)
            ActionRow(label = "Deinstallieren", onClick = onUninstall)
        }
    }
}

@Composable
private fun ActionRow(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    )
}

private const val SCRIM_ALPHA = 0.6f
