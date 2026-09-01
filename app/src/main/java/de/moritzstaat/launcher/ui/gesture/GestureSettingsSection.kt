package de.moritzstaat.launcher.ui.gesture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.gesture.Gesture
import de.moritzstaat.launcher.data.gesture.GestureAccess
import de.moritzstaat.launcher.data.gesture.GestureAction
import de.moritzstaat.launcher.ui.common.SelectableRow

/** One row per gesture, plus the switch for the service the two system actions need. */
@Composable
fun GestureSettingsSection(modifier: Modifier = Modifier) {
    val viewModel: GestureViewModel = viewModel()
    val context = LocalContext.current
    val gestures by viewModel.gestures.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<Gesture?>(null) }
    var pickingAppFor by remember { mutableStateOf<Gesture?>(null) }
    var serviceRunning by remember { mutableStateOf(GestureAccess.isGranted(context)) }

    // The user leaves for the system settings and comes back; re-read rather than guess.
    LifecycleResumeEffect(Unit) {
        serviceRunning = GestureAccess.isGranted(context)
        onPauseOrDispose { }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = "Gesten", style = MaterialTheme.typography.titleMedium)
        Gesture.entries.forEach { gesture ->
            val action = gestures[gesture] ?: gesture.default
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editing = gesture }
                    .padding(vertical = 8.dp),
            ) {
                Text(text = gesture.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = viewModel.labelOf(action),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = if (serviceRunning) {
                "Bedienungshilfen-Dienst läuft."
            } else {
                "Benachrichtigungen öffnen und Bildschirm sperren brauchen den " +
                    "Bedienungshilfen-Dienst. Bei einer selbst installierten App muss die " +
                    "Einstellung erst über das Menü der App-Info freigegeben werden."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        TextButton(onClick = { context.startActivity(GestureAccess.settingsIntent()) }) {
            Text(text = "Bedienungshilfen öffnen")
        }
    }

    editing?.let { gesture ->
        GestureActionDialog(
            gesture = gesture,
            current = gestures[gesture] ?: gesture.default,
            labelOf = viewModel::labelOf,
            onPick = { action ->
                viewModel.setGesture(gesture, action)
                editing = null
            },
            onPickApp = {
                editing = null
                pickingAppFor = gesture
            },
            onDismiss = { editing = null },
        )
    }

    pickingAppFor?.let { gesture ->
        AppPickerDialog(
            apps = apps,
            onPick = { entry ->
                viewModel.setGesture(gesture, GestureAction.LaunchApp(entry.key.flatten()))
                pickingAppFor = null
            },
            onDismiss = { pickingAppFor = null },
        )
    }
}

@Composable
private fun GestureActionDialog(
    gesture: Gesture,
    current: GestureAction,
    labelOf: (GestureAction) -> String,
    onPick: (GestureAction) -> Unit,
    onPickApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = gesture.label) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                GestureAction.SIMPLE.forEach { action ->
                    SelectableRow(
                        label = labelOf(action),
                        selected = current == action,
                        onClick = { onPick(action) },
                    )
                }
                SelectableRow(
                    label = "App starten ...",
                    selected = current is GestureAction.LaunchApp,
                    onClick = onPickApp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "Schließen") }
        },
    )
}

@Composable
private fun AppPickerDialog(
    apps: List<AppEntry>,
    onPick: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "App wählen") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = LIST_MAX_HEIGHT)) {
                items(apps, key = { it.key.flatten() }) { entry ->
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(entry) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "Abbrechen") }
        },
    )
}

private val LIST_MAX_HEIGHT = 360.dp
