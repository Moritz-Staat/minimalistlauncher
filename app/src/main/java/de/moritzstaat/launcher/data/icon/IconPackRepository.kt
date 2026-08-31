package de.moritzstaat.launcher.data.icon

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat

/** An installed icon pack, as offered in the settings. */
data class IconPackInfo(
    val packageName: String,
    val label: String,
)

/** A loaded pack: its resources, its appfilter and every drawable name it advertises. */
class LoadedIconPack(
    val packageName: String,
    val resources: Resources,
    val filter: IconPackFilter,
    val drawableNames: Set<String>,
) {
    fun drawable(name: String): Drawable? {
        val id = resources.getIdentifier(name, "drawable", packageName)
        if (id == 0) return null
        return runCatching { ResourcesCompat.getDrawable(resources, id, null) }.getOrNull()
    }
}

/**
 * Finds and loads ADW and Nova style icon packs.
 *
 * Discovery goes through the two well known intent filters. The manifest declares exactly
 * those two actions under <queries>, so no broad package visibility is needed.
 */
class IconPackRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager

    fun installedPacks(): List<IconPackInfo> {
        val found = LinkedHashMap<String, IconPackInfo>()
        for (action in PACK_ACTIONS) {
            val intent = Intent(action)
            val resolved = runCatching {
                packageManager.queryIntentActivities(intent, 0)
            }.getOrDefault(emptyList())
            for (info in resolved) {
                val packageName = info.activityInfo.packageName
                found.putIfAbsent(
                    packageName,
                    IconPackInfo(packageName, info.loadLabel(packageManager).toString()),
                )
            }
        }
        return found.values.sortedBy { it.label }
    }

    fun load(packageName: String): LoadedIconPack? {
        val resources = runCatching {
            packageManager.getResourcesForApplication(packageName)
        }.getOrNull() ?: return null

        val filter = readFilter(resources, packageName) ?: return null
        val names = readDrawableNames(resources, packageName) + filter.componentToDrawable.values
        return LoadedIconPack(packageName, resources, filter, names)
    }

    private fun readFilter(resources: Resources, packageName: String): IconPackFilter? {
        resources.xmlOrNull(packageName, APPFILTER)?.let { return IconPackParser.parse(it) }
        return runCatching {
            resources.assets.open("$APPFILTER.xml").use { IconPackParser.parse(it) }
        }.getOrNull()
    }

    private fun readDrawableNames(resources: Resources, packageName: String): Set<String> {
        resources.xmlOrNull(packageName, DRAWABLE_LIST)?.let {
            return IconPackParser.parseDrawableNames(it)
        }
        return runCatching {
            resources.assets.open("$DRAWABLE_LIST.xml").use { IconPackParser.parseDrawableNames(it) }
        }.getOrDefault(emptySet())
    }

    private fun Resources.xmlOrNull(packageName: String, name: String) = runCatching {
        val id = getIdentifier(name, "xml", packageName)
        if (id == 0) null else getXml(id)
    }.getOrNull()

    private companion object {
        val PACK_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
        )
        const val APPFILTER = "appfilter"
        const val DRAWABLE_LIST = "drawable"
    }
}
