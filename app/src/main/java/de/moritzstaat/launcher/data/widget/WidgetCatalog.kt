package de.moritzstaat.launcher.data.widget

import de.moritzstaat.launcher.data.app.AppSorting
import kotlin.math.max

/** One offered widget, already reduced to what the picker needs to show. */
data class WidgetOption(
    val providerFlat: String,
    val label: String,
    val description: String,
    val columns: Int,
    val rows: Int,
    val resizable: Boolean,
) {
    /** e.g. "4 × 1", plus a hint when the widget can be resized after placing. */
    val sizeLabel: String
        get() = buildString {
            append(columns)
            append(" × ")
            append(rows)
            if (resizable) append(", anpassbar")
        }
}

/** The widgets one app offers. The picker lists apps and unfolds them on demand. */
data class WidgetGroup(
    val packageName: String,
    val appLabel: String,
    val widgets: List<WidgetOption>,
)

/**
 * Turns the flat provider list the system hands out into something a person can navigate.
 *
 * The stock list is every widget of every app in one run — a few hundred rows of bare labels
 * like "1×1" or "Shortcut", where nothing says which app it came from or how big it is. So the
 * rules live here, free of Android types and therefore testable: group by app, sort both
 * levels by the same German collation as the app list, and turn raw dp into grid cells.
 */
object WidgetCatalog {

    /**
     * Widget grid cells for a minimum size in dp.
     *
     * The launcher's own home screen has no fixed grid, but every widget was designed against
     * the platform's 70dp cell with 30dp of margin, and that is the number the user recognises
     * from other launchers.
     */
    fun cellsFor(minSizeDp: Int): Int =
        max(1, (minSizeDp + CELL_MARGIN_DP + CELL_SIZE_DP - 1) / CELL_SIZE_DP)

    /** Groups by app and sorts apps and widgets alphabetically. */
    fun group(options: List<Pair<String, WidgetOption>>, appLabels: Map<String, String>): List<WidgetGroup> {
        val groups = options
            .groupBy { (packageName, _) -> packageName }
            .map { (packageName, entries) ->
                WidgetGroup(
                    packageName = packageName,
                    appLabel = appLabels[packageName] ?: packageName,
                    widgets = AppSorting.sorted(entries.map { it.second }) { it.label },
                )
            }
        return AppSorting.sorted(groups) { it.appLabel }
    }

    /**
     * Filters by a typed query, matched against the app name and the widget names.
     *
     * A group whose app name matches keeps all its widgets: having typed the app's name, the
     * user wants to see what it offers, not a subset.
     */
    fun filter(groups: List<WidgetGroup>, query: String): List<WidgetGroup> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return groups
        return groups.mapNotNull { group ->
            if (group.appLabel.lowercase().contains(needle)) return@mapNotNull group
            val hits = group.widgets.filter {
                it.label.lowercase().contains(needle) || it.description.lowercase().contains(needle)
            }
            if (hits.isEmpty()) null else group.copy(widgets = hits)
        }
    }

    private const val CELL_SIZE_DP = 70
    private const val CELL_MARGIN_DP = 30
}
