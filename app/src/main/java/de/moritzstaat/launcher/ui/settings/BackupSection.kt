package de.moritzstaat.launcher.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** Writes the whole setup to a file and reads it back. */
@Composable
fun BackupSection(modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = viewModel()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val exporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(MIME_TYPE),
    ) { uri -> uri?.let(viewModel::exportBackup) }
    val importer = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importBackup) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Gesichert werden Einstellungen, Favoriten, Ordner, eigene Namen und Icons " +
                "sowie ausgeblendete Apps. Nicht gesichert werden platzierte Widgets, die " +
                "Schriftdatei und die Zaehler der Nutzungsbremse.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Einspielen ersetzt den aktuellen Stand, es wird nicht zusammengefuehrt.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { exporter.launch(FILE_NAME) }) {
                Text(text = "Sichern")
            }
            TextButton(onClick = { importer.launch(IMPORT_TYPES) }) {
                Text(text = "Einspielen")
            }
        }
        message?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = viewModel::clearMessage),
            )
        }
    }
}

private const val MIME_TYPE = "application/json"
private const val FILE_NAME = "minimalist-backup.json"
private val IMPORT_TYPES = arrayOf("application/json", "text/plain")
