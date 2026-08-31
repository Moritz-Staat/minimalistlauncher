package de.moritzstaat.launcher.data.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.core.content.getSystemService
import de.moritzstaat.launcher.service.LauncherNotificationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** What the media widget shows. */
data class MediaState(
    val packageName: String,
    val title: String,
    val artist: String,
    val artwork: Bitmap?,
    val isPlaying: Boolean,
    val canSkipNext: Boolean,
    val canSkipPrevious: Boolean,
)

/**
 * The currently interesting media session.
 *
 * Uses the same notification listener component as stage 7, so this needs no extra permission
 * but also stays empty until the notification access is granted.
 */
class MediaRepository(
    context: Context,
    externalScope: CoroutineScope,
) {

    private val appContext = context.applicationContext
    private val sessionManager = appContext.getSystemService<MediaSessionManager>()
    private val listenerComponent =
        ComponentName(appContext, LauncherNotificationListener::class.java)
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var controller: MediaController? = null

    private val sessions: Flow<List<MediaController>> = callbackFlow {
        val manager = sessionManager
        if (manager == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            trySend(controllers.orEmpty())
        }
        val registered = runCatching {
            manager.addOnActiveSessionsChangedListener(listener, listenerComponent, handler)
            trySend(manager.getActiveSessions(listenerComponent).orEmpty())
        }.isSuccess
        if (!registered) trySend(emptyList())
        awaitClose {
            if (registered) runCatching { manager.removeOnActiveSessionsChangedListener(listener) }
        }
    }.conflate()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<MediaState?> = sessions
        .flatMapLatest { controllers -> observe(pickSession(controllers)) }
        .stateIn(externalScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** The playing session wins; otherwise the one the system considers most recent. */
    private fun pickSession(controllers: List<MediaController>): MediaController? =
        controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()

    private fun observe(target: MediaController?): Flow<MediaState?> {
        controller = target
        if (target == null) return flowOf(null)
        return callbackFlow {
            val callback = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    trySend(target.toState())
                }

                override fun onPlaybackStateChanged(playbackState: PlaybackState?) {
                    trySend(target.toState())
                }

                override fun onSessionDestroyed() {
                    trySend(null)
                }
            }
            target.registerCallback(callback, handler)
            trySend(target.toState())
            awaitClose { runCatching { target.unregisterCallback(callback) } }
        }.conflate()
    }

    fun playOrPause() {
        val current = controller ?: return
        if (current.playbackState?.state == PlaybackState.STATE_PLAYING) {
            current.transportControls.pause()
        } else {
            current.transportControls.play()
        }
    }

    fun skipNext() {
        controller?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        controller?.transportControls?.skipToPrevious()
    }

    /** Opens the app the session belongs to. */
    fun sessionPackage(): String? = controller?.packageName

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private fun MediaController.toState(): MediaState? {
    val metadata = metadata ?: return null
    val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
        ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        ?: return null
    val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
        ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        ?: ""
    val artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
    val actions = playbackState?.actions ?: 0L

    return MediaState(
        packageName = packageName,
        title = title,
        artist = artist,
        artwork = artwork,
        isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
        canSkipNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
        canSkipPrevious = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
    )
}
