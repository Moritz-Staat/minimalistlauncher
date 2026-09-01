package de.moritzstaat.launcher.data.gesture

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import de.moritzstaat.launcher.service.LauncherGestureService

/**
 * Whether the accessibility service is switched on, and the way into the system settings.
 *
 * There is no dialog to ask for this: the user has to find the entry themselves, and on a
 * sideloaded build Android additionally hides the switch behind "restricted setting".
 */
object GestureAccess {

    fun isGranted(context: Context): Boolean {
        val component = ComponentName(context, LauncherGestureService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()

        // The list is colon separated and holds flattened component names.
        return enabled.split(':').any { entry ->
            ComponentName.unflattenFromString(entry) == component
        }
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
