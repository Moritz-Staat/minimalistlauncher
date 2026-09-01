package de.moritzstaat.launcher.ui.usage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import de.moritzstaat.launcher.data.usage.FrequentApps
import de.moritzstaat.launcher.data.usage.UsageAccess
import de.moritzstaat.launcher.data.usage.UsageBreakerConfig
import de.moritzstaat.launcher.ui.common.ToggleRow
import kotlin.math.roundToInt

/** Which apps get a pause, after how many openings, and for how long. */
@Composable
fun UsageSettingsSection(modifier: Modifier = Modifier) {
    val viewModel: UsageViewModel = viewModel()
    val context = LocalContext.current
    val config by viewModel.config.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()

    var picking by remember { mutableStateOf(false) }
    val hasAccess by viewModel.usageAccessGranted.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val frequentEnabled by viewModel.frequentEnabled.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = "Häufig genutzt", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Die " + FrequentApps.LIMIT + " am häufigsten geöffneten Apps der letzten " +
                FrequentApps.WINDOW_DAYS + " Tage stehen oben in der App-Liste. Der Homescreen " +
                "bleibt leer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ToggleRow(
            label = "Häufig genutzte oben zeigen",
            checked = frequentEnabled,
            onCheckedChange = viewModel::setFrequentEnabled,
        )

        Text(
            text = "Nutzungsbremse",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "Ab einer eingestellten Zahl von Öffnungen fragt der Launcher nach, statt " +
                "die App sofort zu starten. Blockiert wird nie.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ToggleRow(
            label = "Nutzungsbremse aktiv",
            checked = config.enabled,
            onCheckedChange = viewModel::setEnabled,
        )

        SliderRow(
            label = "Ab " + config.threshold + " Öffnungen am Tag",
            value = config.threshold.toFloat(),
            range = UsageBreakerConfig.THRESHOLD_RANGE,
            onValueChange = { viewModel.setThreshold(it) },
        )
        SliderRow(
            label = if (config.pauseSeconds == 0) {
                "Ohne Wartezeit"
            } else {
                config.pauseSeconds.toString() + " Sekunden warten"
            },
            value = config.pauseSeconds.toFloat(),
            range = UsageBreakerConfig.PAUSE_RANGE,
            onValueChange = { viewModel.setPauseSeconds(it) },
        )

        Text(
            text = if (config.packages.isEmpty()) {
                "Noch keine App gewählt."
            } else {
                config.packages.size.toString() + " App(s) gewählt."
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        TextButton(onClick = { picking = true }) {
            Text(text = "Apps wählen")
        }

        if (!hasAccess) {
            Text(
                text = "Ohne Nutzungszugriff zählt nur, was über den Launcher geöffnet " +
                    "wird. Mit Zugriff zählt jedes Öffnen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { context.startActivity(UsageAccess.settingsIntent()) }) {
                Text(text = "Nutzungszugriff erteilen")
            }
        }
    }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(text = "Apps mit Bremse") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = LIST_MAX_HEIGHT)) {
                    items(apps, key = { it.key.flatten() }) { entry ->
                        val packageName = entry.key.packageName
                        val selected = packageName in config.packages
                        val opens = viewModel.opensToday(packageName)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleApp(packageName) }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(
                                text = if (selected) entry.label + " ✓" else entry.label,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Heute " + opens + " mal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { picking = false }) { Text(text = "Fertig") }
            },
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
        )
    }
}

private val LIST_MAX_HEIGHT = 360.dp
