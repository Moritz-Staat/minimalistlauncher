package de.moritzstaat.launcher.ui.search

import android.graphics.Rect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.app.AppKey
import de.moritzstaat.launcher.data.icon.IconLoader
import de.moritzstaat.launcher.data.search.SearchResult
import de.moritzstaat.launcher.ui.applist.AppRow
import de.moritzstaat.launcher.ui.applist.AppRowDefaults

/** Results for the current query, apps first and the web search always last. */
@Composable
fun SearchResultsList(
    results: List<SearchResult>,
    iconLoader: IconLoader,
    onLaunchApp: (AppKey, Rect?) -> Unit,
    onLongPressApp: (AppEntry, Rect?) -> Unit,
    onShortcut: (SearchResult.Shortcut) -> Unit,
    onContact: (SearchResult.Contact) -> Unit,
    onWebSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The results replace the app list inside the same sheet, so they need the same inset at
    // the top and room at the bottom for the search field that sits over them.
    val insets = PaddingValues(
        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TOP_GAP,
        bottom = BOTTOM_SPACE,
    )

    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = insets) {
        items(count = results.size, key = { results[it].id }) { index ->
            when (val result = results[index]) {
                is SearchResult.App -> AppRow(
                    entry = result.entry,
                    iconLoader = iconLoader,
                    highlightIndices = result.matchedIndices,
                    onClick = { bounds -> onLaunchApp(result.entry.key, bounds) },
                    onLongClick = { bounds -> onLongPressApp(result.entry, bounds) },
                )

                is SearchResult.Shortcut -> SecondaryRow(
                    title = result.label,
                    subtitle = result.appLabel,
                    onClick = { onShortcut(result) },
                )

                is SearchResult.Contact -> SecondaryRow(
                    title = result.hit.displayName,
                    subtitle = "Kontakt",
                    onClick = { onContact(result) },
                )

                is SearchResult.WebSearch -> SecondaryRow(
                    title = "Im Web suchen",
                    subtitle = result.query,
                    onClick = { onWebSearch(result.query) },
                )
            }
        }
    }
}

/**
 * Rows that are not apps. Same height and same left edge as an app row so the list keeps one
 * rhythm, but without an icon slot to fill.
 */
@Composable
private fun SecondaryRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppRowDefaults.Height)
            .clickable(onClick = onClick)
            .padding(horizontal = AppRowDefaults.HorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(AppRowDefaults.IconSize + AppRowDefaults.IconGap))
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val TOP_GAP = 12.dp

/** Enough that the last result clears the search field. */
private val BOTTOM_SPACE = 96.dp
