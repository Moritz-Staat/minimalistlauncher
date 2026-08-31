package de.moritzstaat.launcher.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import de.moritzstaat.launcher.data.notification.NotificationAccess
import de.moritzstaat.launcher.system.HomeRole

/**
 * Minimal stage 2 setup sheet: report whether the launcher holds the home role and offer the
 * system dialog. Replaced by the full onboarding in stage 16.
 */
@Composable
fun SetupOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
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
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Einrichtung", style = MaterialTheme.typography.headlineSmall)
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
                if (intent != null) roleLauncher.launch(intent)
                else context.startActivity(HomeRole.createSettingsIntent())
            },
            enabled = !isDefaultHome,
        ) {
            Text(text = "Als Standard-Launcher setzen")
        }
        Text(
            text = if (hasNotificationAccess) {
                "Benachrichtigungszugriff ist erteilt."
            } else {
                "Ohne Benachrichtigungszugriff laeuft alles ausser Vorschautexten und Media-Widget."
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
        TextButton(onClick = onDismiss) {
            Text(text = "Schliessen")
        }
    }
}
