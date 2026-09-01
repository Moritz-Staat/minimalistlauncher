package de.moritzstaat.launcher.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.backup.LauncherBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Backup and restore of the whole setup, and the first run flag. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as LauncherApplication).services

    val onboardingDone: StateFlow<Boolean> = services.settings.onboardingDone
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _message = MutableStateFlow<String?>(null)

    /** Result of the last backup or restore, shown as one line in the settings. */
    val message: StateFlow<String?> = _message.asStateFlow()

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val text = services.backupRepository.create().encode()
            val written = withContext(Dispatchers.IO) {
                runCatching {
                    resolver().openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                        ?: error("no stream")
                }.isSuccess
            }
            _message.value = if (written) "Sicherung geschrieben." else "Sicherung fehlgeschlagen."
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    resolver().openInputStream(uri)?.use { it.readBytes().decodeToString() }
                }.getOrNull()
            }
            val backup = text?.let(LauncherBackup::decode)
            if (backup == null) {
                _message.value = "Datei ist keine Sicherung."
                return@launch
            }
            val restored = services.backupRepository.restore(backup)
            _message.value = "Sicherung eingespielt: " + restored + " Einträge."
        }
    }

    fun setOnboardingDone(done: Boolean) {
        viewModelScope.launch { services.settings.setOnboardingDone(done) }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun resolver() = getApplication<Application>().contentResolver
}
