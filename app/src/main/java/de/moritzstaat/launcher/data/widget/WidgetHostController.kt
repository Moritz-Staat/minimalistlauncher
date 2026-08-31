package de.moritzstaat.launcher.data.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.SizeF
import androidx.core.content.getSystemService

/** Where a widget lives. */
enum class WidgetSlot {
    /** Between clock and favourites. */
    UnderClock,

    /** Replaces the clock block entirely. */
    InsteadOfClock,

    /** Inside an app pop-up; the owning app is stored alongside. */
    Popup,
    ;

    companion object {
        fun fromStorage(value: String): WidgetSlot =
            entries.firstOrNull { it.name == value } ?: UnderClock
    }
}

/**
 * Owns the launcher's [AppWidgetHost].
 *
 * The host id is a fixed constant: it identifies this launcher to the system across restarts
 * and reinstalls, and changing it would orphan every widget the user has bound.
 */
class WidgetHostController(context: Context) {

    private val appContext = context.applicationContext
    private val manager = requireNotNull(appContext.getSystemService<AppWidgetManager>())
    private val host = AppWidgetHost(appContext, HOST_ID)

    private var listening = false

    /** Tied to the activity lifecycle: a listening host keeps widgets updating. */
    fun startListening() {
        if (listening) return
        runCatching { host.startListening() }.onSuccess { listening = true }
    }

    fun stopListening() {
        if (!listening) return
        runCatching { host.stopListening() }
        listening = false
    }

    fun installedProviders(): List<AppWidgetProviderInfo> =
        runCatching { manager.installedProviders }.getOrDefault(emptyList())

    fun providerFor(appWidgetId: Int): AppWidgetProviderInfo? =
        runCatching { manager.getAppWidgetInfo(appWidgetId) }.getOrNull()

    fun allocateId(): Int = host.allocateAppWidgetId()

    /**
     * Tries the silent bind. Returns false when the system wants the user to confirm, in which
     * case [createBindIntent] provides the dialog.
     */
    fun bindIfAllowed(appWidgetId: Int, provider: AppWidgetProviderInfo): Boolean =
        runCatching {
            manager.bindAppWidgetIdIfAllowed(appWidgetId, provider.profile, provider.provider, null)
        }.getOrDefault(false)

    fun createBindIntent(appWidgetId: Int, provider: AppWidgetProviderInfo): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, provider.profile)

    fun needsConfiguration(provider: AppWidgetProviderInfo): Boolean = provider.configure != null

    /**
     * Explicit configure intent. Some providers do not export their configure activity; for
     * those [startConfigureForResult] goes through the host instead.
     */
    fun createConfigureIntent(appWidgetId: Int, provider: AppWidgetProviderInfo): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            .setComponent(provider.configure)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    /** Fallback that lets the system start a configure activity we may not launch ourselves. */
    fun startConfigureForResult(activity: Activity, appWidgetId: Int, requestCode: Int): Boolean =
        runCatching {
            host.startAppWidgetConfigureActivityForResult(activity, appWidgetId, 0, requestCode, null)
        }.isSuccess

    fun createView(context: Context, appWidgetId: Int): AppWidgetHostView? {
        val provider = providerFor(appWidgetId) ?: return null
        return runCatching { host.createView(context, appWidgetId, provider) }.getOrNull()
    }

    /**
     * Tells the widget how much room it has. Without this many widgets render at their minimum
     * size and leave the rest of the slot empty.
     */
    fun updateSize(view: AppWidgetHostView, widthDp: Float, heightDp: Float) {
        runCatching {
            view.updateAppWidgetSize(Bundle.EMPTY, listOf(SizeF(widthDp, heightDp)))
        }
    }

    /**
     * Always called when a widget is removed. A forgotten id stays allocated for the lifetime
     * of the host and comes back as an empty grey box after the next restart.
     */
    fun deleteId(appWidgetId: Int) {
        runCatching { host.deleteAppWidgetId(appWidgetId) }
    }

    /** Drops every id the database no longer knows about, for example after a backup import. */
    fun pruneOrphans(knownIds: Set<Int>) {
        val allocated = runCatching { host.appWidgetIds }.getOrNull() ?: return
        allocated.filterNot { it in knownIds }.forEach { deleteId(it) }
    }

    private companion object {
        /** Stable across restarts and reinstalls; never change this. */
        const val HOST_ID = 0x4C41
    }
}
