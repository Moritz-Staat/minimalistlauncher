package de.moritzstaat.launcher.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
 * Stage 12 fills this out with the theming options; stage 8 only needs the media entries.
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

    suspend fun setMediaApps(appKeys: Set<String>) {
        store.edit { it[KEY_MEDIA_APPS] = appKeys }
    }

    suspend fun setMediaAppsOnOutputChange(enabled: Boolean) {
        store.edit { it[KEY_MEDIA_ON_OUTPUT] = enabled }
    }

    private companion object {
        val KEY_MEDIA_APPS = stringSetPreferencesKey("media_apps")
        val KEY_MEDIA_ON_OUTPUT = booleanPreferencesKey("media_apps_on_output_change")
    }
}
