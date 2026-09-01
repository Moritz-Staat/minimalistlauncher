package de.moritzstaat.launcher.data.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetCatalogTest {

    private fun option(label: String, columns: Int = 2, rows: Int = 1, description: String = "") =
        WidgetOption(
            providerFlat = "pkg/$label",
            label = label,
            description = description,
            columns = columns,
            rows = rows,
            resizable = false,
        )

    @Test
    fun `dp turns into the grid cells the user knows from other launchers`() {
        assertEquals(1, WidgetCatalog.cellsFor(40))
        // 70 dp does not fit one cell: the platform reserves 30 dp of margin per cell.
        assertEquals(2, WidgetCatalog.cellsFor(70))
        assertEquals(2, WidgetCatalog.cellsFor(110))
        assertEquals(3, WidgetCatalog.cellsFor(180))
        assertEquals(4, WidgetCatalog.cellsFor(250))
    }

    @Test
    fun `a size is never zero cells, however small the widget claims to be`() {
        assertEquals(1, WidgetCatalog.cellsFor(0))
        assertEquals(1, WidgetCatalog.cellsFor(-20))
    }

    @Test
    fun `the size label reads as columns by rows`() {
        assertEquals("4 × 2", option("Uhr", columns = 4, rows = 2).sizeLabel)
    }

    @Test
    fun `a resizable widget says so`() {
        val resizable = option("Uhr", columns = 4, rows = 1).copy(resizable = true)

        assertTrue(resizable.sizeLabel, resizable.sizeLabel.endsWith("anpassbar"))
    }

    @Test
    fun `widgets are grouped by app and both levels sorted`() {
        val groups = WidgetCatalog.group(
            options = listOf(
                "com.b" to option("Zeitplan"),
                "com.a" to option("Uhr"),
                "com.b" to option("Aufgaben"),
            ),
            appLabels = mapOf("com.a" to "Alpha", "com.b" to "Beta"),
        )

        assertEquals(listOf("Alpha", "Beta"), groups.map { it.appLabel })
        assertEquals(listOf("Aufgaben", "Zeitplan"), groups[1].widgets.map { it.label })
    }

    @Test
    fun `an app without a readable label falls back to its package name`() {
        val groups = WidgetCatalog.group(listOf("com.unknown" to option("Uhr")), emptyMap())

        assertEquals("com.unknown", groups.single().appLabel)
    }

    @Test
    fun `german collation applies to the app names too`() {
        val groups = WidgetCatalog.group(
            options = listOf("com.b" to option("x"), "com.a" to option("y")),
            appLabels = mapOf("com.a" to "Ärzte", "com.b" to "Astronomie"),
        )

        // "Ä" sorts under "A", so it must not land behind "Z".
        assertEquals(listOf("Ärzte", "Astronomie"), groups.map { it.appLabel })
    }

    @Test
    fun `searching matches the widget name`() {
        val groups = WidgetCatalog.group(
            options = listOf("com.a" to option("Uhr"), "com.a" to option("Kalender")),
            appLabels = mapOf("com.a" to "Alpha"),
        )

        val hits = WidgetCatalog.filter(groups, "uhr")

        assertEquals(listOf("Uhr"), hits.single().widgets.map { it.label })
    }

    @Test
    fun `matching the app name keeps every widget it offers`() {
        val groups = WidgetCatalog.group(
            options = listOf("com.a" to option("Uhr"), "com.a" to option("Kalender")),
            appLabels = mapOf("com.a" to "Alpha"),
        )

        val hits = WidgetCatalog.filter(groups, "alpha")

        assertEquals(2, hits.single().widgets.size)
    }

    @Test
    fun `the description is searchable too`() {
        val groups = WidgetCatalog.group(
            options = listOf("com.a" to option("W1", description = "Zeigt das Wetter")),
            appLabels = mapOf("com.a" to "Alpha"),
        )

        assertEquals(1, WidgetCatalog.filter(groups, "wetter").size)
    }

    @Test
    fun `an empty query changes nothing and a miss gives nothing`() {
        val groups = WidgetCatalog.group(
            options = listOf("com.a" to option("Uhr")),
            appLabels = mapOf("com.a" to "Alpha"),
        )

        assertEquals(groups, WidgetCatalog.filter(groups, "   "))
        assertTrue(WidgetCatalog.filter(groups, "gibtsnicht").isEmpty())
    }
}
