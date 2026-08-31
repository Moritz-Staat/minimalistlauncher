package de.moritzstaat.launcher.data.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatcherTest {

    private fun score(label: String, query: String): Int? =
        FuzzyMatcher.match(label, query)?.score

    private fun rank(query: String, vararg labels: String): List<String> =
        labels.mapNotNull { label -> FuzzyMatcher.match(label, query)?.let { label to it.score } }
            .sortedByDescending { it.second }
            .map { it.first }

    @Test
    fun `prefixes beat matches in the middle of a word`() {
        assertEquals(listOf("Kalender", "Kontakte"), rank("ka", "Kontakte", "Kalender"))
    }

    @Test
    fun `word starts rank above loose subsequences`() {
        val order = rank("map", "Google Maps", "Mediathek App")
        assertEquals("Google Maps", order.first())
    }

    @Test
    fun `camel case initials find Gmail from gm`() {
        assertNotNull(FuzzyMatcher.match("Gmail", "gm"))
        assertTrue(score("Gmail", "gm")!! > score("Programm", "gm")!!)
    }

    @Test
    fun `initials of separate words match`() {
        assertNotNull(FuzzyMatcher.match("Google Maps", "gm"))
        assertNotNull(FuzzyMatcher.match("Deutsche Bahn Navigator", "dbn"))
    }

    @Test
    fun `search ignores diacritics in both directions`() {
        assertNotNull(FuzzyMatcher.match("Öffi", "off"))
        assertNotNull(FuzzyMatcher.match("Offi", "öff"))
        assertNotNull(FuzzyMatcher.match("Telefonbücher", "bucher"))
    }

    @Test
    fun `sharp s is searchable as double s`() {
        assertNotNull(FuzzyMatcher.match("Straßenverkehr", "strassen"))
    }

    @Test
    fun `case does not matter`() {
        assertEquals(score("WhatsApp", "whatsapp"), score("WhatsApp", "WHATSAPP"))
    }

    @Test
    fun `an exact label beats a prefix`() {
        assertTrue(score("Uhr", "uhr")!! > score("Uhrzeit", "uhr")!!)
    }

    @Test
    fun `nothing matches an unrelated query`() {
        assertNull(FuzzyMatcher.match("Kalender", "xyz"))
        assertNull(FuzzyMatcher.match("Kalender", ""))
    }

    @Test
    fun `matched indices point into the original label`() {
        val result = FuzzyMatcher.match("Gmail", "gm")
        assertNotNull(result)
        assertEquals(listOf(0, 1), result!!.matchedIndices.toList())
    }

    @Test
    fun `matched indices survive folding that changes the length`() {
        val result = FuzzyMatcher.match("Straße", "strasse")
        assertNotNull(result)
        // 'ß' folds to two characters, both pointing back at the same original index.
        assertEquals(listOf(0, 1, 2, 3, 4, 4, 5), result!!.matchedIndices.toList())
    }

    @Test
    fun `earlier words outrank later words`() {
        assertTrue(score("Bahn Navigator", "nav")!! > score("Deutsche Bahn Navigator", "nav")!!)
    }
}
