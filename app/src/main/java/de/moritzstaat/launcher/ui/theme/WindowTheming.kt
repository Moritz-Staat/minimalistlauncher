package de.moritzstaat.launcher.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Darkens the wallpaper behind the launcher. A plain black layer at the very bottom of the
 * tree, so the text above it keeps its contrast whatever picture the user set.
 */
@Composable
fun WallpaperScrim(dim: Float, modifier: Modifier = Modifier) {
    if (dim <= 0.001f) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = dim.coerceIn(0f, 1f))),
    )
}

/**
 * Blurs whatever is behind the launcher window, which on the home screen is the wallpaper.
 *
 * The system blur is the only way to reach the wallpaper at all; an app cannot read it without
 * the storage permission. It is off while battery saver runs or the device is too slow, which
 * [WindowManager.isCrossWindowBlurEnabled] reports, and then the dim layer carries the look.
 */
@Composable
fun WallpaperBlurEffect(blur: Float) {
    val view = LocalView.current
    val window = view.context.findActivity()?.window

    DisposableEffect(window, blur) {
        if (window == null) return@DisposableEffect onDispose { }
        val radius = (blur.coerceIn(0f, 1f) * MAX_WALLPAPER_BLUR_PX).toInt()
        window.setBlurBehind(radius)
        onDispose { window.setBlurBehind(0) }
    }
}

/**
 * Status bar visibility and the colour of the system bar icons.
 *
 * The navigation bar always stays: hiding it would take the gesture area with it. Light icons
 * belong on a dark theme and dark icons on a light one, otherwise they vanish into the
 * wallpaper.
 */
@Composable
fun SystemBarsEffect(hidden: Boolean, darkTheme: Boolean) {
    val view = LocalView.current
    val window = view.context.findActivity()?.window

    DisposableEffect(window, hidden, darkTheme) {
        if (window == null) return@DisposableEffect onDispose { }
        val controller = WindowInsetsControllerCompat(window, view)
        val bars = WindowInsetsCompat.Type.statusBars()
        if (hidden) controller.hide(bars) else controller.show(bars)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
        onDispose { }
    }
}

private fun Window.setBlurBehind(radius: Int) {
    val supported = (context.getSystemService(WindowManager::class.java))?.isCrossWindowBlurEnabled
    if (supported != true && radius > 0) return

    if (radius > 0) {
        addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    } else {
        clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    }
    attributes = attributes.apply { blurBehindRadius = radius }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val MAX_WALLPAPER_BLUR_PX = 80f
