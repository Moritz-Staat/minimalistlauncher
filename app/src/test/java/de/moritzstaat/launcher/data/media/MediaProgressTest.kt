package de.moritzstaat.launcher.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaProgressTest {

    private val duration = 210_000L // 3:30

    @Test
    fun `while playing the position advances with the wall clock`() {
        val elapsed = MediaProgress.elapsedMs(
            positionMs = 30_000L,
            durationMs = duration,
            updatedAtMs = 1_000L,
            nowMs = 6_000L,
            speed = 1f,
            isPlaying = true,
        )

        assertEquals(35_000L, elapsed)
    }

    @Test
    fun `while paused the reported position stands still`() {
        val elapsed = MediaProgress.elapsedMs(
            positionMs = 30_000L,
            durationMs = duration,
            updatedAtMs = 1_000L,
            nowMs = 600_000L,
            speed = 0f,
            isPlaying = false,
        )

        assertEquals(30_000L, elapsed)
    }

    @Test
    fun `a faster playback speed is taken into account`() {
        val elapsed = MediaProgress.elapsedMs(
            positionMs = 0L,
            durationMs = duration,
            updatedAtMs = 0L,
            nowMs = 10_000L,
            speed = 1.5f,
            isPlaying = true,
        )

        assertEquals(15_000L, elapsed)
    }

    @Test
    fun `the position never runs past the end of the track`() {
        val elapsed = MediaProgress.elapsedMs(
            positionMs = duration - 1_000L,
            durationMs = duration,
            // The session went quiet long ago; without the clamp the bar would overflow.
            updatedAtMs = 0L,
            nowMs = 600_000L,
            speed = 1f,
            isPlaying = true,
        )

        assertEquals(duration, elapsed)
    }

    @Test
    fun `a clock that jumped backwards does not rewind the track`() {
        val elapsed = MediaProgress.elapsedMs(
            positionMs = 30_000L,
            durationMs = duration,
            updatedAtMs = 10_000L,
            nowMs = 5_000L,
            speed = 1f,
            isPlaying = true,
        )

        assertEquals(30_000L, elapsed)
    }

    @Test
    fun `a negative position reads as the start`() {
        val elapsed = MediaProgress.elapsedMs(
            positionMs = -1L,
            durationMs = duration,
            updatedAtMs = 0L,
            nowMs = 0L,
            speed = 1f,
            isPlaying = false,
        )

        assertEquals(0L, elapsed)
    }

    @Test
    fun `an unknown duration still gives a position but no bar`() {
        val elapsed = MediaProgress.elapsedMs(
            positionMs = 5_000L,
            durationMs = 0L,
            updatedAtMs = 0L,
            nowMs = 5_000L,
            speed = 1f,
            isPlaying = true,
        )

        assertEquals(10_000L, elapsed)
        assertNull(MediaProgress.fraction(elapsed, 0L))
        assertNull(MediaProgress.fraction(elapsed, -1L))
    }

    @Test
    fun `the fraction spans the track and stays inside zero to one`() {
        assertEquals(0f, MediaProgress.fraction(0L, duration)!!, 0.001f)
        assertEquals(0.5f, MediaProgress.fraction(105_000L, duration)!!, 0.001f)
        assertEquals(1f, MediaProgress.fraction(duration, duration)!!, 0.001f)
        assertEquals(1f, MediaProgress.fraction(duration * 2, duration)!!, 0.001f)
    }

    @Test
    fun `the label reads as a running time`() {
        assertEquals("0:00", MediaProgress.label(0L))
        assertEquals("0:07", MediaProgress.label(7_400L))
        assertEquals("3:30", MediaProgress.label(210_000L))
        assertEquals("59:59", MediaProgress.label(3_599_000L))
    }

    @Test
    fun `an hour long track gets an hour field`() {
        assertEquals("1:00:00", MediaProgress.label(3_600_000L))
        assertEquals("1:02:03", MediaProgress.label(3_723_000L))
    }

    @Test
    fun `a negative length reads as zero rather than as a minus`() {
        assertEquals("0:00", MediaProgress.label(-5_000L))
    }
}
