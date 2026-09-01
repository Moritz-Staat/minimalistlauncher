package de.moritzstaat.launcher.ui.theme

import de.moritzstaat.launcher.data.media.AccentPicker
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Derives the shades a colour scheme needs from a single accent colour.
 *
 * Material's own tonal palettes come from the wallpaper and are only available in the dynamic
 * schemes; for the manually picked accent the launcher builds its own shades. Packed ARGB in,
 * packed ARGB out, so every rule here is unit testable without a device.
 */
object AccentPalette {

    /** The same colour at a different HSL lightness, hue and saturation untouched. */
    fun tone(argb: Int, lightness: Float): Int {
        val hsl = toHsl(argb)
        return fromHsl(hsl[0], hsl[1], lightness.coerceIn(0f, 1f), alphaOf(argb))
    }

    /** Pulls the colour towards grey; 0 keeps it, 1 makes it fully grey. */
    fun desaturate(argb: Int, amount: Float): Int {
        val hsl = toHsl(argb)
        val saturation = hsl[1] * (1f - amount.coerceIn(0f, 1f))
        return fromHsl(hsl[0], saturation, hsl[2], alphaOf(argb))
    }

    /** Rotates the hue, used for the secondary and tertiary roles. */
    fun hueShift(argb: Int, degrees: Float): Int {
        val hsl = toHsl(argb)
        val hue = ((hsl[0] + degrees) % 360f + 360f) % 360f
        return fromHsl(hue, hsl[1], hsl[2], alphaOf(argb))
    }

    /** HSL lightness, 0..1. */
    fun lightnessOf(argb: Int): Float = toHsl(argb)[2]

    /** Black or white, whichever is easier to read on [argb]. */
    fun contentColorFor(argb: Int): Int {
        val onBlack = AccentPicker.contrastRatio(BLACK, argb)
        val onWhite = AccentPicker.contrastRatio(WHITE, argb)
        return if (onBlack >= onWhite) BLACK else WHITE
    }

    private fun alphaOf(argb: Int): Int = (argb ushr 24) and 0xFF

    /** @return hue in degrees, saturation and lightness in 0..1. */
    fun toHsl(argb: Int): FloatArray {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f

        val maximum = max(r, max(g, b))
        val minimum = min(r, min(g, b))
        val delta = maximum - minimum
        val lightness = (maximum + minimum) / 2f

        if (delta < 1e-6f) return floatArrayOf(0f, 0f, lightness)

        val saturation = delta / (1f - abs(2f * lightness - 1f))
        val hue = when (maximum) {
            r -> 60f * (((g - b) / delta) % 6f)
            g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        return floatArrayOf((hue % 360f + 360f) % 360f, saturation.coerceIn(0f, 1f), lightness)
    }

    fun fromHsl(hue: Float, saturation: Float, lightness: Float, alpha: Int = 0xFF): Int {
        val chroma = (1f - abs(2f * lightness - 1f)) * saturation
        val section = ((hue % 360f + 360f) % 360f) / 60f
        val second = chroma * (1f - abs((section % 2f) - 1f))
        val offset = lightness - chroma / 2f

        val (r, g, b) = when (section.toInt()) {
            0 -> Triple(chroma, second, 0f)
            1 -> Triple(second, chroma, 0f)
            2 -> Triple(0f, chroma, second)
            3 -> Triple(0f, second, chroma)
            4 -> Triple(second, 0f, chroma)
            else -> Triple(chroma, 0f, second)
        }
        return (alpha shl 24) or
            (channel(r + offset) shl 16) or
            (channel(g + offset) shl 8) or
            channel(b + offset)
    }

    private fun channel(value: Float): Int = (value * 255f).roundToInt().coerceIn(0, 255)

    const val BLACK = 0xFF000000.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
}
