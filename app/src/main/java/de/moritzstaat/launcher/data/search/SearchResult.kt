package de.moritzstaat.launcher.data.search

import android.content.pm.ShortcutInfo
import de.moritzstaat.launcher.data.app.AppEntry

/**
 * One row of the search results. Apps first, then shortcuts and contacts, and always the web
 * search as the last row so there is a way out of every query.
 */
sealed interface SearchResult {

    /** Stable list key. */
    val id: String

    data class App(
        val entry: AppEntry,
        val score: Int,
        val matchedIndices: IntArray,
    ) : SearchResult {
        override val id: String get() = "app:${entry.key.flatten()}"

        override fun equals(other: Any?): Boolean =
            other is App && entry == other.entry && score == other.score &&
                matchedIndices.contentEquals(other.matchedIndices)

        override fun hashCode(): Int =
            (31 * entry.hashCode() + score) * 31 + matchedIndices.contentHashCode()
    }

    data class Shortcut(
        val shortcut: ShortcutInfo,
        val label: String,
        val appLabel: String,
        val score: Int,
    ) : SearchResult {
        override val id: String get() = "shortcut:${shortcut.`package`}:${shortcut.id}"
    }

    data class Contact(
        val hit: ContactHit,
        val score: Int,
    ) : SearchResult {
        override val id: String get() = "contact:${hit.lookupKey}"
    }

    data class WebSearch(val query: String) : SearchResult {
        override val id: String get() = "web"
    }
}
