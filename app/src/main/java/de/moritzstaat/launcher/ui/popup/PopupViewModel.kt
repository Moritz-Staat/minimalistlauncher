package de.moritzstaat.launcher.ui.popup

import android.app.Application
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.app.AppFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns the open pop-up and the shortcuts it shows. */
class PopupViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as LauncherApplication).services

    val iconLoader = services.iconLoader
    val shortcutRepository = services.shortcutRepository

    private val _target = MutableStateFlow<PopupTarget?>(null)
    val target: StateFlow<PopupTarget?> = _target.asStateFlow()

    private val _shortcuts = MutableStateFlow<List<ShortcutInfo>>(emptyList())
    val shortcuts: StateFlow<List<ShortcutInfo>> = _shortcuts.asStateFlow()

    fun openApp(entry: AppEntry, anchor: Rect?) {
        _target.value = PopupTarget.App(entry, anchor?.top)
        _shortcuts.value = emptyList()
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { shortcutRepository.shortcutsFor(entry.key) }
            if ((_target.value as? PopupTarget.App)?.entry?.key == entry.key) {
                _shortcuts.value = loaded
            }
        }
    }

    fun openFolder(folder: AppFolder, anchor: Rect?) {
        _target.value = PopupTarget.Folder(folder, anchor?.top)
        _shortcuts.value = emptyList()
    }

    fun dismiss() {
        _target.value = null
        _shortcuts.value = emptyList()
    }

    fun startShortcut(shortcut: ShortcutInfo) {
        shortcutRepository.start(shortcut)
        dismiss()
    }
}
