package de.moritzstaat.launcher.data.app

import de.moritzstaat.launcher.data.db.CustomLabelDao
import de.moritzstaat.launcher.data.db.FolderDao
import de.moritzstaat.launcher.data.db.HiddenAppDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The installed apps as the user has arranged them: custom labels applied, hidden apps
 * removed, folders folded in, sorted once so that every consumer sees the same order.
 */
class AppIndex(
    appRepository: AppRepository,
    customLabelDao: CustomLabelDao,
    hiddenAppDao: HiddenAppDao,
    folderDao: FolderDao,
    externalScope: CoroutineScope,
) {

    /** Every installed app including hidden ones; needed by search and by the settings. */
    val allApps: StateFlow<List<AppEntry>> = combine(
        appRepository.installedApps,
        customLabelDao.observeAll(),
    ) { apps, labels ->
        if (labels.isEmpty()) return@combine apps
        val byKey = labels.associate { it.appKey to it.label }
        val relabelled = apps.map { entry ->
            val custom = byKey[entry.key.flatten()]
            if (custom.isNullOrBlank()) entry else entry.copy(label = custom)
        }
        AppSorting.sorted(relabelled) { it.label }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(externalScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** Apps the user did not hide. */
    val visibleApps: StateFlow<List<AppEntry>> = combine(
        allApps,
        hiddenAppDao.observeAll(),
    ) { apps, hidden ->
        if (hidden.isEmpty()) apps else {
            val hiddenKeys = hidden.toHashSet()
            apps.filterNot { it.key.flatten() in hiddenKeys }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(externalScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** Folders with their apps resolved; folders whose apps all vanished are dropped. */
    val folders: StateFlow<List<AppFolder>> = combine(
        visibleApps,
        folderDao.observeFolders(),
        folderDao.observeItems(),
    ) { apps, folders, items ->
        if (folders.isEmpty()) return@combine emptyList()
        val byKey = apps.associateBy { it.key.flatten() }
        val itemsByFolder = items.groupBy { it.folderId }
        folders.mapNotNull { folder ->
            val members = itemsByFolder[folder.id]
                .orEmpty()
                .mapNotNull { byKey[it.appKey] }
            if (members.isEmpty()) null else AppFolder(folder.id, folder.name, members)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(externalScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /**
     * What the app list shows: apps that are not in a folder, plus the folders themselves,
     * all in one alphabetical order.
     */
    val visibleItems: StateFlow<List<AppListItem>> = combine(
        visibleApps,
        folders,
    ) { apps, folders ->
        val grouped = folders.flatMapTo(HashSet()) { folder ->
            folder.apps.map { it.key.flatten() }
        }
        val items = ArrayList<AppListItem>(apps.size + folders.size)
        apps.forEach { entry ->
            if (entry.key.flatten() !in grouped) items += AppListItem.App(entry)
        }
        folders.forEach { items += AppListItem.Folder(it) }
        AppSorting.sorted(items) { it.label }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(externalScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** Occupied sections for the alphabet bar, in list order. */
    val sections: StateFlow<List<String>> = visibleItems
        .map { items -> AppSorting.sections(items) { it.label } }
        .flowOn(Dispatchers.Default)
        .stateIn(externalScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
