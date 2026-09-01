package de.moritzstaat.launcher.ui.home

import android.app.Application
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.app.AppActions
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.app.AppFolder
import de.moritzstaat.launcher.data.app.AppKey
import de.moritzstaat.launcher.data.app.AppListItem
import de.moritzstaat.launcher.data.notification.NotificationSummary
import de.moritzstaat.launcher.service.LauncherNotificationListener
import de.moritzstaat.launcher.ui.usage.PauseRequest
import android.app.PendingIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the home screen: favourites, the full app list and launching. */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as LauncherApplication).services
    private val favoriteDao = services.database.favoriteDao()

    private val _pause = MutableStateFlow<PauseRequest?>(null)

    val iconLoader = services.iconLoader

    val shortcutRepository = services.shortcutRepository

    val apps: StateFlow<List<AppEntry>> = services.appIndex.visibleApps

    /** What the list actually renders: ungrouped apps plus folders, in one alphabet. */
    val items: StateFlow<List<AppListItem>> = services.appIndex.visibleItems

    val sections: StateFlow<List<String>> = services.appIndex.sections

    val folders: StateFlow<List<AppFolder>> = services.appIndex.folders

    /**
     * Favourites keep the order the user gave them, not the alphabet, and are capped so the
     * home screen never turns into a second app list.
     */
    val favorites: StateFlow<List<AppEntry>> = combine(
        services.appIndex.allApps,
        favoriteDao.observeAll(),
    ) { apps, favorites ->
        val byKey = apps.associateBy { it.key.flatten() }
        favorites.asSequence()
            .mapNotNull { byKey[it.appKey] }
            .take(MAX_FAVORITES)
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** Notification previews, keyed by package. Empty while the access is not granted. */
    val notifications: StateFlow<Map<String, NotificationSummary>> =
        services.notificationRepository.summaries

    /** Opens what the notification points at; falls back to starting the app. */
    fun openNotification(summary: NotificationSummary) {
        val intent = summary.contentIntent
        if (intent == null) {
            services.appRepository.installedApps.value
                .firstOrNull { it.key.packageName == summary.packageName }
                ?.let { services.appRepository.launch(it.key) }
            return
        }
        try {
            intent.send()
        } catch (_: PendingIntent.CanceledException) {
            // The notification vanished between rendering and tapping; nothing to open.
        }
    }

    fun dismissNotification(summary: NotificationSummary) {
        LauncherNotificationListener.dismissAll(summary.keys)
    }

    /**
     * Every launch the launcher makes goes through here, which is also where the usage breaker
     * gets its one chance to ask. The check is a lookup in memory: nothing may delay a tap.
     */
    fun launch(appKey: AppKey, sourceBounds: Rect? = null) {
        val usage = services.usageRepository
        if (usage.shouldPause(appKey.packageName)) {
            _pause.value = PauseRequest(
                appKey = appKey,
                label = labelOf(appKey),
                opensToday = usage.opensToday(appKey.packageName),
                pauseSeconds = usage.config.value.pauseSeconds,
                sourceBounds = sourceBounds,
            )
            return
        }
        start(appKey, sourceBounds)
    }

    /** Set while the pause screen is up; null the rest of the time. */
    val pause: StateFlow<PauseRequest?> get() = _pause.asStateFlow()

    fun confirmPause() {
        val request = _pause.value ?: return
        _pause.value = null
        start(request.appKey, request.sourceBounds)
    }

    fun dismissPause() {
        _pause.value = null
    }

    /** Re-reads the open counters; called whenever the launcher comes back to the front. */
    fun refreshUsage() = services.usageRepository.refresh()

    private fun start(appKey: AppKey, sourceBounds: Rect?) {
        services.usageRepository.registerOpen(appKey.packageName)
        services.appRepository.launch(appKey, sourceBounds)
    }

    private fun labelOf(appKey: AppKey): String =
        apps.value.firstOrNull { it.key == appKey }?.label ?: appKey.packageName

    fun setFavorite(appKey: AppKey, favorite: Boolean) {
        viewModelScope.launch {
            val flat = appKey.flatten()
            if (favorite) {
                if (favoriteDao.count() >= MAX_FAVORITES) return@launch
                favoriteDao.insert(
                    de.moritzstaat.launcher.data.db.FavoriteEntity(flat, favoriteDao.count()),
                )
            } else {
                favoriteDao.delete(flat)
            }
        }
    }

    fun reorderFavorites(appKeys: List<String>) {
        viewModelScope.launch { favoriteDao.replaceOrder(appKeys.take(MAX_FAVORITES)) }
    }

    private companion object {
        const val MAX_FAVORITES = AppActions.MAX_FAVORITES
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
