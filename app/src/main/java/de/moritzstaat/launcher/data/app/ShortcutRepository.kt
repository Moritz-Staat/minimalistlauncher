package de.moritzstaat.launcher.data.app

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.Bundle
import androidx.core.content.getSystemService

/**
 * App shortcuts, the entries a long press on an app icon shows in other launchers.
 *
 * Every call is guarded: the system only hands shortcuts to the app that currently holds the
 * home role, so as long as the user has not switched over yet this quietly returns nothing.
 */
class ShortcutRepository(
    context: Context,
    private val appRepository: AppRepository,
) {

    private val launcherApps = requireNotNull(context.applicationContext.getSystemService<LauncherApps>())

    fun hasAccess(): Boolean = runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)

    /** Shortcuts declared by one app, for the long press menu. */
    fun shortcutsFor(appKey: AppKey): List<ShortcutInfo> {
        val user = appRepository.userFor(appKey) ?: return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(appKey.packageName)
            .setQueryFlags(QUERY_FLAGS)
        return runCatching { launcherApps.getShortcuts(query, user) }
            .getOrNull()
            .orEmpty()
            .filter { it.isEnabled }
    }

    /** All shortcuts of all apps of one profile, for the search field. */
    fun allShortcuts(appKeys: Collection<AppKey>): List<ShortcutInfo> {
        if (!hasAccess()) return emptyList()
        val byUser = appKeys.groupBy { it.userSerial }
        val result = ArrayList<ShortcutInfo>()
        for ((_, keys) in byUser) {
            val user = appRepository.userFor(keys.first()) ?: continue
            val query = LauncherApps.ShortcutQuery().setQueryFlags(QUERY_FLAGS)
            val shortcuts = runCatching { launcherApps.getShortcuts(query, user) }
                .getOrNull()
                .orEmpty()
            result += shortcuts.filter { it.isEnabled }
        }
        return result
    }

    fun start(shortcut: ShortcutInfo, sourceBounds: Rect? = null, options: Bundle? = null): Boolean =
        runCatching {
            launcherApps.startShortcut(
                shortcut.`package`,
                shortcut.id,
                sourceBounds,
                options,
                shortcut.userHandle,
            )
        }.isSuccess

    fun label(shortcut: ShortcutInfo): String =
        (shortcut.longLabel ?: shortcut.shortLabel)?.toString().orEmpty()

    private companion object {
        const val QUERY_FLAGS = LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
    }
}
