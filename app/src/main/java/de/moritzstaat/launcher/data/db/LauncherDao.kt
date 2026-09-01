package de.moritzstaat.launcher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY position ASC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY position ASC")
    suspend fun getAll(): List<FavoriteEntity>

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE appKey = :appKey")
    suspend fun delete(appKey: String)

    @Query("DELETE FROM favorites")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favorites: List<FavoriteEntity>)

    /** Rewrites the whole order in one transaction so the list never renders half sorted. */
    @Transaction
    suspend fun replaceOrder(appKeys: List<String>) {
        clear()
        insertAll(appKeys.mapIndexed { index, key -> FavoriteEntity(key, index) })
    }
}

@Dao
interface HiddenAppDao {

    @Query("SELECT appKey FROM hidden_apps")
    fun observeAll(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(hidden: HiddenAppEntity)

    @Query("DELETE FROM hidden_apps WHERE appKey = :appKey")
    suspend fun delete(appKey: String)

    @Query("DELETE FROM hidden_apps")
    suspend fun clear()

    @Query("SELECT appKey FROM hidden_apps")
    suspend fun getAll(): List<String>
}

@Dao
interface CustomLabelDao {

    @Query("SELECT * FROM custom_labels")
    fun observeAll(): Flow<List<CustomLabelEntity>>

    @Query("SELECT * FROM custom_labels")
    suspend fun getAll(): List<CustomLabelEntity>

    @Upsert
    suspend fun upsert(label: CustomLabelEntity)

    @Query("DELETE FROM custom_labels WHERE appKey = :appKey")
    suspend fun delete(appKey: String)

    @Query("DELETE FROM custom_labels")
    suspend fun clear()
}

@Dao
interface IconOverrideDao {

    @Query("SELECT * FROM icon_overrides")
    fun observeAll(): Flow<List<IconOverrideEntity>>

    @Query("SELECT * FROM icon_overrides")
    suspend fun getAll(): List<IconOverrideEntity>

    @Upsert
    suspend fun upsert(override: IconOverrideEntity)

    @Query("DELETE FROM icon_overrides WHERE appKey = :appKey")
    suspend fun delete(appKey: String)

    @Query("DELETE FROM icon_overrides")
    suspend fun clear()
}

@Dao
interface NotificationPrefDao {

    @Query("SELECT packageName FROM notification_prefs WHERE redacted = 1")
    fun observeRedacted(): Flow<List<String>>

    @Query("SELECT * FROM notification_prefs")
    suspend fun getAll(): List<NotificationPrefEntity>

    @Upsert
    suspend fun upsert(pref: NotificationPrefEntity)

    @Query("DELETE FROM notification_prefs WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM notification_prefs")
    suspend fun clear()
}

@Dao
interface WidgetDao {

    @Query("SELECT * FROM widgets ORDER BY position ASC")
    fun observeAll(): Flow<List<WidgetEntity>>

    @Query("SELECT * FROM widgets ORDER BY position ASC")
    suspend fun getAll(): List<WidgetEntity>

    @Query("SELECT COUNT(*) FROM widgets WHERE slot = :slot AND (ownerKey IS :ownerKey)")
    suspend fun countIn(slot: String, ownerKey: String?): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(widget: WidgetEntity)

    @Query("DELETE FROM widgets WHERE appWidgetId = :appWidgetId")
    suspend fun delete(appWidgetId: Int)
}

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folder_items ORDER BY position ASC")
    fun observeItems(): Flow<List<FolderItemEntity>>

    @Query("SELECT * FROM folders ORDER BY name ASC")
    suspend fun getFolders(): List<FolderEntity>

    @Query("SELECT * FROM folder_items ORDER BY position ASC")
    suspend fun getItems(): List<FolderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: FolderItemEntity)

    @Query("SELECT COUNT(*) FROM folder_items WHERE folderId = :folderId")
    suspend fun countItems(folderId: Long): Int

    @Query("DELETE FROM folder_items WHERE appKey = :appKey")
    suspend fun removeApp(appKey: String)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)

    @Query("DELETE FROM folders")
    suspend fun clearFolders()

    @Query("DELETE FROM folder_items")
    suspend fun clearItems()

    @Query("DELETE FROM folder_items WHERE folderId = :folderId")
    suspend fun clearFolder(folderId: Long)

    @Query("UPDATE folders SET name = :name WHERE id = :folderId")
    suspend fun renameFolder(folderId: Long, name: String)

    /** Removing the last app also removes the folder; an empty folder is only clutter. */
    @Transaction
    suspend fun removeAppAndPrune(appKey: String, folderId: Long) {
        removeApp(appKey)
        if (countItems(folderId) == 0) deleteFolder(folderId)
    }
}

@Dao
interface AppOpenDao {

    @Query("SELECT * FROM app_opens WHERE dayEpoch = :dayEpoch")
    suspend fun forDay(dayEpoch: Long): List<AppOpenEntity>

    /** One statement, so two launches in the same second cannot lose a count. */
    @Query(
        "INSERT INTO app_opens (packageName, dayEpoch, count) VALUES (:packageName, :dayEpoch, 1) " +
            "ON CONFLICT(packageName, dayEpoch) DO UPDATE SET count = count + 1",
    )
    suspend fun increment(packageName: String, dayEpoch: Long)

    @Query("DELETE FROM app_opens WHERE dayEpoch < :dayEpoch")
    suspend fun deleteBefore(dayEpoch: Long)
}
