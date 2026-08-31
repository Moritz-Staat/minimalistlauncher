package de.moritzstaat.launcher.data.app

/** A folder together with the apps it holds. */
data class AppFolder(
    val id: Long,
    val name: String,
    val apps: List<AppEntry>,
)

/**
 * One line of the app list. Folders are not a separate screen: they sit in the list exactly
 * where an app of that name would, which is why both share one item type and one sort order.
 */
sealed interface AppListItem {

    val id: String
    val label: String

    data class App(val entry: AppEntry) : AppListItem {
        override val id: String get() = "app:${entry.key.flatten()}"
        override val label: String get() = entry.label
    }

    data class Folder(val folder: AppFolder) : AppListItem {
        override val id: String get() = "folder:${folder.id}"
        override val label: String get() = folder.name
    }
}

/** Section header of the alphabet bar this item belongs to. */
val AppListItem.section: String get() = AppSorting.sectionFor(label)
