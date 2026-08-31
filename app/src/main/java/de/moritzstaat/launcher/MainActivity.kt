package de.moritzstaat.launcher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import de.moritzstaat.launcher.ui.LauncherRoot
import de.moritzstaat.launcher.ui.shell.ShellViewModel
import de.moritzstaat.launcher.ui.theme.LauncherTheme

/**
 * The one and only activity. Runs in singleTask mode, so pressing home while the launcher is
 * already in front arrives here as [onNewIntent] instead of a fresh instance.
 */
class MainActivity : ComponentActivity() {

    private val shellViewModel: ShellViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            LauncherTheme {
                LauncherRoot(shellViewModel = shellViewModel)
            }
        }
    }

    /**
     * Pressing home collapses the launcher back to its resting state. Without this the user
     * would press home and stare at whatever overlay was open before.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shellViewModel.closeOverlays()
    }
}
