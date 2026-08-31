package de.moritzstaat.launcher.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moritzstaat.launcher.data.app.AppKey
import de.moritzstaat.launcher.data.icon.IconLoader

/**
 * Draws one app icon. Loading goes through [IconLoader], so a warm cache resolves on the
 * first composition and no bitmap is decoded twice.
 */
@Composable
fun AppIcon(
    appKey: AppKey,
    iconLoader: IconLoader,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    // Redraw when the icon style, the pack or a manual override changes.
    val config by iconLoader.config.collectAsStateWithLifecycle()
    val bitmap by produceState<ImageBitmap?>(initialValue = null, appKey, sizePx, config.signature) {
        value = iconLoader.load(appKey, sizePx)?.asImageBitmap()
    }

    val image = bitmap
    if (image == null) {
        Box(modifier = modifier.size(size))
    } else {
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            modifier = modifier.size(size),
        )
    }
}
