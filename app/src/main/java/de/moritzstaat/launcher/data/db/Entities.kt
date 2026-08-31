package de.moritzstaat.launcher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Favourites are an ordered, hand-curated list; [position] is the user's own order. */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val appKey: String,
    val position: Int,
)

/** Apps the user removed from the list. They stay installed and stay searchable if enabled. */
@Entity(tableName = "hidden_apps")
data class HiddenAppEntity(
    @PrimaryKey val appKey: String,
)

/** User supplied name that replaces the label reported by the system. */
@Entity(tableName = "custom_labels")
data class CustomLabelEntity(
    @PrimaryKey val appKey: String,
    val label: String,
)

/**
 * Icon chosen by hand. [iconPackPackage] null means "back to the original icon"; a value
 * plus [drawableName] points at one drawable inside an installed icon pack.
 */
@Entity(tableName = "icon_overrides")
data class IconOverrideEntity(
    @PrimaryKey val appKey: String,
    val iconPackPackage: String?,
    val drawableName: String?,
)

/**
 * Per app notification preference. Only rows the user actually changed are stored; everything
 * absent means "show the content".
 */
@Entity(tableName = "notification_prefs")
data class NotificationPrefEntity(
    @PrimaryKey val packageName: String,
    val redacted: Boolean,
)

/**
 * One bound app widget. [appWidgetId] is the id the [android.appwidget.AppWidgetHost] handed
 * out; it must be deleted through the host when this row goes away, otherwise the id leaks and
 * survives restarts.
 *
 * [ownerKey] is null for the home screen slots and carries the flattened AppKey for widgets
 * that live inside an app pop-up.
 */
@Entity(tableName = "widgets")
data class WidgetEntity(
    @PrimaryKey val appWidgetId: Int,
    val slot: String,
    val ownerKey: String?,
    val position: Int,
)

/** A folder. It sits in the app list alphabetically, exactly where an app of that name would. */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

/** Membership of one app in one folder. Apps inside a folder leave the top level list. */
@Entity(tableName = "folder_items", primaryKeys = ["folderId", "appKey"])
data class FolderItemEntity(
    val folderId: Long,
    val appKey: String,
    val position: Int,
)
