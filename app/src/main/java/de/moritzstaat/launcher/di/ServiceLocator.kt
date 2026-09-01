package de.moritzstaat.launcher.di

import android.content.Context
import de.moritzstaat.launcher.data.app.AppActions
import de.moritzstaat.launcher.data.app.AppIndex
import de.moritzstaat.launcher.data.app.AppRepository
import de.moritzstaat.launcher.data.app.ShortcutRepository
import de.moritzstaat.launcher.data.calendar.CalendarRepository
import de.moritzstaat.launcher.data.search.ContactSearch
import de.moritzstaat.launcher.data.search.SearchEngine
import de.moritzstaat.launcher.data.db.LauncherDatabase
import de.moritzstaat.launcher.data.icon.IconCache
import de.moritzstaat.launcher.data.icon.IconConfig
import de.moritzstaat.launcher.data.icon.IconOverride
import de.moritzstaat.launcher.data.icon.IconPackRepository
import de.moritzstaat.launcher.data.icon.IconStyle
import de.moritzstaat.launcher.data.media.AudioOutputRepository
import de.moritzstaat.launcher.data.media.MediaRepository
import de.moritzstaat.launcher.data.settings.FontStore
import de.moritzstaat.launcher.data.settings.LauncherSettings
import de.moritzstaat.launcher.data.settings.ThemeConfig
import de.moritzstaat.launcher.data.weather.LocationProvider
import de.moritzstaat.launcher.data.weather.OpenMeteoClient
import de.moritzstaat.launcher.data.weather.WeatherRepository
import de.moritzstaat.launcher.data.widget.WidgetHostController
import de.moritzstaat.launcher.data.notification.NotificationRepository
import de.moritzstaat.launcher.data.icon.IconLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
            folderDao = database.folderDao(),
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

    val fontStore: FontStore by lazy { FontStore(context) }

    val calendarRepository: CalendarRepository by lazy {
        CalendarRepository(context, applicationScope)
    }

    val locationProvider: LocationProvider by lazy { LocationProvider(context) }

    val weatherRepository: WeatherRepository by lazy {
        WeatherRepository(
            settings = settings,
            location = locationProvider,
            client = OpenMeteoClient(),
            scope = applicationScope,
        )
    }

    /**
     * The active theme. Held eagerly so the first frame after a cold start already draws in the
     * user's colours instead of flashing the defaults.
     */
    val theme: StateFlow<ThemeConfig> by lazy {
        settings.theme.stateIn(applicationScope, SharingStarted.Eagerly, ThemeConfig())
    }

    val widgetHost: WidgetHostController by lazy { WidgetHostController(context) }

    val iconCache: IconCache by lazy { IconCache() }

    val iconPackRepository: IconPackRepository by lazy { IconPackRepository(context) }

    /**
     * Everything that decides how an icon looks, folded into one flow. Loading an icon pack
     * touches the file system, so this runs on IO and is shared by every consumer.
     */
    val iconConfig: StateFlow<IconConfig> by lazy {
        combine(
            settings.iconStyle,
            settings.iconPackPackage,
            database.iconOverrideDao().observeAll(),
        ) { style, packName, overrides ->
            val pack = packName.takeIf { it.isNotBlank() }?.let { iconPackRepository.load(it) }
            IconConfig(
                style = IconStyle.fromStorage(style),
                pack = pack,
                overrides = overrides.associate {
                    it.appKey to IconOverride(it.iconPackPackage, it.drawableName)
                },
            )
        }
            .flowOn(Dispatchers.IO)
            .stateIn(applicationScope, SharingStarted.Eagerly, IconConfig())
    }

    val iconLoader: IconLoader by lazy {
        IconLoader(context, appRepository, iconCache, iconPackRepository, iconConfig)
    }
}
