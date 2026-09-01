package de.moritzstaat.launcher.data.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageBreakerTest {

    private val config = UsageBreakerConfig(
        enabled = true,
        packages = setOf("com.example.social"),
        threshold = 5,
        pauseSeconds = 3,
    )

    @Test
    fun `the pause comes once the threshold is reached`() {
        assertFalse(UsageBreaker.shouldPause(config, "com.example.social", opensToday = 4))
        assertTrue(UsageBreaker.shouldPause(config, "com.example.social", opensToday = 5))
        assertTrue(UsageBreaker.shouldPause(config, "com.example.social", opensToday = 50))
    }

    @Test
    fun `apps that were not chosen are never held back`() {
        assertFalse(UsageBreaker.shouldPause(config, "com.example.mail", opensToday = 99))
    }

    @Test
    fun `the switch turns everything off`() {
        val off = config.copy(enabled = false)

        assertFalse(UsageBreaker.shouldPause(off, "com.example.social", opensToday = 99))
    }

    @Test
    fun `a threshold of one asks from the first opening`() {
        val eager = config.copy(threshold = 1)

        assertTrue(UsageBreaker.shouldPause(eager, "com.example.social", opensToday = 1))
        assertFalse(UsageBreaker.shouldPause(eager, "com.example.social", opensToday = 0))
    }

    @Test
    fun `impossible values are clamped instead of locking an app away`() {
        val broken = UsageBreakerConfig(threshold = 0, pauseSeconds = 3600)

        val sane = UsageBreaker.sanitise(broken)

        assertEquals(UsageBreakerConfig.THRESHOLD_RANGE.first, sane.threshold)
        assertEquals(UsageBreakerConfig.PAUSE_RANGE.last, sane.pauseSeconds)
    }

    @Test
    fun `a negative pause becomes no pause at all`() {
        val sane = UsageBreaker.sanitise(UsageBreakerConfig(pauseSeconds = -5))

        assertEquals(0, sane.pauseSeconds)
    }

    @Test
    fun `the defaults hold nothing back`() {
        val defaults = UsageBreakerConfig()

        assertFalse(defaults.enabled)
        assertTrue(defaults.packages.isEmpty())
        assertFalse(UsageBreaker.shouldPause(defaults, "com.example.social", opensToday = 999))
    }
}
