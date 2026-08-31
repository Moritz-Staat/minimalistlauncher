package de.moritzstaat.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moritzstaat.launcher.ui.shell.OverlayTarget
import de.moritzstaat.launcher.ui.shell.ShellViewModel

/**
 * Root of the launcher UI.
 *
 * The back gesture is always consumed here. A home screen has nowhere to go back to, and an
 * unhandled back press would drop the user onto a black screen.
 */
@Composable
fun LauncherRoot(shellViewModel: ShellViewModel) {
    val overlay by shellViewModel.overlay.collectAsStateWithLifecycle()

    BackHandler(enabled = true) {
        shellViewModel.closeOverlays()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScaffold(
            onOpenSettings = { shellViewModel.open(OverlayTarget.Settings) },
        )
        if (overlay == OverlayTarget.Settings) {
            SetupOverlay(onDismiss = { shellViewModel.closeOverlays() })
        }
    }
}
