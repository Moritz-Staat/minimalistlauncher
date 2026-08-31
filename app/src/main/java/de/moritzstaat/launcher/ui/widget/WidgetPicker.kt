package de.moritzstaat.launcher.ui.widget

import android.app.Activity
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.moritzstaat.launcher.data.widget.WidgetHostController
import de.moritzstaat.launcher.data.widget.WidgetSlot

/**
 * Picks an app widget and takes it through the whole binding dance:
 * allocate an id, try the silent bind, ask the user when the system insists, run the provider's
 * configuration activity if it has one, and only then keep the widget.
 *
 * Every path that does not end in a placed widget releases the allocated id again.
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
    val providers = remember { host.installedProviders().sortedBy { it.loadLabel(context.packageManager) } }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding(),
    ) {
        Text(
            text = "Widget hinzufuegen",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(count = providers.size, key = { providers[it].provider.flattenToString() }) { index ->
                val provider = providers[index]
                ProviderRow(
                    label = provider.loadLabel(context.packageManager),
                    onClick = {
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
                    },
                )
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
private fun ProviderRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    )
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

private const val CONFIGURE_REQUEST_CODE = 0x571
