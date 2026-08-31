package de.moritzstaat.launcher.ui.widget

import android.app.Application
import android.appwidget.AppWidgetProviderInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.db.WidgetEntity
import de.moritzstaat.launcher.data.widget.WidgetSlot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One placed widget with the provider it belongs to. */
data class PlacedWidget(
    val appWidgetId: Int,
    val slot: WidgetSlot,
    val ownerKey: String?,
    val provider: AppWidgetProviderInfo?,
)

/** Placement and lifecycle of the bound app widgets. */
class WidgetViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as LauncherApplication).services
    private val dao = services.database.widgetDao()

    val host = services.widgetHost

    val widgets: StateFlow<List<PlacedWidget>> = dao.observeAll()
        .map { rows ->
            // Ids the host still knows but the database does not are dead weight.
            services.widgetHost.pruneOrphans(rows.map { it.appWidgetId }.toSet())
            rows.map { row ->
                PlacedWidget(
                    appWidgetId = row.appWidgetId,
                    slot = WidgetSlot.fromStorage(row.slot),
                    ownerKey = row.ownerKey,
                    provider = services.widgetHost.providerFor(row.appWidgetId),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun widgetsIn(slot: WidgetSlot, ownerKey: String? = null): List<PlacedWidget> =
        widgets.value.filter { it.slot == slot && it.ownerKey == ownerKey }

    fun place(appWidgetId: Int, slot: WidgetSlot, ownerKey: String? = null) {
        viewModelScope.launch {
            val position = dao.countIn(slot.name, ownerKey)
            dao.insert(WidgetEntity(appWidgetId, slot.name, ownerKey, position))
        }
    }

    /** Removes the row and releases the id; never one without the other. */
    fun remove(appWidgetId: Int) {
        viewModelScope.launch {
            dao.delete(appWidgetId)
            services.widgetHost.deleteId(appWidgetId)
        }
    }

    /** Called when binding or configuring was abandoned, so the allocated id is not lost. */
    fun discard(appWidgetId: Int) {
        services.widgetHost.deleteId(appWidgetId)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
