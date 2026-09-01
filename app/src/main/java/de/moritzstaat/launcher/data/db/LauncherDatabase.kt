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
        FolderEntity::class,
        FolderItemEntity::class,
        AppOpenEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class LauncherDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
    abstract fun hiddenAppDao(): HiddenAppDao
    abstract fun customLabelDao(): CustomLabelDao
    abstract fun iconOverrideDao(): IconOverrideDao
    abstract fun notificationPrefDao(): NotificationPrefDao
    abstract fun widgetDao(): WidgetDao
    abstract fun folderDao(): FolderDao
    abstract fun appOpenDao(): AppOpenDao

    companion object {
        fun create(context: Context): LauncherDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                LauncherDatabase::class.java,
                "launcher.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()

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

        /** Stage 15 added the per day open counter behind the usage breaker. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_opens` (" +
                        "`packageName` TEXT NOT NULL, " +
                        "`dayEpoch` INTEGER NOT NULL, " +
                        "`count` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`packageName`, `dayEpoch`))",
                )
            }
        }

        /** Stage 10 added folders. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `folders` (" +
                        "`id` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `folder_items` (" +
                        "`folderId` INTEGER NOT NULL, " +
                        "`appKey` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`folderId`, `appKey`))",
                )
            }
        }
    }
}
