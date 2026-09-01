package de.moritzstaat.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moritzstaat.launcher.ui.LauncherRoot
import de.moritzstaat.launcher.ui.shell.ShellViewModel
import de.moritzstaat.launcher.ui.theme.LauncherTheme

/**
 * The one and only activity. Runs in singleTask mode, so pressing home while the launcher is
 * already in front arrives here as [onNewIntent] instead of a fresh instance.
 */
class MainActivity : ComponentActivity() {

    private val shellViewModel: ShellViewModel by viewModels()

    /**
     * The screen going off ends the session as far as the launcher is concerned.
     *
     * Registered for the whole life of the activity rather than between onStart and onStop: the
     * broadcast and the lifecycle callbacks race, and unregistering in onStop would sometimes
     * drop the very event this exists for.
     */
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) shellViewModel.collapseAll()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        registerReceiver(
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            RECEIVER_NOT_EXPORTED,
        )
        val themes = (application as LauncherApplication).services.theme
        setContent {
            val theme by themes.collectAsStateWithLifecycle()
            LauncherTheme(config = theme) {
                LauncherRoot(shellViewModel = shellViewModel)
            }
        }
    }

    /** A listening widget host is what keeps placed widgets updating. */
    override fun onStart() {
        super.onStart()
        (application as LauncherApplication).services.widgetHost.startListening()
    }

    override fun onStop() {
        (application as LauncherApplication).services.widgetHost.stopListening()
        super.onStop()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenOffReceiver) }
        super.onDestroy()
    }

    /**
     * Pressing home collapses the launcher back to its resting state. Without this the user
     * would press home and stare at whatever overlay was open before.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shellViewModel.collapseAll()
    }
}
