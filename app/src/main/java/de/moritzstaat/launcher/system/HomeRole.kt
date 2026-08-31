package de.moritzstaat.launcher.system

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService

/**
 * Thin wrapper around the ROLE_HOME role.
 *
 * The launcher never forces itself onto the device: it can only report whether it currently
 * holds the home role and hand out the system intent that lets the user decide. The stock
 * launcher stays installed and reachable at all times.
 */
object HomeRole {

    fun isSupported(context: Context): Boolean =
        context.getSystemService<RoleManager>()?.isRoleAvailable(RoleManager.ROLE_HOME) == true

    fun isHeld(context: Context): Boolean =
        context.getSystemService<RoleManager>()?.isRoleHeld(RoleManager.ROLE_HOME) == true

    /** System dialog that asks the user to make this app the home screen, or null if unavailable. */
    fun createRequestIntent(context: Context): Intent? {
        val roleManager = context.getSystemService<RoleManager>() ?: return null
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) return null
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }

    /** Fallback for OEM builds that refuse the role dialog: open the system home settings. */
    fun createSettingsIntent(): Intent =
        Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
