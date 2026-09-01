package de.moritzstaat.launcher.ui.calendar

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moritzstaat.launcher.ui.common.SelectableRow
import de.moritzstaat.launcher.ui.common.ToggleRow

/** Whether appointments appear on the home screen, and which calendars they come from. */
@Composable
fun CalendarSettingsSection(modifier: Modifier = Modifier) {
    val viewModel: CalendarViewModel = viewModel()
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val calendars by viewModel.calendars.collectAsStateWithLifecycle()
    val selected by viewModel.selectedCalendarIds.collectAsStateWithLifecycle()

    var granted by remember { mutableStateOf(viewModel.hasPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { allowed ->
        granted = allowed
        // Switching on without the permission would leave a switch that shows nothing.
        viewModel.setEnabled(allowed)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = "Kalender", style = MaterialTheme.typography.titleMedium)
        ToggleRow(
            label = "Termine anzeigen",
            checked = enabled && granted,
            onCheckedChange = { wanted ->
                if (wanted && !granted) {
                    permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                } else {
                    viewModel.setEnabled(wanted)
                }
            },
        )
        if (!granted) {
            Text(
                text = "Ohne Kalenderzugriff bleibt die Zeile leer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        Text(
            text = if (selected.isEmpty()) "Alle Kalender" else "Ausgewaehlte Kalender",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp),
        )
        calendars.forEach { calendar ->
            SelectableRow(
                label = calendar.displayName,
                // No selection at all means every calendar, so every row reads as selected.
                selected = selected.isEmpty() || calendar.id.toString() in selected,
                onClick = { viewModel.toggleCalendar(calendar.id) },
            )
        }
    }
}
