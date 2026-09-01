package de.moritzstaat.launcher.data.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrequentAppsTest {

    private fun tally(name: String, opens: Int, lastDay: Long = 100L) =
        OpenTally(packageName = name, opens = opens, lastDayEpoch = lastDay)

    @Test
    fun `the most used apps come first`() {
        val ranked = FrequentApps.rank(
            listOf(tally("c", 3), tally("a", 12), tally("b", 7)),
        )

        assertEquals(listOf("a", "b", "c"), ranked)
    }

    @Test
    fun `a single accidental opening does not promote an app`() {
        val ranked = FrequentApps.rank(listOf(tally("once", 1), tally("often", 5)))

        assertEquals(listOf("often"), ranked)
    }

    @Test
    fun `the block never grows past its limit`() {
        val many = (1..20).map { tally("app$it", 50 - it) }

        assertEquals(FrequentApps.LIMIT, FrequentApps.rank(many).size)
    }

    @Test
    fun `equal counts break towards the app used more recently`() {
        val ranked = FrequentApps.rank(
            listOf(tally("stale", 5, lastDay = 90L), tally("fresh", 5, lastDay = 99L)),
        )

        assertEquals(listOf("fresh", "stale"), ranked)
    }

    @Test
    fun `fully equal candidates keep a stable order`() {
        val first = FrequentApps.rank(listOf(tally("b", 5), tally("a", 5)))
        val second = FrequentApps.rank(listOf(tally("a", 5), tally("b", 5)))

        assertEquals(listOf("a", "b"), first)
        assertEquals(first, second)
    }

    @Test
    fun `no history means no block at all`() {
        assertTrue(FrequentApps.rank(emptyList()).isEmpty())
    }

    @Test
    fun `a fresh install shows nothing rather than everything`() {
        // Every app opened once: below the threshold, so the block stays out of the way until
        // there is a real habit to show.
        val fresh = (1..10).map { tally("app$it", 1) }

        assertTrue(FrequentApps.rank(fresh).isEmpty())
    }

    @Test
    fun `the retention window and the ranking window are the same`() {
        // The counters are deleted after WINDOW_DAYS, so a longer ranking window would silently
        // read rows that no longer exist.
        assertEquals(7L, FrequentApps.WINDOW_DAYS)
    }

    @Test
    fun `the limit and the threshold stay sane`() {
        assertTrue(FrequentApps.LIMIT in 1..8)
        assertTrue(FrequentApps.MIN_OPENS >= 2)
    }
}
