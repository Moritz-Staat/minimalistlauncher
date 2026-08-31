package de.moritzstaat.launcher.ui.icons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moritzstaat.launcher.data.icon.IconStyle

/** Icon style and icon pack, as a block inside the settings. */
@Composable
fun IconSettingsSection(modifier: Modifier = Modifier) {
    val viewModel: IconSettingsViewModel = viewModel()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val packs by viewModel.packs.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = "Icons", style = MaterialTheme.typography.titleMedium)
        IconStyle.entries.forEach { style ->
            SelectableRow(
                label = styleLabel(style),
                selected = config.style == style,
                enabled = style != IconStyle.IconPack || config.pack != null,
                onClick = { viewModel.setStyle(style) },
            )
        }

        Text(
            text = "Icon-Pack",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (packs.isEmpty()) {
            Text(
                text = "Kein Icon-Pack installiert.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        packs.forEach { pack ->
            SelectableRow(
                label = pack.label,
                selected = config.pack?.packageName == pack.packageName,
                onClick = { viewModel.setPack(pack.packageName) },
            )
        }
    }
}

private fun styleLabel(style: IconStyle): String = when (style) {
    IconStyle.Original -> "Original"
    IconStyle.IconPack -> "Icon-Pack"
    IconStyle.Dots -> "Punkte"
    IconStyle.Monochrome -> "Monochrom"
}

@Composable
private fun SelectableRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Text(
        text = if (selected) "$label ✓" else label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
    )
}
