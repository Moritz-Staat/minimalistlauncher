package de.moritzstaat.launcher.ui.actions

import android.app.Application
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.app.AppActions
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.app.AppKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the long press menu currently knows about the app it was opened on. */
data class AppActionsState(
    val entry: AppEntry,
    val anchorBounds: Rect?,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val shortcuts: List<ShortcutInfo> = emptyList(),
    val favoritesFull: Boolean = false,
)

/** Drives the long press menu of a single app row. */
class AppActionsViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as LauncherApplication).services
    private val actions = services.appActions
    private val shortcuts = services.shortcutRepository

    private val _state = MutableStateFlow<AppActionsState?>(null)
    val state: StateFlow<AppActionsState?> = _state.asStateFlow()

    /** Set while the rename dialog is open. */
    private val _renaming = MutableStateFlow<AppEntry?>(null)
    val renaming: StateFlow<AppEntry?> = _renaming.asStateFlow()

    fun open(entry: AppEntry, anchorBounds: Rect?) {
        _state.value = AppActionsState(entry = entry, anchorBounds = anchorBounds)
        viewModelScope.launch {
            val favorite = actions.isFavorite(entry.key)
            val full = services.database.favoriteDao().count() >= AppActions.MAX_FAVORITES
            val loaded = withContext(Dispatchers.IO) { shortcuts.shortcutsFor(entry.key) }
            _state.update { current ->
                if (current?.entry?.key != entry.key) current
                else current.copy(isFavorite = favorite, shortcuts = loaded, favoritesFull = full)
            }
        }
    }

    fun dismiss() {
        _state.value = null
    }

    fun toggleFavorite() {
        val current = _state.value ?: return
        viewModelScope.launch {
            actions.setFavorite(current.entry.key, !current.isFavorite)
            dismiss()
        }
    }

    fun startRename() {
        _renaming.value = _state.value?.entry
        dismiss()
    }

    fun cancelRename() {
        _renaming.value = null
    }

    fun confirmRename(label: String?) {
        val entry = _renaming.value ?: return
        viewModelScope.launch {
            actions.rename(entry.key, label)
            _renaming.value = null
        }
    }

    fun hide() {
        val current = _state.value ?: return
        viewModelScope.launch {
            actions.setHidden(current.entry.key, true)
            dismiss()
        }
    }

    fun openAppInfo() {
        val current = _state.value ?: return
        actions.openAppInfo(current.entry.key, current.anchorBounds)
        dismiss()
    }

    fun requestUninstall() {
        val current = _state.value ?: return
        actions.requestUninstall(current.entry.key)
        dismiss()
    }

    fun startShortcut(shortcut: ShortcutInfo) {
        shortcuts.start(shortcut, _state.value?.anchorBounds)
        dismiss()
    }

    fun launch(appKey: AppKey, bounds: Rect?) {
        services.appRepository.launch(appKey, bounds)
    }
}
