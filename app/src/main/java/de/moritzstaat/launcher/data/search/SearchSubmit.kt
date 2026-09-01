package de.moritzstaat.launcher.data.search

/**
 * What pressing enter in the search field does.
 *
 * Only ever fires when exactly one thing matched. With two candidates the best one is a guess,
 * and a typo that opens the wrong app is worse than one more tap — so anything ambiguous falls
 * through to the web search, which is what the field did before.
 */
object SearchSubmit {

    /**
     * @return the single match, or null when there is none or more than one.
     *
     * The web search row is always present and is not a match: it is the way out of a query,
     * not a result of it.
     */
    fun singleMatch(results: List<SearchResult>): SearchResult? {
        val matches = results.filter { it !is SearchResult.WebSearch }
        return matches.singleOrNull()
    }
}
