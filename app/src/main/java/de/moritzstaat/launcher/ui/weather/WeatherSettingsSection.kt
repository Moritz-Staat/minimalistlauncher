package de.moritzstaat.launcher.ui.weather

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moritzstaat.launcher.data.weather.TemperatureUnit
import de.moritzstaat.launcher.ui.common.SelectableRow
import de.moritzstaat.launcher.ui.common.ToggleRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The weather line: on or off, the unit, and where the reading last came from. */
@Composable
fun WeatherSettingsSection(modifier: Modifier = Modifier) {
    val viewModel: WeatherViewModel = viewModel()
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val unit by viewModel.unit.collectAsStateWithLifecycle()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    var granted by remember { mutableStateOf(viewModel.hasLocationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { allowed ->
        granted = allowed
        viewModel.setEnabled(allowed)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = "Wetter", style = MaterialTheme.typography.titleMedium)
        ToggleRow(
            label = "Wetter anzeigen",
            checked = enabled && granted,
            onCheckedChange = { wanted ->
                if (wanted && !granted) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                } else {
                    viewModel.setEnabled(wanted)
                }
            },
        )
        if (!granted) {
            Text(
                text = "Ohne ungefähren Standort gibt es kein Wetter. " +
                    "Die Daten kommen von Open-Meteo, ohne Konto und ohne Werbe-IDs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        TemperatureUnit.entries.forEach { entry ->
            SelectableRow(
                label = unitLabel(entry),
                selected = unit == entry,
                onClick = { viewModel.setUnit(entry) },
            )
        }
        Text(
            text = statusLine(snapshot?.fetchedAtMillis, refreshing),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { viewModel.refresh(force = true) }, enabled = !refreshing) {
            Text(text = "Jetzt aktualisieren")
        }
    }
}

private fun unitLabel(unit: TemperatureUnit): String = when (unit) {
    TemperatureUnit.Celsius -> "Grad Celsius"
    TemperatureUnit.Fahrenheit -> "Grad Fahrenheit"
}

private fun statusLine(fetchedAtMillis: Long?, refreshing: Boolean): String = when {
    refreshing -> "Wird abgerufen ..."
    fetchedAtMillis == null || fetchedAtMillis == 0L -> "Noch keine Daten."
    else -> "Stand: " + FORMATTER.format(
        Instant.ofEpochMilli(fetchedAtMillis).atZone(ZoneId.systemDefault()),
    )
}

private val FORMATTER = DateTimeFormatter.ofPattern("d. MMMM, HH:mm", Locale.GERMANY)
