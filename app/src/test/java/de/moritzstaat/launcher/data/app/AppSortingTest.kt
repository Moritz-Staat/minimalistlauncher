package de.moritzstaat.launcher.data.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSortingTest {

    @Test
    fun `umlauts land under their base letter`() {
        assertEquals("A", AppSorting.sectionFor("Ähnlich"))
        assertEquals("O", AppSorting.sectionFor("Öffi"))
        assertEquals("U", AppSorting.sectionFor("Über"))
        assertEquals("E", AppSorting.sectionFor("Église"))
    }

    @Test
    fun `special letters fold to their base letter`() {
        assertEquals("S", AppSorting.sectionFor("ßeta"))
        assertEquals("O", AppSorting.sectionFor("Ørsted"))
        assertEquals("L", AppSorting.sectionFor("Łódź"))
    }

    @Test
    fun `digits and symbols share the trailing section`() {
        assertEquals("#", AppSorting.sectionFor("1Password"))
        assertEquals("#", AppSorting.sectionFor("+Fit"))
        assertEquals("#", AppSorting.sectionFor("★ Notes"))
        assertEquals("#", AppSorting.sectionFor(""))
        assertEquals("#", AppSorting.sectionFor("   "))
    }

    @Test
    fun `lower case labels get an upper case section`() {
        assertEquals("W", AppSorting.sectionFor("whatsapp"))
    }

    @Test
    fun `leading whitespace is ignored`() {
        assertEquals("K", AppSorting.sectionFor("  Kalender"))
    }

    @Test
    fun `umlauts sort next to their base letter, not behind z`() {
        val labels = listOf("Zoom", "Über", "Uber", "Apotheke", "Ähnlich")
        val sorted = AppSorting.sorted(labels) { it }
        assertEquals(listOf("Ähnlich", "Apotheke", "Uber", "Über", "Zoom"), sorted)
    }

    @Test
    fun `non letters sort after every letter`() {
        val labels = listOf("1Password", "Zoom", "+Fit", "Apotheke")
        val sorted = AppSorting.sorted(labels) { it }
        assertEquals(listOf("Apotheke", "Zoom", "+Fit", "1Password"), sorted)
    }

    @Test
    fun `sorting is case insensitive but stable`() {
        val labels = listOf("gmail", "Gmail", "GitHub")
        val sorted = AppSorting.sorted(labels) { it }
        assertEquals(listOf("GitHub", "Gmail", "gmail"), sorted)
    }

    @Test
    fun `sections are distinct and in list order`() {
        val labels = listOf("Apotheke", "Ähnlich", "Bahn", "Zoom", "1Password", "+Fit")
        val sorted = AppSorting.sorted(labels) { it }
        assertEquals(listOf("A", "B", "Z", "#"), AppSorting.sections(sorted) { it })
    }
}
