package de.moritzstaat.launcher.di

import android.content.Context
import de.moritzstaat.launcher.data.app.AppIndex
import de.moritzstaat.launcher.data.app.AppRepository
import de.moritzstaat.launcher.data.db.LauncherDatabase
import de.moritzstaat.launcher.data.icon.IconCache
import de.moritzstaat.launcher.data.icon.IconLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Hand written dependency graph. A launcher has a handful of process wide singletons; a DI
 * framework would cost startup time and one more library for no gain.
 */
class ServiceLocator(context: Context) {

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: LauncherDatabase by lazy { LauncherDatabase.create(context) }

    val appRepository: AppRepository by lazy { AppRepository(context, applicationScope) }

    val appIndex: AppIndex by lazy {
        AppIndex(
            appRepository = appRepository,
            customLabelDao = database.customLabelDao(),
            hiddenAppDao = database.hiddenAppDao(),
            externalScope = applicationScope,
        )
    }

    val iconCache: IconCache by lazy { IconCache() }

    val iconLoader: IconLoader by lazy { IconLoader(context, appRepository, iconCache) }
}
