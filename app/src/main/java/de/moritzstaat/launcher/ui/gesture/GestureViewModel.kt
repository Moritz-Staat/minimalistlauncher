package de.moritzstaat.launcher.ui.gesture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.gesture.Gesture
import de.moritzstaat.launcher.data.gesture.GestureAccess
import de.moritzstaat.launcher.data.gesture.GestureAction
import de.moritzstaat.launcher.service.LauncherGestureService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What the gestures are set to, and the two actions that need the accessibility service. */
class GestureViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as LauncherApplication).services

    val gestures: StateFlow<Map<Gesture, GestureAction>> = services.gestures

    /** For the "start an app" picker in the settings. */
    val apps: StateFlow<List<AppEntry>> = services.appIndex.visibleApps

    val iconLoader = services.iconLoader

    fun setGesture(gesture: Gesture, action: GestureAction) {
        viewModelScope.launch { services.settings.setGesture(gesture, action) }
    }

    fun launchApp(action: GestureAction.LaunchApp) {
        action.key?.let { services.appRepository.launch(it) }
    }

    /** @return false when the accessibility service is off, so the caller can say why. */
    fun expandNotifications(): Boolean = LauncherGestureService.expandNotifications()

    fun lockScreen(): Boolean = LauncherGestureService.lockScreen()

    fun hasGestureService(): Boolean = GestureAccess.isGranted(getApplication())

    /** Label for the settings list; an app action shows the app's name, not its package. */
    fun labelOf(action: GestureAction): String = when (action) {
        GestureAction.None -> "Nichts"
        GestureAction.OpenAppList -> "App-Liste öffnen"
        GestureAction.OpenSearch -> "Suche öffnen"
        GestureAction.OpenSettings -> "Einstellungen öffnen"
        GestureAction.ExpandNotifications -> "Benachrichtigungen öffnen"
        GestureAction.LockScreen -> "Bildschirm sperren"
        is GestureAction.LaunchApp -> apps.value
            .firstOrNull { it.key.flatten() == action.appKey }
            ?.let { "App: " + it.label }
            ?: "App starten"
    }
}
