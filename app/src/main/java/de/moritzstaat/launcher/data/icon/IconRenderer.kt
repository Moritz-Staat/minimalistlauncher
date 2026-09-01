package de.moritzstaat.launcher.data.icon

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.palette.graphics.Palette

/** How icons are drawn. */
enum class IconStyle {
    /** Whatever the app ships. */
    Original,

    /** Drawn from the selected icon pack, with the pack's own recipe for the rest. */
    IconPack,

    /** One filled dot per app in the icon's dominant colour. Covers every app. */
    Dots,

    /** The monochrome layer of the adaptive icon, greyscale where there is none. */
    Monochrome,

    /** No icons at all: a plain list of names, which is the most minimal the launcher gets. */
    None,
    ;

    companion object {
        fun fromStorage(value: String?): IconStyle =
            entries.firstOrNull { it.name == value } ?: Original
    }
}

/**
 * Turns a source drawable into the bitmap the list draws.
 *
 * Every mode ends in a square bitmap of exactly the requested size, so the list geometry never
 * depends on what an app happens to ship.
 */
object IconRenderer {

    fun render(
        source: Drawable,
        sizePx: Int,
        style: IconStyle,
        pack: LoadedIconPack?,
        packDrawable: Drawable?,
    ): Bitmap = when (style) {
        IconStyle.Original -> source.rasterise(sizePx)
        IconStyle.Dots -> dot(source, sizePx)
        IconStyle.Monochrome -> monochrome(source, sizePx)
        IconStyle.IconPack -> when {
            packDrawable != null -> packDrawable.rasterise(sizePx)
            pack != null -> applyPackRecipe(source, sizePx, pack)
            else -> source.rasterise(sizePx)
        }

        // Not reached: IconLoader answers null for this style and the rows leave the slot out
        // entirely, so nothing ever asks for a bitmap.
        IconStyle.None -> source.rasterise(sizePx)
    }

    /** A filled circle in the icon's dominant tone. Works for every app, including new ones. */
    fun dot(source: Drawable, sizePx: Int): Bitmap {
        val sample = source.rasterise(sizePx)
        val color = dominantColor(sample)
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val radius = sizePx * DOT_RADIUS_FRACTION
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, radius, paint)
        return output
    }

    /**
     * Prefers the monochrome layer the adaptive icon declares; without one the icon is drawn
     * through a saturation-zero colour matrix, which still gives a usable silhouette.
     */
    fun monochrome(source: Drawable, sizePx: Int): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && source is AdaptiveIconDrawable) {
            source.monochrome?.let { layer ->
                val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                layer.setBounds(0, 0, sizePx, sizePx)
                layer.setTint(Color.WHITE)
                layer.draw(canvas)
                return output
            }
        }
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        val layer = canvas.saveLayer(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)
        source.setBounds(0, 0, sizePx, sizePx)
        source.draw(canvas)
        canvas.restoreToCount(layer)
        return output
    }

    /**
     * The pack's fallback for apps it does not cover: scale the original icon, put it on the
     * pack background, cut it with the mask and lay the overlay on top.
     */
    private fun applyPackRecipe(source: Drawable, sizePx: Int, pack: LoadedIconPack): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val bounds = Rect(0, 0, sizePx, sizePx)

        pack.iconBack()?.let { back ->
            back.bounds = bounds
            back.draw(canvas)
        }

        val scaled = (sizePx * pack.filter.scale.coerceIn(MIN_SCALE, MAX_SCALE)).toInt()
        val inset = (sizePx - scaled) / 2
        source.setBounds(inset, inset, inset + scaled, inset + scaled)
        source.draw(canvas)

        pack.filter.iconMask?.let { name ->
            pack.drawable(name)?.let { mask ->
                val maskBitmap = mask.rasterise(sizePx)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                }
                canvas.drawBitmap(maskBitmap, 0f, 0f, paint)
            }
        }

        pack.filter.iconUpon?.let { name ->
            pack.drawable(name)?.let { upon ->
                upon.bounds = bounds
                upon.draw(canvas)
            }
        }

        return output
    }

    /** Packs ship several backgrounds; one is picked deterministically per icon. */
    private fun LoadedIconPack.iconBack(): Drawable? {
        val backs = filter.iconBacks
        if (backs.isEmpty()) return null
        return drawable(backs.first())
    }

    fun dominantColor(bitmap: Bitmap): Int {
        val palette = runCatching { Palette.from(bitmap).clearFilters().generate() }.getOrNull()
            ?: return DEFAULT_DOT_COLOR
        return palette.getVibrantColor(0).takeIf { it != 0 }
            ?: palette.getDominantColor(DEFAULT_DOT_COLOR)
    }

    private const val DOT_RADIUS_FRACTION = 0.32f
    private const val DEFAULT_DOT_COLOR = 0xFF9E9E9E.toInt()
    private const val MIN_SCALE = 0.3f
    private const val MAX_SCALE = 1f
}

/** Rasterises a drawable at a fixed edge length. */
internal fun Drawable.rasterise(sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, sizePx, sizePx)
    draw(canvas)
    return bitmap
}
