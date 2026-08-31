package de.moritzstaat.launcher.data.app

import de.moritzstaat.launcher.data.db.CustomLabelDao
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
 * removed, sorted once so that every consumer sees the same order.
 */
class AppIndex(
    appRepository: AppRepository,
    customLabelDao: CustomLabelDao,
    hiddenAppDao: HiddenAppDao,
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

    /** What the app list shows. */
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

    /** Occupied sections for the alphabet bar, in list order. */
    val sections: StateFlow<List<String>> = visibleApps
        .map { apps -> AppSorting.sections(apps) { it.label } }
        .flowOn(Dispatchers.Default)
        .stateIn(externalScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
