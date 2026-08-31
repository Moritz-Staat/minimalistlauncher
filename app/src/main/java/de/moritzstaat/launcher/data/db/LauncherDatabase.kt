package de.moritzstaat.launcher.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        HiddenAppEntity::class,
        CustomLabelEntity::class,
        IconOverrideEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LauncherDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
    abstract fun hiddenAppDao(): HiddenAppDao
    abstract fun customLabelDao(): CustomLabelDao
    abstract fun iconOverrideDao(): IconOverrideDao

    companion object {
        fun create(context: Context): LauncherDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                LauncherDatabase::class.java,
                "launcher.db",
            ).build()
    }
}
