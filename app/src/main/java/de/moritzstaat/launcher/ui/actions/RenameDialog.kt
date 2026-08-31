package de.moritzstaat.launcher.ui.actions

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.moritzstaat.launcher.data.app.AppEntry

/** Renames one app. An empty field restores the name the system reports. */
@Composable
fun RenameDialog(
    entry: AppEntry,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var text by remember(entry.key) { mutableStateOf(entry.label) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Umbenennen") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(text = entry.systemLabel) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.takeIf { it.isNotBlank() }) }) {
                Text(text = "Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(null) }) {
                Text(text = "Zuruecksetzen")
            }
        },
    )
}
