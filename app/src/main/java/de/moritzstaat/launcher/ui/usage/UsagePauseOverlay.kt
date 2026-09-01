package de.moritzstaat.launcher.ui.usage

import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.moritzstaat.launcher.data.app.AppKey
import kotlinx.coroutines.delay

/** The launch the user asked for, held back until they have seen the number. */
data class PauseRequest(
    val appKey: AppKey,
    val label: String,
    val opensToday: Int,
    val pauseSeconds: Int,
    val sourceBounds: Rect?,
)

/**
 * The pause screen: how often the app has been opened today, a few seconds of nothing, and
 * then the choice.
 *
 * It is deliberately not a block. The app can always be opened; the point is that the decision
 * happens once consciously instead of ten times out of habit.
 */
@Composable
fun UsagePauseOverlay(
    request: PauseRequest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var remaining by remember(request) { mutableIntStateOf(request.pauseSeconds) }

    LaunchedEffect(request) {
        while (remaining > 0) {
            delay(SECOND_MS)
            remaining -= 1
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = request.label,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Heute schon " + request.opensToday + " mal geöffnet.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = if (remaining > 0) {
                "Noch " + remaining + " Sekunden."
            } else {
                "Immer noch?"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 24.dp),
        )
        Button(
            onClick = onConfirm,
            enabled = remaining == 0,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(text = "Trotzdem öffnen")
        }
        TextButton(onClick = onDismiss) {
            Text(text = "Lieber nicht")
        }
    }
}

private const val SECOND_MS = 1_000L
