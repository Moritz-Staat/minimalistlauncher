package de.moritzstaat.launcher.data.search

/**
 * Result of matching a query against a label.
 *
 * [score] is only meaningful relative to other results of the same query: higher wins.
 * [matchedIndices] point into the original, unfolded label so the UI can highlight them.
 */
data class MatchResult(
    val score: Int,
    val matchedIndices: IntArray,
) {
    override fun equals(other: Any?): Boolean =
        other is MatchResult && score == other.score && matchedIndices.contentEquals(other.matchedIndices)

    override fun hashCode(): Int = 31 * score + matchedIndices.contentHashCode()
}

/**
 * Ranking rules for the search field, in the order a user expects them:
 *
 *  1. the whole label,
 *  2. the beginning of the label,
 *  3. the beginning of any word inside the label,
 *  4. the initials of the words, "gm" for "Google Maps",
 *  5. the letters in order starting at a word boundary, "gm" for "Gmail",
 *  6. the letters in order anywhere.
 *
 * Everything runs on the folded text, so diacritics, case and the sharp s do not matter.
 */
object FuzzyMatcher {

    private const val SCORE_EXACT = 10_000
    private const val SCORE_PREFIX = 9_000
    private const val SCORE_WORD_PREFIX = 8_000
    private const val SCORE_INITIALS = 7_000
    private const val SCORE_BOUNDARY_SUBSEQUENCE = 6_000
    private const val SCORE_SUBSEQUENCE = 5_000

    fun match(label: String, query: String): MatchResult? =
        match(TextNormalizer.normalize(label), TextNormalizer.normalize(query).value)

    /**
     * Overload for callers that normalise their labels once and reuse them, which is what the
     * app list does on every keystroke.
     */
    fun match(label: NormalizedText, foldedQuery: String): MatchResult? {
        if (foldedQuery.isEmpty()) return null
        if (label.length == 0) return null

        if (label.value == foldedQuery) {
            return MatchResult(SCORE_EXACT, label.rangeIndices(0, foldedQuery.length))
        }
        if (label.value.startsWith(foldedQuery)) {
            return MatchResult(SCORE_PREFIX, label.rangeIndices(0, foldedQuery.length))
        }

        wordPrefixMatch(label, foldedQuery)?.let { return it }
        initialsMatch(label, foldedQuery)?.let { return it }
        subsequenceMatch(label, foldedQuery)?.let { return it }
        return null
    }

    private fun wordPrefixMatch(label: NormalizedText, query: String): MatchResult? {
        for (start in label.wordStartIndices()) {
            if (start + query.length > label.length) continue
            if (label.value.regionMatches(start, query, 0, query.length)) {
                // Earlier words rank higher; a hit in word two beats a hit in word five.
                return MatchResult(
                    score = SCORE_WORD_PREFIX - start,
                    matchedIndices = label.rangeIndices(start, start + query.length),
                )
            }
        }
        return null
    }

    private fun initialsMatch(label: NormalizedText, query: String): MatchResult? {
        val starts = label.wordStartIndices()
        if (starts.size < query.length) return null
        val matched = IntArray(query.length)
        for (offset in query.indices) {
            val at = starts[offset]
            if (label[at] != query[offset]) return null
            matched[offset] = label.sourceIndex(at)
        }
        return MatchResult(SCORE_INITIALS, matched)
    }

    private fun subsequenceMatch(label: NormalizedText, query: String): MatchResult? {
        val matched = IntArray(query.length)
        var labelIndex = 0
        var gaps = 0
        var startsAtWordBoundary = false

        for (queryIndex in query.indices) {
            var found = -1
            while (labelIndex < label.length) {
                if (label[labelIndex] == query[queryIndex]) {
                    found = labelIndex
                    break
                }
                labelIndex++
            }
            if (found < 0) return null
            if (queryIndex == 0) startsAtWordBoundary = label.isWordStart(found) else gaps += 1
            matched[queryIndex] = label.sourceIndex(found)
            labelIndex = found + 1
        }

        val base = if (startsAtWordBoundary) SCORE_BOUNDARY_SUBSEQUENCE else SCORE_SUBSEQUENCE
        // Tighter matches win: fewer skipped characters and an earlier first hit.
        return MatchResult(
            score = base - gaps * 10 - matched.first(),
            matchedIndices = matched,
        )
    }

    private fun NormalizedText.rangeIndices(fromInclusive: Int, toExclusive: Int): IntArray =
        IntArray(toExclusive - fromInclusive) { sourceIndex(fromInclusive + it) }
}
