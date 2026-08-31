package de.moritzstaat.launcher.data.media

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Chooses the accent colour of the media widget from the swatches Palette found in the cover.
 *
 * Pure integer maths on packed ARGB, so the rules can be unit tested without a bitmap: pick
 * the first candidate that is readable on the launcher's background, and fall back rather than
 * return something invisible.
 */
object AccentPicker {

    /** Minimum WCAG contrast ratio against the background the accent is drawn on. */
    const val MIN_CONTRAST = 3.0

    /**
     * @param candidates packed ARGB colours in order of preference, zeroes meaning "absent".
     * @param background the colour the accent will sit on.
     * @param fallback used when nothing is readable enough.
     */
    fun pick(candidates: List<Int>, background: Int, fallback: Int): Int {
        val usable = candidates.firstOrNull { it != 0 && contrastRatio(it, background) >= MIN_CONTRAST }
        if (usable != null) return usable

        // Nothing was readable: brighten or darken the best candidate until it is.
        val best = candidates.firstOrNull { it != 0 } ?: return fallback
        val adjusted = adjustTowardsContrast(best, background)
        return if (contrastRatio(adjusted, background) >= MIN_CONTRAST) adjusted else fallback
    }

    /** WCAG 2.1 contrast ratio, between 1.0 and 21.0. */
    fun contrastRatio(foreground: Int, background: Int): Double {
        val a = relativeLuminance(foreground)
        val b = relativeLuminance(background)
        val lighter = max(a, b)
        val darker = min(a, b)
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun relativeLuminance(color: Int): Double {
        val r = channel((color shr 16) and 0xFF)
        val g = channel((color shr 8) and 0xFF)
        val b = channel(color and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun channel(value: Int): Double {
        val normalized = value / 255.0
        return if (normalized <= 0.03928) {
            normalized / 12.92
        } else {
            ((normalized + 0.055) / 1.055).pow(2.4)
        }
    }

    /** Moves a colour away from the background in steps until it is readable or maxed out. */
    private fun adjustTowardsContrast(color: Int, background: Int): Int {
        val towardsWhite = relativeLuminance(background) < 0.5
        var current = color
        repeat(MAX_ADJUST_STEPS) {
            current = if (towardsWhite) lighten(current) else darken(current)
            if (contrastRatio(current, background) >= MIN_CONTRAST) return current
        }
        return current
    }

    private fun lighten(color: Int): Int = blendChannels(color, 0xFF)

    private fun darken(color: Int): Int = blendChannels(color, 0x00)

    private fun blendChannels(color: Int, target: Int): Int {
        val alpha = color and -0x1000000
        val r = blend((color shr 16) and 0xFF, target)
        val g = blend((color shr 8) and 0xFF, target)
        val b = blend(color and 0xFF, target)
        return alpha or (r shl 16) or (g shl 8) or b
    }

    private fun blend(value: Int, target: Int): Int =
        (value + (target - value) * STEP).toInt().coerceIn(0, 255)

    private const val STEP = 0.18
    private const val MAX_ADJUST_STEPS = 8
}
