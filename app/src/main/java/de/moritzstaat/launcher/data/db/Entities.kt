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
