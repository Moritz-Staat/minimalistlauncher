package de.moritzstaat.launcher.ui.home

import android.graphics.Rect
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.app.AppKey
import de.moritzstaat.launcher.data.icon.IconLoader
import de.moritzstaat.launcher.ui.applist.AppRow

/**
 * The favourites, in the user's own order. A plain Column, not a list: at most
 * [HomeViewModel.MAX_FAVORITES] rows, so nothing has to be recycled and drag and drop in
 * stage 6 stays simple.
 */
@Composable
fun FavoritesColumn(
    favorites: List<AppEntry>,
    iconLoader: IconLoader,
    onLaunch: (AppKey, Rect?) -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (AppKey, Rect?) -> Unit = { _, _ -> },
    notificationPreviews: Map<String, String> = emptyMap(),
) {
    Column(modifier = modifier.fillMaxWidth()) {
        favorites.forEach { entry ->
            AppRow(
                entry = entry,
                iconLoader = iconLoader,
                notificationPreview = notificationPreviews[entry.key.flatten()],
                onClick = { bounds -> onLaunch(entry.key, bounds) },
                onLongClick = { bounds -> onLongPress(entry.key, bounds) },
            )
        }
    }
}
