package de.moritzstaat.launcher.ui.widget

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moritzstaat.launcher.data.widget.WidgetSlot

/**
 * Renders whatever widgets the user put into one slot. Long pressing the stack offers to
 * remove the widget that is currently showing, which is the only way an app widget id ever
 * leaves the database and the host together.
 */
@Composable
fun HomeWidgetSlot(
    slot: WidgetSlot,
    modifier: Modifier = Modifier,
    ownerKey: String? = null,
) {
    val viewModel: WidgetViewModel = viewModel()
    val all by viewModel.widgets.collectAsStateWithLifecycle()
    val widgets = all.filter { it.slot == slot && it.ownerKey == ownerKey }
    var removalCandidate by remember { mutableStateOf<PlacedWidget?>(null) }

    if (widgets.isEmpty()) return

    WidgetStack(
        widgets = widgets,
        host = viewModel.host,
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(widgets) {
                detectTapGestures(onLongPress = { removalCandidate = widgets.first() })
            },
    )

    removalCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { removalCandidate = null },
            title = { Text(text = "Widget entfernen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.remove(candidate.appWidgetId)
                        removalCandidate = null
                    },
                ) {
                    Text(text = "Entfernen")
                }
            },
            dismissButton = {
                TextButton(onClick = { removalCandidate = null }) {
                    Text(text = "Abbrechen")
                }
            },
        )
    }
}

/** True when the clock should step aside for a widget. */
@Composable
fun hasClockReplacementWidget(): Boolean {
    val viewModel: WidgetViewModel = viewModel()
    val all by viewModel.widgets.collectAsStateWithLifecycle()
    return all.any { it.slot == WidgetSlot.InsteadOfClock && it.ownerKey == null }
}
