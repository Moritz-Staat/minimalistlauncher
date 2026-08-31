package de.moritzstaat.launcher.data.icon

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.getSystemService
import de.moritzstaat.launcher.data.app.AppKey
import de.moritzstaat.launcher.data.app.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Everything the icon pipeline needs to know at the moment of drawing.
 *
 * [signature] changes whenever the result would look different, which is what invalidates the
 * cache and makes the list redraw its icons.
 */
data class IconConfig(
    val style: IconStyle = IconStyle.Original,
    val pack: LoadedIconPack? = null,
    val overrides: Map<String, IconOverride> = emptyMap(),
) {
    val signature: String
        get() = "${style.name}|${pack?.packageName ?: "-"}|${overrides.hashCode()}"
}

/** One hand picked icon: a drawable out of an icon pack. */
data class IconOverride(
    val iconPackPackage: String?,
    val drawableName: String?,
)

/**
 * Turns a launchable activity into a cached bitmap of exactly the size the list draws.
 *
 * The order is always the same: an icon the user picked by hand wins, then the current style,
 * and only then whatever the app itself ships.
 */
class IconLoader(
    context: Context,
    private val appRepository: AppRepository,
    private val cache: IconCache,
    private val iconPackRepository: IconPackRepository,
    val config: StateFlow<IconConfig>,
) {

    private val appContext = context.applicationContext
    private val densityDpi = appContext.resources.configuration.densityDpi

    /** Icon edge length used by the app list, in pixels. */
    val defaultSizePx: Int =
        appContext.getSystemService<ActivityManager>()?.launcherLargeIconSize
            ?: (48 * appContext.resources.displayMetrics.density).toInt()

    suspend fun load(key: AppKey, sizePx: Int = defaultSizePx): Bitmap? {
        val current = config.value
        val cacheKey = "${key.flatten()}|$sizePx|${current.signature}"
        cache[cacheKey]?.let { return it }

        val bitmap = withContext(Dispatchers.IO) { render(key, sizePx, current) } ?: return null
        cache[cacheKey] = bitmap
        return bitmap
    }

    private fun render(key: AppKey, sizePx: Int, current: IconConfig): Bitmap? {
        val source = appRepository.activityInfo(key)?.getIcon(densityDpi) ?: return null
        val flat = key.flatten()

        // A hand picked icon wins over every style, and survives a change of pack.
        current.overrides[flat]?.let { override ->
            val packName = override.iconPackPackage
            val drawableName = override.drawableName
            if (packName != null && drawableName != null) {
                val pack = if (packName == current.pack?.packageName) {
                    current.pack
                } else {
                    iconPackRepository.load(packName)
                }
                pack?.drawable(drawableName)?.let { return it.rasterise(sizePx) }
            }
        }

        val packDrawable = if (current.style == IconStyle.IconPack) {
            current.pack?.drawableFor(key, appRepository.labelOf(key))
        } else {
            null
        }

        return IconRenderer.render(
            source = source,
            sizePx = sizePx,
            style = current.style,
            pack = current.pack,
            packDrawable = packDrawable,
        )
    }

    /** Names of the drawables of the active pack, for the manual icon chooser. */
    fun packDrawableNames(): List<String> = config.value.pack?.drawableNames?.sorted().orEmpty()

    fun activePackPackage(): String? = config.value.pack?.packageName
}

/**
 * The drawable an icon pack has for one app: its explicit entry first, otherwise a confident
 * guess from the drawable names.
 */
internal fun LoadedIconPack.drawableFor(key: AppKey, label: String) =
    filter.componentToDrawable["${key.packageName}/${key.className}"]?.let { drawable(it) }
        ?: IconPackMatcher.findDrawable(drawableNames, label, key.packageName)?.let { drawable(it) }

private fun AppRepository.labelOf(key: AppKey): String =
    activityInfo(key)?.label?.toString().orEmpty()
