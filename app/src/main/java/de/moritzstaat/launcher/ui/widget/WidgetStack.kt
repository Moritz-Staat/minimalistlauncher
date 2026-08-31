package de.moritzstaat.launcher.ui.widget

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.moritzstaat.launcher.data.widget.WidgetHostController

/**
 * A stack of widgets in one slot, swiped horizontally with a dot indicator.
 *
 * Horizontal swiping is confined to this component; the app list itself never scrolls sideways
 * so the row pop-up gesture of stage 10 stays free.
 */
@Composable
fun WidgetStack(
    widgets: List<PlacedWidget>,
    host: WidgetHostController,
    modifier: Modifier = Modifier,
    height: Dp = DEFAULT_HEIGHT,
) {
    if (widgets.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { widgets.size })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
        ) { page ->
            AppWidgetFrame(
                appWidgetId = widgets[page].appWidgetId,
                host = host,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (widgets.size > 1) {
            PageIndicator(
                pageCount = widgets.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
        }
    }
}

/** Embeds the platform host view and keeps it told about the space it has. */
@Composable
fun AppWidgetFrame(
    appWidgetId: Int,
    host: WidgetHostController,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val widthDp = with(density) { constraints.maxWidth.toDp().value }
        val heightDp = with(density) { constraints.maxHeight.toDp().value }

        AndroidView(
            factory = { context ->
                host.createView(context, appWidgetId) ?: android.widget.FrameLayout(context)
            },
            update = { view ->
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                if (view is android.appwidget.AppWidgetHostView) {
                    host.updateSize(view, widthDp, heightDp)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        repeat(pageCount) { index ->
            val active = index == currentPage
            Box(
                modifier = Modifier
                    .size(if (active) 6.dp else 4.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                    ),
            )
        }
    }
}

private val DEFAULT_HEIGHT = 110.dp
