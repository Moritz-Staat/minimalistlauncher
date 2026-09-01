package de.moritzstaat.launcher.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.moritzstaat.launcher.data.weather.TemperatureUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "launcher_settings",
)

/**
 * Global launcher settings. Everything that describes one app lives in Room instead; this is
 * for the switches that apply to the whole launcher.
 *
 * The theme is read and written as a whole; everything else is one entry per switch.
 */
class LauncherSettings(context: Context) {

    private val store = context.applicationContext.settingsStore

    private val preferences: Flow<Preferences> = store.data.catch { error ->
        // A corrupt preferences file must not take the home screen down with it.
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    /** Apps offered when headphones or a speaker connect. Flattened AppKeys. */
    val mediaApps: Flow<Set<String>> = preferences.map { it[KEY_MEDIA_APPS].orEmpty() }

    /** Whether connecting an audio output should surface those apps at all. */
    val mediaAppsOnOutputChange: Flow<Boolean> =
        preferences.map { it[KEY_MEDIA_ON_OUTPUT] ?: true }

    /** Name of the [de.moritzstaat.launcher.data.icon.IconStyle] entry currently in use. */
    val iconStyle: Flow<String> = preferences.map { it[KEY_ICON_STYLE] ?: "Original" }

    /** Package of the selected icon pack, empty when none is chosen. */
    val iconPackPackage: Flow<String> = preferences.map { it[KEY_ICON_PACK].orEmpty() }

    /** The whole theme, assembled from the individual preference entries. */
    val theme: Flow<ThemeConfig> = preferences.map { prefs ->
        val defaults = ThemeConfig()
        ThemeConfig(
            clockStyle = prefs[KEY_CLOCK_STYLE]
                ?.let { name -> ClockStyle.entries.firstOrNull { it.name == name } }
                ?: defaults.clockStyle,
            hourFormat = prefs[KEY_HOUR_FORMAT]
                ?.let { name -> HourFormat.entries.firstOrNull { it.name == name } }
                ?: defaults.hourFormat,
            showDate = prefs[KEY_SHOW_DATE] ?: defaults.showDate,
            colorMode = prefs[KEY_COLOR_MODE]
                ?.let { name -> ColorMode.entries.firstOrNull { it.name == name } }
                ?: defaults.colorMode,
            accentArgb = prefs[KEY_ACCENT] ?: defaults.accentArgb,
            darkTheme = prefs[KEY_DARK] ?: defaults.darkTheme,
            wallpaperDim = prefs[KEY_DIM] ?: defaults.wallpaperDim,
            wallpaperBlur = prefs[KEY_BLUR] ?: defaults.wallpaperBlur,
            hideStatusBar = prefs[KEY_HIDE_STATUS_BAR] ?: defaults.hideStatusBar,
            fontPath = prefs[KEY_FONT_PATH] ?: defaults.fontPath,
        )
    }

    /** Writes a whole theme at once; used by the presets and by the theme import. */
    suspend fun setTheme(theme: ThemeConfig) {
        store.edit { prefs ->
            prefs[KEY_CLOCK_STYLE] = theme.clockStyle.name
            prefs[KEY_HOUR_FORMAT] = theme.hourFormat.name
            prefs[KEY_SHOW_DATE] = theme.showDate
            prefs[KEY_COLOR_MODE] = theme.colorMode.name
            prefs[KEY_ACCENT] = theme.accentArgb
            prefs[KEY_DARK] = theme.darkTheme
            prefs[KEY_DIM] = theme.wallpaperDim
            prefs[KEY_BLUR] = theme.wallpaperBlur
            prefs[KEY_HIDE_STATUS_BAR] = theme.hideStatusBar
            prefs[KEY_FONT_PATH] = theme.fontPath
        }
    }

    /** Whether the next calendar entries appear on the home screen at all. */
    val calendarEnabled: Flow<Boolean> = preferences.map { it[KEY_CALENDAR_ENABLED] ?: false }

    /** Ids of the calendars to read. Empty means every visible calendar. */
    val calendarIds: Flow<Set<String>> = preferences.map { it[KEY_CALENDAR_IDS].orEmpty() }

    val weatherEnabled: Flow<Boolean> = preferences.map { it[KEY_WEATHER_ENABLED] ?: false }

    val temperatureUnit: Flow<TemperatureUnit> = preferences.map { prefs ->
        prefs[KEY_TEMPERATURE_UNIT]
            ?.let { name -> TemperatureUnit.entries.firstOrNull { it.name == name } }
            ?: TemperatureUnit.Celsius
    }

    /** The last reading, as JSON. Empty until the first successful request. */
    val weatherCache: Flow<String> = preferences.map { it[KEY_WEATHER_CACHE].orEmpty() }

    suspend fun setCalendarEnabled(enabled: Boolean) {
        store.edit { it[KEY_CALENDAR_ENABLED] = enabled }
    }

    suspend fun setCalendarIds(ids: Set<String>) {
        store.edit { it[KEY_CALENDAR_IDS] = ids }
    }

    suspend fun setWeatherEnabled(enabled: Boolean) {
        store.edit { it[KEY_WEATHER_ENABLED] = enabled }
    }

    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        store.edit { it[KEY_TEMPERATURE_UNIT] = unit.name }
    }

    suspend fun setWeatherCache(json: String) {
        store.edit { it[KEY_WEATHER_CACHE] = json }
    }

    suspend fun setIconStyle(style: String) {
        store.edit { it[KEY_ICON_STYLE] = style }
    }

    suspend fun setIconPackPackage(packageName: String) {
        store.edit { it[KEY_ICON_PACK] = packageName }
    }

    suspend fun setMediaApps(appKeys: Set<String>) {
        store.edit { it[KEY_MEDIA_APPS] = appKeys }
    }

    suspend fun setMediaAppsOnOutputChange(enabled: Boolean) {
        store.edit { it[KEY_MEDIA_ON_OUTPUT] = enabled }
    }

    private companion object {
        val KEY_MEDIA_APPS = stringSetPreferencesKey("media_apps")
        val KEY_MEDIA_ON_OUTPUT = booleanPreferencesKey("media_apps_on_output_change")
        val KEY_ICON_STYLE = stringPreferencesKey("icon_style")
        val KEY_ICON_PACK = stringPreferencesKey("icon_pack")
        val KEY_CLOCK_STYLE = stringPreferencesKey("clock_style")
        val KEY_HOUR_FORMAT = stringPreferencesKey("hour_format")
        val KEY_SHOW_DATE = booleanPreferencesKey("show_date")
        val KEY_COLOR_MODE = stringPreferencesKey("color_mode")
        val KEY_ACCENT = intPreferencesKey("accent_argb")
        val KEY_DARK = booleanPreferencesKey("dark_theme")
        val KEY_DIM = floatPreferencesKey("wallpaper_dim")
        val KEY_BLUR = floatPreferencesKey("wallpaper_blur")
        val KEY_HIDE_STATUS_BAR = booleanPreferencesKey("hide_status_bar")
        val KEY_FONT_PATH = stringPreferencesKey("font_path")
        val KEY_CALENDAR_ENABLED = booleanPreferencesKey("calendar_enabled")
        val KEY_CALENDAR_IDS = stringSetPreferencesKey("calendar_ids")
        val KEY_WEATHER_ENABLED = booleanPreferencesKey("weather_enabled")
        val KEY_TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit")
        val KEY_WEATHER_CACHE = stringPreferencesKey("weather_cache")
    }
}
