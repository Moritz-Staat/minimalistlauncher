package de.moritzstaat.launcher.data.app

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.ConcurrentHashMap

/**
 * The single source of installed, launchable apps.
 *
 * Uses [LauncherApps] rather than PackageManager queries: it covers every user profile,
 * pushes change notifications instead of needing polling, and works without the
 * QUERY_ALL_PACKAGES permission.
 */
class AppRepository(
    context: Context,
    externalScope: CoroutineScope,
) {

    private val appContext = context.applicationContext
    private val launcherApps = requireNotNull(appContext.getSystemService<LauncherApps>())
    private val userManager = requireNotNull(appContext.getSystemService<UserManager>())

    /** Resolved activity infos, kept so launching does not have to query the system again. */
    private val activities = ConcurrentHashMap<String, LauncherActivityInfo>()

    private val packageChanges: Flow<Unit> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = signal()
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = signal()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = signal()

            override fun onPackagesAvailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean,
            ) = signal()

            override fun onPackagesUnavailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean,
            ) = signal()

            override fun onPackagesSuspended(
                packageNames: Array<out String>?,
                user: UserHandle?,
            ) = signal()

            override fun onPackagesUnsuspended(
                packageNames: Array<out String>?,
                user: UserHandle?,
            ) = signal()

            private fun signal() {
                trySend(Unit)
            }
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        trySend(Unit)
        awaitClose { launcherApps.unregisterCallback(callback) }
    }.conflate()

    @OptIn(ExperimentalCoroutinesApi::class)
    val installedApps: StateFlow<List<AppEntry>> = packageChanges
        .mapLatest { loadInstalled() }
        .flowOn(Dispatchers.IO)
        .stateIn(externalScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private fun loadInstalled(): List<AppEntry> {
        val primary = android.os.Process.myUserHandle()
        val entries = ArrayList<AppEntry>(128)
        val resolved = HashMap<String, LauncherActivityInfo>(128)
        for (user in userManager.userProfiles) {
            val serial = userManager.getSerialNumberForUser(user)
            for (info in launcherApps.getActivityList(null, user)) {
                val key = AppKey(
                    packageName = info.componentName.packageName,
                    className = info.componentName.className,
                    userSerial = serial,
                )
                val label = info.label?.toString().orEmpty().ifBlank { key.packageName }
                resolved[key.flatten()] = info
                entries += AppEntry(
                    key = key,
                    label = label,
                    systemLabel = label,
                    isWorkProfile = user != primary,
                )
            }
        }
        activities.keys.retainAll(resolved.keys)
        activities.putAll(resolved)
        return AppSorting.sorted(entries) { it.label }
    }

    fun activityInfo(key: AppKey): LauncherActivityInfo? = activities[key.flatten()]

    fun userFor(key: AppKey): UserHandle? = userManager.getUserForSerialNumber(key.userSerial)

    /** Starts an app. [sourceBounds] drives the system's launch animation. */
    fun launch(key: AppKey, sourceBounds: Rect? = null, options: Bundle? = null): Boolean {
        val info = activityInfo(key) ?: return false
        return runCatching {
            launcherApps.startMainActivity(info.componentName, info.user, sourceBounds, options)
        }.isSuccess
    }

    fun openAppInfo(key: AppKey, sourceBounds: Rect? = null): Boolean {
        val info = activityInfo(key) ?: return false
        return runCatching {
            launcherApps.startAppDetailsActivity(info.componentName, info.user, sourceBounds, null)
        }.isSuccess
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
