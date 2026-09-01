package de.moritzstaat.launcher.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import de.moritzstaat.launcher.data.notification.NotificationAccess
import de.moritzstaat.launcher.system.HomeRole

/**
 * First run: what the launcher is, the home role, and the optional permissions in the order
 * they matter.
 *
 * Every step can be skipped. The launcher has to be usable with nothing granted at all, and a
 * setup that insists on permissions teaches the user to tap "allow" without reading.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var isDefaultHome by remember { mutableStateOf(HomeRole.isHeld(context)) }
    var hasNotificationAccess by remember { mutableStateOf(NotificationAccess.isGranted(context)) }

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
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (step) {
                0 -> {
                    Title("Minimalist")
                    Body(
                        "Eine Liste, eine Uhr, sonst nichts. Nach oben wischen oeffnet die " +
                            "Apps, der Buchstabenbalken rechts springt in die Liste, " +
                            "langes Druecken auf den Hintergrund oeffnet die Einstellungen.",
                    )
                }

                1 -> {
                    Title("Standard-Launcher")
                    Body(
                        if (isDefaultHome) {
                            "Erledigt. Der bisherige Launcher bleibt installiert und kann " +
                                "jederzeit wieder gewaehlt werden."
                        } else {
                            "Damit die Home-Taste hier landet. Der bisherige Launcher bleibt " +
                                "installiert; die Auswahl laesst sich jederzeit zuruecknehmen."
                        },
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
                }

                2 -> {
                    Title("Was optional dazukommt")
                    Body(
                        "Benachrichtigungen fuer Vorschautexte und das Media-Widget, " +
                            "Kontakte fuer die Suche, Kalender fuer Termine unter der Uhr und " +
                            "der grobe Standort fuer das Wetter. Alles einzeln, alles spaeter " +
                            "in den Einstellungen aenderbar.",
                    )
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        },
                    ) {
                        Text(text = "Berechtigungen abfragen")
                    }
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(NotificationAccess.settingsIntent(context))
                            }.onFailure {
                                context.startActivity(NotificationAccess.fallbackSettingsIntent())
                            }
                        },
                        enabled = !hasNotificationAccess,
                    ) {
                        Text(text = "Benachrichtigungszugriff")
                    }
                }

                else -> {
                    Title("Fertig")
                    Body(
                        "Alles Weitere steht in den Einstellungen: Uhr, Farben, Icons, Gesten " +
                            "und die Nutzungsbremse. Lange auf den leeren Hintergrund druecken.",
                    )
                }
            }
        }

        Spacer(Modifier.padding(top = 24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onFinish) {
                Text(text = "Ueberspringen")
            }
            Button(onClick = { if (step >= LAST_STEP) onFinish() else step += 1 }) {
                Text(text = if (step >= LAST_STEP) "Los" else "Weiter")
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text = text, style = MaterialTheme.typography.headlineMedium)
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val LAST_STEP = 3
