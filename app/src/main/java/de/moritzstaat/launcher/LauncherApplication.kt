package de.moritzstaat.launcher

import android.app.Application
import android.content.res.Configuration
import de.moritzstaat.launcher.di.ServiceLocator

/**
 * Holds the process wide singletons. Kept deliberately thin: everything the launcher needs
 * is created lazily so that a cold start does no work it can avoid.
 */
class LauncherApplication : Application() {

    lateinit var services: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        services = ServiceLocator(this)
    }

    /**
     * The trim level constants are deprecated on API 36 but the callback still fires with the
     * old numbers, so they are spelled out here instead of being read from a deprecated field.
     * UI_HIDDEN (20) is ignored on purpose: it arrives every single time the user opens an app,
     * and dropping the icon cache there would make coming back to the launcher visibly slower.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when {
            level >= TRIM_LEVEL_COMPLETE -> services.iconCache.clear()
            level >= TRIM_LEVEL_BACKGROUND -> services.iconCache.trim()
        }
    }

    /** Density or locale changes invalidate every rasterised icon. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        services.iconCache.clear()
    }

    private companion object {
        const val TRIM_LEVEL_BACKGROUND = 40
        const val TRIM_LEVEL_COMPLETE = 80
    }
}
