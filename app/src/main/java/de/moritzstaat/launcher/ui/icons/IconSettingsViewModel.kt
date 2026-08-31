package de.moritzstaat.launcher.ui.icons

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.app.AppKey
import de.moritzstaat.launcher.data.db.IconOverrideEntity
import de.moritzstaat.launcher.data.icon.IconConfig
import de.moritzstaat.launcher.data.icon.IconPackInfo
import de.moritzstaat.launcher.data.icon.IconStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Icon style, icon pack and the per app override. */
class IconSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as LauncherApplication).services

    val config: StateFlow<IconConfig> = services.iconConfig

    private val _packs = MutableStateFlow<List<IconPackInfo>>(emptyList())
    val packs: StateFlow<List<IconPackInfo>> = _packs.asStateFlow()

    init {
        viewModelScope.launch {
            _packs.value = withContext(Dispatchers.IO) {
                services.iconPackRepository.installedPacks()
            }
        }
    }

    fun setStyle(style: IconStyle) {
        viewModelScope.launch { services.settings.setIconStyle(style.name) }
    }

    /** Choosing a pack also switches the style to it, which is what the user meant. */
    fun setPack(packageName: String) {
        viewModelScope.launch {
            services.settings.setIconPackPackage(packageName)
            services.settings.setIconStyle(IconStyle.IconPack.name)
        }
    }

    /** Drawable names of the active pack, for the manual chooser. */
    fun packDrawableNames(): List<String> = services.iconLoader.packDrawableNames()

    /**
     * Pins one drawable to one app. The pack is stored alongside, so the choice survives a
     * later change of pack instead of silently reverting.
     */
    fun setOverride(appKey: AppKey, drawableName: String) {
        val pack = services.iconLoader.activePackPackage() ?: return
        viewModelScope.launch {
            services.database.iconOverrideDao()
                .upsert(IconOverrideEntity(appKey.flatten(), pack, drawableName))
        }
    }

    fun clearOverride(appKey: AppKey) {
        viewModelScope.launch {
            services.database.iconOverrideDao().delete(appKey.flatten())
        }
    }
}
