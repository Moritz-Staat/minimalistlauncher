package de.moritzstaat.launcher.ui.weather

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * One line under the clock: symbol, temperature, condition and today's range.
 *
 * Tapping it fetches a new reading. There is no weather app to open — the launcher does not
 * know which one the user would want, and guessing wrong is worse than doing the obvious thing.
 */
@Composable
fun WeatherSlot(modifier: Modifier = Modifier) {
    val viewModel: WeatherViewModel = viewModel()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()

    // Coming back to the home screen is the moment a stale reading is worth replacing.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    AnimatedVisibility(
        visible = snapshot != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        snapshot?.let { weather ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.refresh(force = true) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = weather.symbol, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = weather.temperatureLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = weather.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = weather.highLowLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
