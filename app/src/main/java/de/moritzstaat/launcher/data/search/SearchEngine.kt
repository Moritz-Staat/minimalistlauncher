package de.moritzstaat.launcher.data.search

import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.app.ShortcutRepository

/**
 * Puts one query against everything the launcher can find.
 *
 * The app part is pure and covered by unit tests; shortcuts and contacts are thin wrappers
 * around system queries that are simply skipped when they are not available.
 */
class SearchEngine(
    private val shortcutRepository: ShortcutRepository,
    private val contactSearch: ContactSearch,
) {

    fun search(query: String, apps: List<AppEntry>): List<SearchResult> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val results = ArrayList<SearchResult>()
        results += matchApps(apps, trimmed)
        results += matchShortcuts(apps, trimmed)
        results += matchContacts(trimmed)
        results += SearchResult.WebSearch(trimmed)
        return results
    }

    private fun matchShortcuts(apps: List<AppEntry>, query: String): List<SearchResult.Shortcut> {
        if (query.length < MIN_SHORTCUT_QUERY) return emptyList()
        val folded = TextNormalizer.normalize(query).value
        val appLabels = apps.associate { it.key.packageName to it.label }
        return shortcutRepository.allShortcuts(apps.map { it.key })
            .mapNotNull { shortcut ->
                val label = shortcutRepository.label(shortcut)
                if (label.isBlank()) return@mapNotNull null
                val match = FuzzyMatcher.match(TextNormalizer.normalize(label), folded)
                    ?: return@mapNotNull null
                SearchResult.Shortcut(
                    shortcut = shortcut,
                    label = label,
                    appLabel = appLabels[shortcut.`package`].orEmpty(),
                    score = match.score,
                )
            }
            .sortedByDescending { it.score }
            .take(MAX_SHORTCUTS)
    }

    private fun matchContacts(query: String): List<SearchResult.Contact> =
        contactSearch.search(query).mapIndexed { index, hit ->
            SearchResult.Contact(hit, score = -index)
        }

    companion object {
        private const val MIN_SHORTCUT_QUERY = 2
        private const val MAX_SHORTCUTS = 4
        private const val MAX_APPS = 12

        /**
         * Pure app ranking. Labels are folded once per call; the caller keeps the app list
         * stable, so this stays cheap enough to run on every keystroke.
         */
        fun matchApps(apps: List<AppEntry>, query: String): List<SearchResult.App> {
            val folded = TextNormalizer.normalize(query).value
            if (folded.isEmpty()) return emptyList()
            return apps.asSequence()
                .mapNotNull { entry ->
                    val match = FuzzyMatcher.match(TextNormalizer.normalize(entry.label), folded)
                        ?: return@mapNotNull null
                    SearchResult.App(entry, match.score, match.matchedIndices)
                }
                .sortedByDescending { it.score }
                .take(MAX_APPS)
                .toList()
        }
    }
}
