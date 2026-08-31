package de.moritzstaat.launcher.ui.alphabet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveScaleTest {

    @Test
    fun `the letter under the finger is at full magnification`() {
        assertEquals(1f, WaveScale.falloff(0f), 1e-5f)
        assertEquals(1f + WaveScale.AMPLITUDE, WaveScale.scaleFor(0f), 1e-5f)
    }

    @Test
    fun `magnification decreases monotonically with distance`() {
        var previous = WaveScale.scaleFor(0f)
        var distance = 0.25f
        while (distance <= 6f) {
            val current = WaveScale.scaleFor(distance)
            assertTrue("scale must shrink at $distance", current < previous)
            previous = current
            distance += 0.25f
        }
    }

    @Test
    fun `far away letters are back to their normal size`() {
        assertTrue(WaveScale.scaleFor(8f) < 1.001f)
    }

    @Test
    fun `the curve is symmetric because callers pass an absolute distance`() {
        assertEquals(WaveScale.falloff(2f), WaveScale.falloff(2f), 1e-6f)
    }

    @Test
    fun `letters are pulled towards the middle, never pushed out`() {
        assertTrue(WaveScale.pullSlotsFor(0f) < 0f)
        assertEquals(0f, WaveScale.pullSlotsFor(20f), 1e-3f)
    }

    @Test
    fun `index maps the touch position onto its slot`() {
        val height = 400f
        val count = 8 // 50 px per slot
        assertEquals(0, WaveScale.indexAt(0f, height, count))
        assertEquals(0, WaveScale.indexAt(49f, height, count))
        assertEquals(1, WaveScale.indexAt(50f, height, count))
        assertEquals(7, WaveScale.indexAt(399f, height, count))
    }

    @Test
    fun `sliding past the ends keeps hold of the first and last letter`() {
        assertEquals(0, WaveScale.indexAt(-120f, 400f, 8))
        assertEquals(7, WaveScale.indexAt(999f, 400f, 8))
    }

    @Test
    fun `no touch and degenerate bars report no active letter`() {
        assertEquals(-1, WaveScale.indexAt(Float.NaN, 400f, 8))
        assertEquals(-1, WaveScale.indexAt(10f, 0f, 8))
        assertEquals(-1, WaveScale.indexAt(10f, 400f, 0))
    }
}
