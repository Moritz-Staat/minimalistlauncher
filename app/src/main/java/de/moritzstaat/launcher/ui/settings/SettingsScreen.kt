package de.moritzstaat.launcher.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moritzstaat.launcher.data.notification.NotificationAccess
import de.moritzstaat.launcher.data.widget.WidgetSlot
import de.moritzstaat.launcher.system.HomeRole
import de.moritzstaat.launcher.ui.calendar.CalendarSettingsSection
import de.moritzstaat.launcher.ui.common.SettingsGroup
import de.moritzstaat.launcher.ui.gesture.GestureSettingsSection
import de.moritzstaat.launcher.ui.icons.IconSettingsSection
import de.moritzstaat.launcher.ui.theme.ThemeSettingsSection
import de.moritzstaat.launcher.ui.usage.UsageSettingsSection
import de.moritzstaat.launcher.ui.weather.WeatherSettingsSection

/**
 * All settings on one screen, in folded groups.
 *
 * There is no navigation graph behind this: a launcher's settings are one screen deep, and a
 * second back stack would fight the rule that back never leaves the launcher.
 */
@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    onAddWidget: (WidgetSlot) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel()
    var isDefaultHome by remember { mutableStateOf(HomeRole.isHeld(context)) }
    var hasNotificationAccess by remember { mutableStateOf(NotificationAccess.isGranted(context)) }

    // The role dialog result arrives before the role is actually written, so re-read on resume.
    LifecycleResumeEffect(Unit) {
        isDefaultHome = HomeRole.isHeld(context)
        hasNotificationAccess = NotificationAccess.isGranted(context)
        onPauseOrDispose { }
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        isDefaultHome = HomeRole.isHeld(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = "Einstellungen", style = MaterialTheme.typography.headlineSmall)

        SettingsGroup(
            title = "System",
            subtitle = if (isDefaultHome) "Standard-Launcher" else "Noch nicht Standard",
            initiallyExpanded = !isDefaultHome,
        ) {
            Text(
                text = if (isDefaultHome) {
                    "Minimalist ist der Standard-Launcher."
                } else {
                    "Minimalist ist noch nicht der Standard-Launcher."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = {
                    val intent = HomeRole.createRequestIntent(context)
                    if (intent != null) roleLauncher.launch(intent) else {
                        context.startActivity(HomeRole.createSettingsIntent())
                    }
                },
                enabled = !isDefaultHome,
            ) {
                Text(text = "Als Standard-Launcher setzen")
            }
            Text(
                text = if (hasNotificationAccess) {
                    "Benachrichtigungszugriff ist erteilt."
                } else {
                    "Ohne Benachrichtigungszugriff laeuft alles ausser Vorschautexten und " +
                        "Media-Widget."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = {
                    runCatching { context.startActivity(NotificationAccess.settingsIntent(context)) }
                        .onFailure {
                            context.startActivity(NotificationAccess.fallbackSettingsIntent())
                        }
                },
                enabled = !hasNotificationAccess,
            ) {
                Text(text = "Benachrichtigungszugriff erteilen")
            }
        }

        SettingsGroup(title = "Darstellung", subtitle = "Uhr, Farben, Hintergrund, Schrift") {
            ThemeSettingsSection()
        }
        SettingsGroup(title = "Icons", subtitle = "Icon-Pack, Punkte, Monochrom") {
            IconSettingsSection()
        }
        SettingsGroup(title = "Kalender", subtitle = "Termine unter der Uhr") {
            CalendarSettingsSection()
        }
        SettingsGroup(title = "Wetter", subtitle = "Open-Meteo, ohne Konto") {
            WeatherSettingsSection()
        }
        SettingsGroup(title = "Gesten", subtitle = "Tippen, Wischen, Langdruck") {
            GestureSettingsSection()
        }
        SettingsGroup(title = "Nutzungsbremse", subtitle = "Nachfragen statt sperren") {
            UsageSettingsSection()
        }
        SettingsGroup(title = "Widgets", subtitle = "Unter der Uhr oder statt der Uhr") {
            Button(onClick = { onAddWidget(WidgetSlot.UnderClock) }) {
                Text(text = "Widget unter der Uhr")
            }
            Button(onClick = { onAddWidget(WidgetSlot.InsteadOfClock) }) {
                Text(text = "Widget statt der Uhr")
            }
        }
        SettingsGroup(title = "Sicherung", subtitle = "Alles in eine Datei") {
            BackupSection()
        }
        SettingsGroup(title = "Erste Schritte", subtitle = "Einrichtung erneut anzeigen") {
            Text(
                text = "Zeigt die Einrichtung beim naechsten Oeffnen des Launchers noch einmal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { viewModel.setOnboardingDone(false) }) {
                Text(text = "Einrichtung erneut zeigen")
            }
        }

        TextButton(onClick = onDismiss) {
            Text(text = "Schliessen")
        }
    }
}
