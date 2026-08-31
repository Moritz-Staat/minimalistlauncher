package de.moritzstaat.launcher.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [
        FavoriteEntity::class,
        HiddenAppEntity::class,
        CustomLabelEntity::class,
        IconOverrideEntity::class,
        NotificationPrefEntity::class,
        WidgetEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class LauncherDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
    abstract fun hiddenAppDao(): HiddenAppDao
    abstract fun customLabelDao(): CustomLabelDao
    abstract fun iconOverrideDao(): IconOverrideDao
    abstract fun notificationPrefDao(): NotificationPrefDao
    abstract fun widgetDao(): WidgetDao

    companion object {
        fun create(context: Context): LauncherDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                LauncherDatabase::class.java,
                "launcher.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

        /** Stage 7 added the per app notification preference. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notification_prefs` (" +
                        "`packageName` TEXT NOT NULL, " +
                        "`redacted` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`packageName`))",
                )
            }
        }

        /** Stage 9 added the bound app widgets. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `widgets` (" +
                        "`appWidgetId` INTEGER NOT NULL, " +
                        "`slot` TEXT NOT NULL, " +
                        "`ownerKey` TEXT, " +
                        "`position` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`appWidgetId`))",
                )
            }
        }
    }
}
