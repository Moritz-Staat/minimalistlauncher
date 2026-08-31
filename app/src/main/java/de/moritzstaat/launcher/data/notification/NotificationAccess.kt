package de.moritzstaat.launcher.data.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import de.moritzstaat.launcher.service.LauncherNotificationListener

/**
 * Status of the notification access and the way into the system settings for it.
 *
 * There is no way to request the access with a dialog: the user has to grant it in the system
 * settings, and Android drops it again on every reinstall of the app.
 */
object NotificationAccess {

    fun isGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    /**
     * Opens the settings page for this listener. The generic list is used as a fallback,
     * because the per-component page is not present on every build.
     */
    fun settingsIntent(context: Context): Intent {
        val component = ComponentName(context, LauncherNotificationListener::class.java)
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(EXTRA_COMPONENT_NAME, component.flattenToString())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun fallbackSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Public since API 34, spelled out here because minSdk is 31. */
    private const val EXTRA_COMPONENT_NAME = ":settings:fragment_args_key"
}
