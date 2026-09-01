package de.moritzstaat.launcher.data.backup

import de.moritzstaat.launcher.data.db.CustomLabelEntity
import de.moritzstaat.launcher.data.db.FavoriteEntity
import de.moritzstaat.launcher.data.db.FolderEntity
import de.moritzstaat.launcher.data.db.FolderItemEntity
import de.moritzstaat.launcher.data.db.IconOverrideEntity
import de.moritzstaat.launcher.data.db.HiddenAppEntity
import de.moritzstaat.launcher.data.db.LauncherDatabase
import de.moritzstaat.launcher.data.db.NotificationPrefEntity
import de.moritzstaat.launcher.data.settings.LauncherSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads and writes the whole setup as one file.
 *
 * A restore replaces what it covers rather than merging into it: two backups merged would give
 * a state that was never on any device.
 */
class BackupRepository(
    private val database: LauncherDatabase,
    private val settings: LauncherSettings,
) {

    suspend fun create(): LauncherBackup = withContext(Dispatchers.IO) {
        val folders = database.folderDao().getFolders()
        val items = database.folderDao().getItems()

        LauncherBackup(
            preferences = settings.exportPreferences(),
            favorites = database.favoriteDao().getAll().sortedBy { it.position }
                .map { it.appKey },
            hidden = database.hiddenAppDao().getAll(),
            labels = database.customLabelDao().getAll().associate { it.appKey to it.label },
            icons = database.iconOverrideDao().getAll().mapNotNull { override ->
                BackupIcon(
                    appKey = override.appKey,
                    iconPackPackage = override.iconPackPackage ?: return@mapNotNull null,
                    drawableName = override.drawableName ?: return@mapNotNull null,
                )
            },
            notificationRedacted = database.notificationPrefDao().getAll()
                .filter { it.redacted }
                .map { it.packageName },
            folders = folders.map { folder ->
                BackupFolder(
                    name = folder.name,
                    apps = items.filter { it.folderId == folder.id }
                        .sortedBy { it.position }
                        .map { it.appKey },
                )
            },
        )
    }

    /**
     * @return the number of restored entries, for the line the settings show afterwards.
     */
    suspend fun restore(backup: LauncherBackup): Int = withContext(Dispatchers.IO) {
        settings.importPreferences(backup.preferences)

        database.favoriteDao().clear()
        database.favoriteDao().insertAll(
            backup.favorites.mapIndexed { index, appKey -> FavoriteEntity(appKey, index) },
        )

        database.hiddenAppDao().clear()
        backup.hidden.forEach { database.hiddenAppDao().insert(HiddenAppEntity(it)) }

        database.customLabelDao().clear()
        backup.labels.forEach { (appKey, label) ->
            database.customLabelDao().upsert(CustomLabelEntity(appKey, label))
        }

        database.iconOverrideDao().clear()
        backup.icons.forEach { icon ->
            database.iconOverrideDao().upsert(
                IconOverrideEntity(icon.appKey, icon.iconPackPackage, icon.drawableName),
            )
        }

        database.notificationPrefDao().clear()
        backup.notificationRedacted.forEach {
            database.notificationPrefDao().upsert(NotificationPrefEntity(it, redacted = true))
        }

        // Folder ids are handed out by the database, so the folders are recreated rather than
        // restored under their old ids.
        database.folderDao().clearItems()
        database.folderDao().clearFolders()
        backup.folders.forEach { folder ->
            val id = database.folderDao().insertFolder(FolderEntity(0, folder.name))
            folder.apps.forEachIndexed { index, appKey ->
                database.folderDao().insertItem(FolderItemEntity(id, appKey, index))
            }
        }

        backup.favorites.size + backup.hidden.size + backup.labels.size + backup.icons.size +
            backup.notificationRedacted.size + backup.folders.size
    }
}
