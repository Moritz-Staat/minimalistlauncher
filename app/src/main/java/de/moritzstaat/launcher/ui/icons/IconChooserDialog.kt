package de.moritzstaat.launcher.ui.icons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moritzstaat.launcher.data.app.AppEntry

/**
 * Picks one drawable out of the active icon pack for a single app.
 *
 * The choice is stored with the pack it came from, so switching the pack later does not throw
 * away what the user set by hand.
 */
@Composable
fun IconChooserDialog(
    entry: AppEntry,
    onDismiss: () -> Unit,
) {
    val viewModel: IconSettingsViewModel = viewModel()
    val names = remember(entry.key) { viewModel.packDrawableNames() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = entry.label) },
        text = {
            Column {
                if (names.isEmpty()) {
                    Text(
                        text = "Kein Icon-Pack aktiv. Erst in den Einstellungen eines waehlen.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(320.dp)) {
                        items(count = names.size, key = { names[it] }) { index ->
                            val name = names[index]
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setOverride(entry.key, name)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.clearOverride(entry.key)
                    onDismiss()
                },
            ) {
                Text(text = "Zuruecksetzen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Abbrechen")
            }
        },
    )
}
