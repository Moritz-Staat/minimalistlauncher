package de.moritzstaat.launcher.data.icon

import de.moritzstaat.launcher.data.search.FuzzyMatcher
import de.moritzstaat.launcher.data.search.TextNormalizer

/**
 * Finds a drawable for an app the pack does not list explicitly.
 *
 * Icon packs name their drawables after the app, so "whatsapp", "google_maps" or
 * "com_spotify_music" are all reasonable guesses. This matches the app label and its package
 * against those names and only accepts a confident hit; a wrong icon is worse than none.
 */
object IconPackMatcher {

    /** Below this the guess is too loose to be trusted. */
    const val MIN_SCORE = 8_000

    fun findDrawable(
        drawableNames: Collection<String>,
        appLabel: String,
        packageName: String,
    ): String? {
        if (drawableNames.isEmpty()) return null

        exactMatch(drawableNames, appLabel)?.let { return it }
        exactMatch(drawableNames, packageName)?.let { return it }
        packageTailMatch(drawableNames, packageName)?.let { return it }

        val query = TextNormalizer.normalize(appLabel).value
        if (query.isEmpty()) return null

        var best: String? = null
        var bestScore = MIN_SCORE - 1
        for (name in drawableNames) {
            val readable = TextNormalizer.normalize(name.readableDrawableName())
            val match = FuzzyMatcher.match(readable, query) ?: continue
            if (match.score > bestScore) {
                bestScore = match.score
                best = name
            }
        }
        return best
    }

    private fun exactMatch(names: Collection<String>, candidate: String): String? {
        val normalized = candidate.toDrawableName()
        return names.firstOrNull { it.toDrawableName() == normalized }
    }

    /** "com.spotify.music" also lives as "music" or "spotify" in many packs. */
    private fun packageTailMatch(names: Collection<String>, packageName: String): String? {
        val parts = packageName.split('.').filter { it.isNotBlank() }
        for (part in parts.asReversed()) {
            if (part.length < MIN_PART_LENGTH) continue
            exactMatch(names, part)?.let { return it }
        }
        return null
    }

    /** Drawable names have no spaces or dots; fold everything down to bare letters. */
    internal fun String.toDrawableName(): String =
        TextNormalizer.normalize(this).value.filter { it.isLetterOrDigit() }

    /** Turns "google_maps" back into something the label matcher can compare against. */
    internal fun String.readableDrawableName(): String =
        replace('_', ' ').replace('-', ' ').trim()

    private const val MIN_PART_LENGTH = 4
}
