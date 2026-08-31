package de.moritzstaat.launcher.ui.home

import android.app.Application
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.app.AppActions
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.app.AppKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the home screen: favourites, the full app list and launching. */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as LauncherApplication).services
    private val favoriteDao = services.database.favoriteDao()

    val iconLoader = services.iconLoader

    val shortcutRepository = services.shortcutRepository

    val apps: StateFlow<List<AppEntry>> = services.appIndex.visibleApps

    val sections: StateFlow<List<String>> = services.appIndex.sections

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

    fun launch(appKey: AppKey, sourceBounds: Rect? = null) {
        services.appRepository.launch(appKey, sourceBounds)
    }

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
