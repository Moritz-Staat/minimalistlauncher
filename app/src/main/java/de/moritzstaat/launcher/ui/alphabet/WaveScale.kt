package de.moritzstaat.launcher.ui.alphabet

import kotlin.math.exp

/**
 * Shape of the alphabet bar's magnification. Pure maths, so the curve can be checked by a
 * plain unit test instead of by eye on the device.
 */
object WaveScale {

    /** How much larger the letter directly under the finger gets, on top of 1.0. */
    const val AMPLITUDE = 1.15f

    /** Width of the bell, in multiples of one letter slot. */
    const val SIGMA_SLOTS = 1.9f

    /** How far magnified letters slide towards the middle of the screen, in slot heights. */
    const val PULL_SLOTS = 1.4f

    /**
     * Gaussian falloff of the magnification.
     *
     * @param distanceSlots distance between letter and finger, measured in slot heights.
     * @return 0 for untouched, 1 for right under the finger.
     */
    fun falloff(distanceSlots: Float): Float {
        val x = distanceSlots / SIGMA_SLOTS
        return exp(-0.5f * x * x)
    }

    fun scaleFor(distanceSlots: Float): Float = 1f + AMPLITUDE * falloff(distanceSlots)

    /** Negative, because the bar sits on the right edge and grows towards the middle. */
    fun pullSlotsFor(distanceSlots: Float): Float = -PULL_SLOTS * falloff(distanceSlots)

    /**
     * Index of the letter under [touchY], or -1 when nothing is touched.
     *
     * Out-of-range values are clamped rather than rejected: sliding a finger past the top or
     * the bottom of the bar should stay on the first or last letter instead of losing grip.
     */
    fun indexAt(touchY: Float, barHeightPx: Float, count: Int): Int {
        if (count <= 0 || barHeightPx <= 0f || touchY.isNaN()) return -1
        val slot = barHeightPx / count
        val index = (touchY / slot).toInt()
        return index.coerceIn(0, count - 1)
    }
}
