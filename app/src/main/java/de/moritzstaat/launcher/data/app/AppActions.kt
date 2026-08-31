package de.moritzstaat.launcher.data.app

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import de.moritzstaat.launcher.data.db.CustomLabelEntity
import de.moritzstaat.launcher.data.db.FavoriteEntity
import de.moritzstaat.launcher.data.db.FolderEntity
import de.moritzstaat.launcher.data.db.FolderItemEntity
import de.moritzstaat.launcher.data.db.HiddenAppEntity
import de.moritzstaat.launcher.data.db.LauncherDatabase

/**
 * Everything the long press menu can do to one app. Kept out of the view models so the
 * settings screens and the backup import can reuse exactly the same rules.
 */
class AppActions(
    context: Context,
    private val database: LauncherDatabase,
    private val appRepository: AppRepository,
) {

    private val appContext = context.applicationContext

    suspend fun isFavorite(appKey: AppKey): Boolean =
        database.favoriteDao().getAll().any { it.appKey == appKey.flatten() }

    /** Returns false when the favourites are already full and nothing was added. */
    suspend fun setFavorite(appKey: AppKey, favorite: Boolean): Boolean {
        val dao = database.favoriteDao()
        val flat = appKey.flatten()
        return if (favorite) {
            if (dao.count() >= MAX_FAVORITES) return false
            dao.insert(FavoriteEntity(flat, dao.count()))
            true
        } else {
            dao.delete(flat)
            true
        }
    }

    /** A blank or null [label] restores the name the system reports. */
    suspend fun rename(appKey: AppKey, label: String?) {
        val dao = database.customLabelDao()
        val flat = appKey.flatten()
        if (label.isNullOrBlank()) dao.delete(flat) else dao.upsert(CustomLabelEntity(flat, label.trim()))
    }

    suspend fun setHidden(appKey: AppKey, hidden: Boolean) {
        val dao = database.hiddenAppDao()
        val flat = appKey.flatten()
        if (hidden) {
            dao.insert(HiddenAppEntity(flat))
            database.favoriteDao().delete(flat)
        } else {
            dao.delete(flat)
        }
    }

    /** An app belongs to at most one folder, so this replaces any previous membership. */
    suspend fun moveToFolder(appKey: AppKey, folderId: Long) {
        val dao = database.folderDao()
        val flat = appKey.flatten()
        dao.removeApp(flat)
        dao.insertItem(FolderItemEntity(folderId, flat, dao.countItems(folderId)))
        pruneEmptyFolders()
    }

    /** Creates a folder and puts the app in it. Returns the new folder id. */
    suspend fun moveToNewFolder(appKey: AppKey, name: String): Long {
        val dao = database.folderDao()
        val folderId = dao.insertFolder(FolderEntity(name = name.trim()))
        moveToFolder(appKey, folderId)
        return folderId
    }

    suspend fun removeFromFolders(appKey: AppKey) {
        database.folderDao().removeApp(appKey.flatten())
        pruneEmptyFolders()
    }

    /** An empty folder is only clutter in the app list. */
    private suspend fun pruneEmptyFolders() {
        val dao = database.folderDao()
        val used = dao.getItems().map { it.folderId }.toHashSet()
        dao.getFolders().filterNot { it.id in used }.forEach { dao.deleteFolder(it.id) }
    }

    fun openAppInfo(appKey: AppKey, sourceBounds: Rect? = null): Boolean =
        appRepository.openAppInfo(appKey, sourceBounds)

    /**
     * Hands the request to the system uninstaller. The launcher never removes anything itself;
     * the confirmation dialog and the decision stay with the user.
     */
    fun requestUninstall(appKey: AppKey): Boolean {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", appKey.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appRepository.userFor(appKey)?.let { intent.putExtra(Intent.EXTRA_USER, it) }
        return runCatching { appContext.startActivity(intent) }.isSuccess
    }

    companion object {
        /** Hard cap on the home screen. More rows and the home screen becomes a second list. */
        const val MAX_FAVORITES = 8
    }
}
