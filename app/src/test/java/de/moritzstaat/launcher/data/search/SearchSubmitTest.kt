package de.moritzstaat.launcher.data.search

import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.app.AppKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchSubmitTest {

    private fun app(name: String): SearchResult.App = SearchResult.App(
        entry = AppEntry(
            key = AppKey(name, "$name.Main", 0L),
            label = name,
            systemLabel = name,
            isWorkProfile = false,
        ),
        score = 100,
        matchedIndices = IntArray(0),
    )

    private val web = SearchResult.WebSearch("wh")

    @Test
    fun `one app plus the web row counts as a single match`() {
        val single = SearchSubmit.singleMatch(listOf(app("WhatsApp"), web))

        assertEquals(app("WhatsApp"), single)
    }

    @Test
    fun `two candidates are ambiguous and yield nothing`() {
        assertNull(SearchSubmit.singleMatch(listOf(app("WhatsApp"), app("Wetter"), web)))
    }

    @Test
    fun `a query that only offers the web search yields nothing`() {
        assertNull(SearchSubmit.singleMatch(listOf(web)))
    }

    @Test
    fun `an empty result list yields nothing`() {
        assertNull(SearchSubmit.singleMatch(emptyList()))
    }

    @Test
    fun `a lone shortcut or contact also counts`() {
        val contact = SearchResult.Contact(
            hit = ContactHit(
                lookupKey = "abc",
                displayName = "Hanna",
                photoUri = null,
            ),
            score = 90,
        )

        assertEquals(contact, SearchSubmit.singleMatch(listOf(contact, web)))
    }

    @Test
    fun `the web row is never the single match, wherever it sits`() {
        // Defensive: the engine puts it last, but the rule must not depend on that.
        assertNull(SearchSubmit.singleMatch(listOf(web, web)))
    }
}
