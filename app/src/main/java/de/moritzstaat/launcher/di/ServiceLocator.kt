package de.moritzstaat.launcher.di

import android.content.Context
import de.moritzstaat.launcher.data.app.AppActions
import de.moritzstaat.launcher.data.app.AppIndex
import de.moritzstaat.launcher.data.app.AppRepository
import de.moritzstaat.launcher.data.app.ShortcutRepository
import de.moritzstaat.launcher.data.search.ContactSearch
import de.moritzstaat.launcher.data.search.SearchEngine
import de.moritzstaat.launcher.data.db.LauncherDatabase
import de.moritzstaat.launcher.data.icon.IconCache
import de.moritzstaat.launcher.data.media.AudioOutputRepository
import de.moritzstaat.launcher.data.media.MediaRepository
import de.moritzstaat.launcher.data.settings.LauncherSettings
import de.moritzstaat.launcher.data.widget.WidgetHostController
import de.moritzstaat.launcher.data.notification.NotificationRepository
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

    val shortcutRepository: ShortcutRepository by lazy {
        ShortcutRepository(context, appRepository)
    }

    val contactSearch: ContactSearch by lazy { ContactSearch(context) }

    val searchEngine: SearchEngine by lazy { SearchEngine(shortcutRepository, contactSearch) }

    val appActions: AppActions by lazy { AppActions(context, database, appRepository) }

    /**
     * Fed by the notification listener service. Created eagerly enough that the service can
     * publish into it the moment the system binds it, which can happen before any UI exists.
     */
    val notificationRepository: NotificationRepository by lazy { NotificationRepository() }

    val mediaRepository: MediaRepository by lazy { MediaRepository(context, applicationScope) }

    val audioOutputRepository: AudioOutputRepository by lazy { AudioOutputRepository(context) }

    val settings: LauncherSettings by lazy { LauncherSettings(context) }

    val widgetHost: WidgetHostController by lazy { WidgetHostController(context) }

    val iconCache: IconCache by lazy { IconCache() }

    val iconLoader: IconLoader by lazy { IconLoader(context, appRepository, iconCache) }
}
