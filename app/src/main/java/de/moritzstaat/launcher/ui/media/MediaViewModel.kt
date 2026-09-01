package de.moritzstaat.launcher.ui.media

import android.app.Application
import android.graphics.Bitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.app.AppKey
import de.moritzstaat.launcher.data.media.MediaState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.transformLatest

/** Media widget and the short-lived music app suggestions after an audio output connects. */
class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as LauncherApplication).services
    private val mediaRepository = services.mediaRepository

    val iconLoader = services.iconLoader

    /**
     * The widget stays for half a minute after a pause, then disappears. Without the delay it
     * would blink away every time a track ends or the user pauses for a moment.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val media: StateFlow<MediaState?> = mediaRepository.state
        .flatMapLatest { state ->
            when {
                state == null -> flowOf(null)
                state.isPlaying -> flowOf(state)
                else -> flow {
                    emit(state)
                    delay(LINGER_AFTER_PAUSE_MS)
                    emit(null)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** The music apps the user configured, resolved against what is actually installed. */
    private val configuredMediaApps: Flow<List<AppEntry>> = combine(
        services.settings.mediaApps,
        services.settings.mediaAppsOnOutputChange,
        services.appIndex.allApps,
    ) { configured, enabled, apps ->
        if (!enabled || configured.isEmpty()) return@combine emptyList()
        val byKey = apps.associateBy { it.key.flatten() }
        configured.mapNotNull { byKey[it] }
    }

    /** True for a short window after every connect event. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val suggestionWindow: Flow<Boolean> =
        services.audioOutputRepository.outputConnected.transformLatest {
            emit(true)
            delay(SUGGESTION_WINDOW_MS)
            emit(false)
        }

    /** Music apps offered right after headphones or a speaker connect; empty again afterwards. */
    val suggestedMediaApps: StateFlow<List<AppEntry>> =
        combine(suggestionWindow, configuredMediaApps) { visible, apps ->
            if (visible) apps else emptyList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** The app that owns the session, so its icon and launch path can be reused. */
    val sessionApp: StateFlow<AppEntry?> = combine(
        mediaRepository.state,
        services.appIndex.allApps,
    ) { state, apps ->
        state ?: return@combine null
        apps.firstOrNull { it.key.packageName == state.packageName }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /**
     * Name of the audio output, re-read on every connect event and whenever the session
     * changes: switching headphones does not touch the media session at all.
     */
    val outputName: StateFlow<String?> = combine(
        services.audioOutputRepository.outputConnected.onStart { emit(Unit) },
        mediaRepository.state,
    ) { _, _ -> services.audioOutputRepository.currentOutputName() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /**
     * Icon of one of the session's own actions, loaded from that app's resources.
     *
     * The same route the icon packs take. A failure simply yields null and the action is then
     * not offered, rather than drawing a blank button.
     */
    suspend fun customActionIcon(packageName: String, iconResId: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val resources = getApplication<Application>().packageManager
                    .getResourcesForApplication(packageName)
                val drawable = ResourcesCompat.getDrawable(resources, iconResId, null)
                drawable?.toBitmap()
            }.getOrNull()
        }

    fun sendCustomAction(action: String) = mediaRepository.sendCustomAction(action)

    fun playOrPause() = mediaRepository.playOrPause()

    fun skipNext() = mediaRepository.skipNext()

    fun skipPrevious() = mediaRepository.skipPrevious()

    fun openSessionApp() {
        sessionApp.value?.let { services.appRepository.launch(it.key) }
    }

    fun launch(appKey: AppKey) {
        services.appRepository.launch(appKey)
    }

    private companion object {
        const val LINGER_AFTER_PAUSE_MS = 30_000L
        const val SUGGESTION_WINDOW_MS = 20_000L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
