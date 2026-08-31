package de.moritzstaat.launcher.ui.actions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.app.AppFolder

/**
 * Puts one app into a folder: either an existing one or a new one typed in right here.
 *
 * An app belongs to at most one folder, so choosing a folder also takes it out of any other.
 */
@Composable
fun FolderPickerDialog(
    entry: AppEntry,
    folders: List<AppFolder>,
    onDismiss: () -> Unit,
    onPickExisting: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onRemoveFromFolders: () -> Unit,
) {
    var newName by remember(entry.key) { mutableStateOf("") }
    val currentFolder = folders.firstOrNull { folder ->
        folder.apps.any { it.key == entry.key }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Ordner") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                folders.forEach { folder ->
                    Text(
                        text = if (folder.id == currentFolder?.id) "${folder.name} ✓" else folder.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPickExisting(folder.id) }
                            .padding(vertical = 12.dp),
                    )
                }
                if (folders.isNotEmpty()) HorizontalDivider()
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text(text = "Neuer Ordner") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                if (currentFolder != null) {
                    Text(
                        text = "Aus dem Ordner nehmen",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRemoveFromFolders)
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(newName) },
                enabled = newName.isNotBlank(),
            ) {
                Text(text = "Anlegen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Abbrechen")
            }
        },
    )
}
