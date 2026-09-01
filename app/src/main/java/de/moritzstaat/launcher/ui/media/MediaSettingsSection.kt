package de.moritzstaat.launcher.ui.media

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.ui.common.ToggleRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Which music apps come up when headphones or a speaker connect. */
class MediaSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as LauncherApplication).services

    val apps: StateFlow<List<AppEntry>> = services.appIndex.visibleApps

    val enabled: StateFlow<Boolean> = services.settings.mediaAppsOnOutputChange
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), true)

    val selected: StateFlow<Set<String>> = services.settings.mediaApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { services.settings.setMediaAppsOnOutputChange(enabled) }
    }

    fun toggleApp(appKey: String) {
        viewModelScope.launch {
            val current = selected.value
            services.settings.setMediaApps(
                if (appKey in current) current - appKey else current + appKey,
            )
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

@Composable
fun MediaSettingsSection(modifier: Modifier = Modifier) {
    val viewModel: MediaSettingsViewModel = viewModel()
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()

    var picking by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Nach dem Verbinden von Kopfhoerern oder einem Lautsprecher blendet der " +
                "Homescreen diese Apps kurz ein.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ToggleRow(
            label = "Beim Verbinden einblenden",
            checked = enabled,
            onCheckedChange = viewModel::setEnabled,
        )
        Text(
            text = if (selected.isEmpty()) {
                "Noch keine App gewaehlt."
            } else {
                selected.size.toString() + " App(s) gewaehlt."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = { picking = true }) {
            Text(text = "Musik-Apps waehlen")
        }
    }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(text = "Musik-Apps") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = LIST_MAX_HEIGHT)) {
                    items(apps, key = { it.key.flatten() }) { entry ->
                        val key = entry.key.flatten()
                        Text(
                            text = if (key in selected) entry.label + " ✓" else entry.label,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleApp(key) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { picking = false }) { Text(text = "Fertig") }
            },
        )
    }
}

private val LIST_MAX_HEIGHT = 360.dp
