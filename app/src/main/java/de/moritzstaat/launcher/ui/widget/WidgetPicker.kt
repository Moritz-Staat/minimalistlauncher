package de.moritzstaat.launcher.ui.widget

import android.app.Activity
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.moritzstaat.launcher.data.widget.WidgetCatalog
import de.moritzstaat.launcher.data.widget.WidgetCatalogSource
import de.moritzstaat.launcher.data.widget.WidgetGroup
import de.moritzstaat.launcher.data.widget.WidgetHostController
import de.moritzstaat.launcher.data.widget.WidgetOption
import de.moritzstaat.launcher.data.widget.WidgetSlot

/**
 * Picks an app widget and takes it through the whole binding dance:
 * allocate an id, try the silent bind, ask the user when the system insists, run the provider's
 * configuration activity if it has one, and only then keep the widget.
 *
 * Every path that does not end in a placed widget releases the allocated id again.
 *
 * The list is grouped by app and folded shut, because the flat provider list the system hands
 * out is a few hundred rows of bare labels - "1×1", "Shortcut", "Widget" - with nothing saying
 * where a row came from or how big it would be. A search field, the provider's own preview and
 * the size in cells answer that without a single extra tap.
 */
@Composable
fun WidgetPicker(
    slot: WidgetSlot,
    ownerKey: String?,
    host: WidgetHostController,
    onPlaced: (Int) -> Unit,
    onDiscard: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val source = remember(host) { WidgetCatalogSource(context, host) }
    val groups by produceState<List<WidgetGroup>?>(initialValue = null, source) {
        value = source.load()
    }

    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<PendingWidget?>(null) }

    val configureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val current = pending ?: return@rememberLauncherForActivityResult
        pending = null
        if (result.resultCode == Activity.RESULT_OK) {
            onPlaced(current.appWidgetId)
            onDismiss()
        } else {
            onDiscard(current.appWidgetId)
        }
    }

    val bindLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val current = pending ?: return@rememberLauncherForActivityResult
        if (result.resultCode != Activity.RESULT_OK) {
            pending = null
            onDiscard(current.appWidgetId)
            return@rememberLauncherForActivityResult
        }
        continueAfterBind(
            context = context,
            host = host,
            pending = current,
            startConfigure = { intent -> configureLauncher.launch(intent) },
            onPlaced = {
                pending = null
                onPlaced(current.appWidgetId)
                onDismiss()
            },
            onDiscard = {
                pending = null
                onDiscard(current.appWidgetId)
            },
        )
    }

    val pick: (WidgetOption) -> Unit = { option ->
        val provider = source.providerFor(option.providerFlat)
        if (provider != null) {
            val appWidgetId = host.allocateId()
            val request = PendingWidget(appWidgetId, provider, slot, ownerKey)
            pending = request
            if (host.bindIfAllowed(appWidgetId, provider)) {
                continueAfterBind(
                    context = context,
                    host = host,
                    pending = request,
                    startConfigure = { intent -> configureLauncher.launch(intent) },
                    onPlaced = {
                        pending = null
                        onPlaced(appWidgetId)
                        onDismiss()
                    },
                    onDiscard = {
                        pending = null
                        onDiscard(appWidgetId)
                    },
                )
            } else {
                bindLauncher.launch(host.createBindIntent(appWidgetId, provider))
            }
        }
    }

    // Searching unfolds the single remaining app on its own: with one hit left, the extra tap
    // is busywork.
    val visible = remember(groups, query) {
        WidgetCatalog.filter(groups.orEmpty(), query)
    }
    val forcedOpen = if (query.isNotBlank() && visible.size == 1) visible.first().packageName else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding(),
    ) {
        Text(
            text = "Widget hinzufügen",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text(text = "App oder Widget suchen") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )

        when {
            groups == null -> Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            visible.isEmpty() -> Text(
                text = if (query.isBlank()) {
                    "Keine App auf dem Gerät bietet Widgets an."
                } else {
                    "Nichts gefunden."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )

            else -> LazyColumn(modifier = Modifier.weight(1f)) {
                items(count = visible.size, key = { visible[it].packageName }) { index ->
                    val group = visible[index]
                    val isOpen = expanded == group.packageName || forcedOpen == group.packageName
                    GroupRow(
                        group = group,
                        expanded = isOpen,
                        onToggle = {
                            expanded = if (expanded == group.packageName) null else group.packageName
                        },
                    )
                    AnimatedVisibility(visible = isOpen) {
                        Column {
                            group.widgets.forEach { option ->
                                WidgetRow(
                                    option = option,
                                    source = source,
                                    onClick = { pick(option) },
                                )
                            }
                        }
                    }
                }
            }
        }

        TextButton(
            onClick = {
                pending?.let { onDiscard(it.appWidgetId) }
                pending = null
                onDismiss()
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(text = "Abbrechen")
        }
    }
}

@Composable
private fun GroupRow(group: WidgetGroup, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = group.appLabel,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = group.widgets.size.toString() + if (expanded) "  ▾" else "  ▸",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WidgetRow(
    option: WidgetOption,
    source: WidgetCatalogSource,
    onClick: () -> Unit,
) {
    // Per row, so only what is on screen is ever decoded.
    val preview by produceState<Bitmap?>(initialValue = null, option.providerFlat) {
        value = source.preview(option.providerFlat)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 40.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = PREVIEW_WIDTH, height = PREVIEW_HEIGHT)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            preview?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                )
            }
        }
        Column(modifier = Modifier.heightIn(min = PREVIEW_HEIGHT)) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = option.sizeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (option.description.isNotBlank()) {
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class PendingWidget(
    val appWidgetId: Int,
    val provider: AppWidgetProviderInfo,
    val slot: WidgetSlot,
    val ownerKey: String?,
)

/**
 * After a successful bind: run the provider's configuration activity if it declares one.
 *
 * Some providers do not export that activity, so the explicit intent is tried first and the
 * host's own launch is the fallback. The fallback delivers its result to the activity rather
 * than here, so the widget is kept optimistically; an abandoned configuration then leaves a
 * widget the user can remove by hand.
 */
private fun continueAfterBind(
    context: Context,
    host: WidgetHostController,
    pending: PendingWidget,
    startConfigure: (Intent) -> Unit,
    onPlaced: () -> Unit,
    onDiscard: () -> Unit,
) {
    if (!host.needsConfiguration(pending.provider)) {
        onPlaced()
        return
    }
    val intent = host.createConfigureIntent(pending.appWidgetId, pending.provider)
    val canLaunch = intent.resolveActivity(context.packageManager) != null
    if (canLaunch) {
        runCatching { startConfigure(intent) }
            .onFailure { fallbackConfigure(context, host, pending, onPlaced, onDiscard) }
        return
    }
    fallbackConfigure(context, host, pending, onPlaced, onDiscard)
}

private fun fallbackConfigure(
    context: Context,
    host: WidgetHostController,
    pending: PendingWidget,
    onPlaced: () -> Unit,
    onDiscard: () -> Unit,
) {
    val activity = context.findActivity()
    if (activity != null &&
        host.startConfigureForResult(activity, pending.appWidgetId, CONFIGURE_REQUEST_CODE)
    ) {
        onPlaced()
    } else {
        onDiscard()
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private val PREVIEW_WIDTH = 84.dp
private val PREVIEW_HEIGHT = 56.dp

private const val CONFIGURE_REQUEST_CODE = 0x571
