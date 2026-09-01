package de.moritzstaat.launcher.data.widget

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the installed widget providers into the model the picker shows.
 *
 * Labels and descriptions come out of other apps' resources, which is file system work, so the
 * whole catalogue is built once off the main thread. Previews are deliberately *not* part of
 * it: they are full bitmaps and only the handful of rows actually on screen needs one.
 */
class WidgetCatalogSource(
    private val context: Context,
    private val host: WidgetHostController,
) {

    private var byFlat: Map<String, AppWidgetProviderInfo> = emptyMap()

    suspend fun load(): List<WidgetGroup> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val density = context.resources.displayMetrics.density
        val providers = host.installedProviders()

        byFlat = providers.associateBy { it.provider.flattenToString() }

        val appLabels = HashMap<String, String>()
        val options = providers.mapNotNull { provider ->
            val packageName = provider.provider.packageName
            val applicationInfo = provider.activityInfo?.applicationInfo
            if (applicationInfo != null && packageName !in appLabels) {
                appLabels[packageName] =
                    packageManager.getApplicationLabel(applicationInfo).toString()
            }

            val label = provider.loadLabel(packageManager)?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            packageName to WidgetOption(
                providerFlat = provider.provider.flattenToString(),
                label = label,
                description = runCatching { provider.loadDescription(context) }
                    .getOrNull()?.toString().orEmpty(),
                // The framework reports these already scaled to pixels.
                columns = WidgetCatalog.cellsFor((provider.minWidth / density).toInt()),
                rows = WidgetCatalog.cellsFor((provider.minHeight / density).toInt()),
                resizable = provider.resizeMode != AppWidgetProviderInfo.RESIZE_NONE,
            )
        }

        WidgetCatalog.group(options, appLabels)
    }

    fun providerFor(providerFlat: String): AppWidgetProviderInfo? = byFlat[providerFlat]

    /**
     * The provider's own preview, or its icon when it ships none.
     *
     * Scaled down on the way in: some previews are full screenshots, and the picker draws them
     * at thumbnail size anyway.
     */
    suspend fun preview(providerFlat: String): Bitmap? = withContext(Dispatchers.IO) {
        val provider = byFlat[providerFlat] ?: return@withContext null
        val drawable: Drawable = runCatching { provider.loadPreviewImage(context, 0) }.getOrNull()
            ?: runCatching { provider.loadIcon(context, 0) }.getOrNull()
            ?: return@withContext null

        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: FALLBACK_PX
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: FALLBACK_PX
        val scale = minOf(1f, MAX_PREVIEW_PX.toFloat() / maxOf(width, height))

        runCatching {
            drawable.toBitmap(
                width = maxOf(1, (width * scale).toInt()),
                height = maxOf(1, (height * scale).toInt()),
            )
        }.getOrNull()
    }

    private companion object {
        const val MAX_PREVIEW_PX = 480
        const val FALLBACK_PX = 144
    }
}
