package de.moritzstaat.launcher.data.icon

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.getSystemService
import de.moritzstaat.launcher.data.app.AppKey
import de.moritzstaat.launcher.data.app.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns a launchable activity into a cached bitmap of exactly the size the list draws.
 *
 * Stage 11 puts icon packs, dot mode and monochrome mode in front of this; the cache key
 * already carries a variant so those modes can coexist without flushing each other.
 */
class IconLoader(
    context: Context,
    private val appRepository: AppRepository,
    private val cache: IconCache,
) {

    private val appContext = context.applicationContext
    private val densityDpi = appContext.resources.configuration.densityDpi

    /** Icon edge length used by the app list, in pixels. */
    val defaultSizePx: Int =
        appContext.getSystemService<ActivityManager>()?.launcherLargeIconSize
            ?: (48 * appContext.resources.displayMetrics.density).toInt()

    suspend fun load(
        key: AppKey,
        sizePx: Int = defaultSizePx,
        variant: String = VARIANT_ORIGINAL,
    ): Bitmap? {
        val cacheKey = cacheKey(key, sizePx, variant)
        cache[cacheKey]?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            val drawable = appRepository.activityInfo(key)?.getIcon(densityDpi) ?: return@withContext null
            drawable.toBitmap(sizePx)
        } ?: return null
        cache[cacheKey] = bitmap
        return bitmap
    }

    private fun cacheKey(key: AppKey, sizePx: Int, variant: String) =
        "${key.flatten()}|$sizePx|$variant"

    companion object {
        const val VARIANT_ORIGINAL = "orig"
    }
}

/** Rasterises a drawable at a fixed edge length; reuses the bitmap a BitmapDrawable already has. */
internal fun Drawable.toBitmap(sizePx: Int): Bitmap {
    if (this is BitmapDrawable) {
        val source = bitmap
        if (source != null && source.width == sizePx && source.height == sizePx) return source
    }
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, sizePx, sizePx)
    draw(canvas)
    return bitmap
}
