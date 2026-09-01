package de.moritzstaat.launcher.ui.theme

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.settings.ClockStyle
import de.moritzstaat.launcher.data.settings.ColorMode
import de.moritzstaat.launcher.data.settings.HourFormat
import de.moritzstaat.launcher.data.settings.ThemeConfig
import de.moritzstaat.launcher.data.settings.ThemePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Clock, colours, wallpaper treatment, font, and the import and export of the whole theme. */
class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as LauncherApplication).services

    val theme: StateFlow<ThemeConfig> = services.theme

    /** Last import or export result, shown as one line under the buttons. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setClockStyle(style: ClockStyle) = update { it.copy(clockStyle = style) }

    fun setHourFormat(format: HourFormat) = update { it.copy(hourFormat = format) }

    fun setShowDate(show: Boolean) = update { it.copy(showDate = show) }

    fun setColorMode(mode: ColorMode) = update { it.copy(colorMode = mode) }

    /** Picking a colour also switches to the manual mode, which is what the user meant. */
    fun setAccent(argb: Int) = update { it.copy(accentArgb = argb, colorMode = manualOrKeep(it)) }

    fun setDarkTheme(dark: Boolean) = update { it.copy(darkTheme = dark) }

    fun setWallpaperDim(dim: Float) = update { it.copy(wallpaperDim = dim.coerceIn(0f, 1f)) }

    fun setWallpaperBlur(blur: Float) = update { it.copy(wallpaperBlur = blur.coerceIn(0f, 1f)) }

    fun setHideStatusBar(hidden: Boolean) = update { it.copy(hideStatusBar = hidden) }

    /** A preset keeps the font: it is the user's file, not part of the look we ship. */
    fun applyPreset(preset: ThemePreset) = update { preset.config.copy(fontPath = it.fontPath) }

    fun setFont(uri: Uri) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { services.fontStore.store(uri) }
            if (path == null) {
                _message.value = "Schriftdatei konnte nicht gelesen werden."
                return@launch
            }
            services.settings.setTheme(theme.value.copy(fontPath = path))
            _message.value = "Schrift übernommen."
        }
    }

    fun clearFont() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { services.fontStore.clear() }
            services.settings.setTheme(theme.value.copy(fontPath = ""))
        }
    }

    fun exportTheme(uri: Uri) {
        val text = theme.value.encode()
        viewModelScope.launch {
            val written = withContext(Dispatchers.IO) {
                runCatching {
                    resolver().openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                        ?: error("no stream")
                }.isSuccess
            }
            _message.value = if (written) "Theme exportiert." else "Export fehlgeschlagen."
        }
    }

    fun importTheme(uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    resolver().openInputStream(uri)?.use { it.readBytes().decodeToString() }
                }.getOrNull()
            }
            if (text == null) {
                _message.value = "Import fehlgeschlagen."
                return@launch
            }
            // The font path in the file points into the exporting install; keep our own.
            val imported = ThemeConfig.decode(text).copy(fontPath = theme.value.fontPath)
            services.settings.setTheme(imported)
            _message.value = "Theme importiert."
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun resolver() = getApplication<Application>().contentResolver

    private fun manualOrKeep(config: ThemeConfig): ColorMode =
        if (config.colorMode == ColorMode.ExtraDark) ColorMode.ExtraDark else ColorMode.Manual

    private fun update(transform: (ThemeConfig) -> ThemeConfig) {
        viewModelScope.launch { services.settings.setTheme(transform(theme.value)) }
    }
}
