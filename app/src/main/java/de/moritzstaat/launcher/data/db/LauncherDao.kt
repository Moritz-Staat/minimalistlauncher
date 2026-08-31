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
