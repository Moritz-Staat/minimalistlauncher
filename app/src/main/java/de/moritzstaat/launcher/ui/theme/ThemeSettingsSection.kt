package de.moritzstaat.launcher.ui.theme

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moritzstaat.launcher.data.settings.ClockStyle
import de.moritzstaat.launcher.data.settings.ColorMode
import de.moritzstaat.launcher.data.settings.HourFormat
import de.moritzstaat.launcher.data.settings.ThemeConfig
import de.moritzstaat.launcher.data.settings.ThemePreset
import de.moritzstaat.launcher.ui.common.SelectableRow
import de.moritzstaat.launcher.ui.common.ToggleRow
import kotlin.math.roundToInt

/** Clock, colours, wallpaper and font, as a block inside the settings. */
@Composable
fun ThemeSettingsSection(modifier: Modifier = Modifier) {
    val viewModel: ThemeViewModel = viewModel()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val fontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::setFont) }
    val themeImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importTheme) }
    val themeExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(THEME_MIME_TYPE),
    ) { uri -> uri?.let(viewModel::exportTheme) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = "Darstellung", style = MaterialTheme.typography.titleMedium)

        SubTitle("Vorlagen")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ThemePreset.entries.forEach { preset ->
                TextButton(onClick = { viewModel.applyPreset(preset) }) {
                    Text(text = preset.label)
                }
            }
        }

        SubTitle("Uhr")
        ClockStyle.entries.forEach { style ->
            SelectableRow(
                label = clockStyleLabel(style),
                selected = theme.clockStyle == style,
                onClick = { viewModel.setClockStyle(style) },
            )
        }
        HourFormat.entries.forEach { format ->
            SelectableRow(
                label = hourFormatLabel(format),
                selected = theme.hourFormat == format,
                onClick = { viewModel.setHourFormat(format) },
            )
        }
        ToggleRow(
            label = "Datum anzeigen",
            checked = theme.showDate,
            onCheckedChange = viewModel::setShowDate,
        )

        SubTitle("Farben")
        ColorMode.entries.forEach { mode ->
            SelectableRow(
                label = colorModeLabel(mode),
                selected = theme.colorMode == mode,
                onClick = { viewModel.setColorMode(mode) },
            )
        }
        AccentSwatches(selected = theme.accentArgb, onPick = viewModel::setAccent)
        ToggleRow(
            label = "Dunkles Theme",
            checked = theme.darkTheme,
            onCheckedChange = viewModel::setDarkTheme,
        )

        SubTitle("Hintergrund")
        SliderRow(
            label = "Abdunkeln",
            value = theme.wallpaperDim,
            onValueChange = viewModel::setWallpaperDim,
        )
        SliderRow(
            label = "Weichzeichnen",
            value = theme.wallpaperBlur,
            onValueChange = viewModel::setWallpaperBlur,
        )
        ToggleRow(
            label = "Statusleiste ausblenden",
            checked = theme.hideStatusBar,
            onCheckedChange = viewModel::setHideStatusBar,
        )

        SubTitle("Schrift")
        Text(
            text = if (theme.fontPath.isBlank()) "Systemschrift" else "Eigene Schrift",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { fontPicker.launch(FONT_MIME_TYPES) }) {
                Text(text = "Schrift wählen")
            }
            TextButton(
                onClick = viewModel::clearFont,
                enabled = theme.fontPath.isNotBlank(),
            ) {
                Text(text = "Zurücksetzen")
            }
        }

        SubTitle("Theme sichern")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { themeExporter.launch(THEME_FILE_NAME) }) {
                Text(text = "Exportieren")
            }
            TextButton(onClick = { themeImporter.launch(THEME_IMPORT_TYPES) }) {
                Text(text = "Importieren")
            }
        }
        message?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = viewModel::clearMessage),
            )
        }
    }
}

/** A handful of accents. Anything finer needs a colour wheel, which this sheet is not. */
@Composable
private fun AccentSwatches(selected: Int, onPick: (Int) -> Unit) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ACCENTS.forEach { accent ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(accent), CircleShape)
                    .border(
                        width = if (accent == selected) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                    )
                    .clickable { onPick(accent) },
            )
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label + " " + (value * 100).roundToInt() + " %",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..1f)
    }
}

@Composable
private fun SubTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 12.dp),
    )
}

private fun clockStyleLabel(style: ClockStyle): String = when (style) {
    ClockStyle.Large -> "Groß"
    ClockStyle.Narrow -> "Schmal"
    ClockStyle.TwoLine -> "Zweizeilig"
    ClockStyle.Text -> "Als Text"
}

private fun hourFormatLabel(format: HourFormat): String = when (format) {
    HourFormat.System -> "Zeitformat wie im System"
    HourFormat.TwelveHour -> "12 Stunden"
    HourFormat.TwentyFourHour -> "24 Stunden"
}

private fun colorModeLabel(mode: ColorMode): String = when (mode) {
    ColorMode.MaterialYou -> "Material You"
    ColorMode.Manual -> "Eigene Akzentfarbe"
    ColorMode.ExtraDark -> "Extra dunkel"
}

private val ACCENTS = listOf(
    ThemeConfig.DEFAULT_ACCENT,
    0xFFD71921.toInt(),
    0xFFF2B705.toInt(),
    0xFF4CAF50.toInt(),
    0xFFB388FF.toInt(),
    0xFFE0E0E0.toInt(),
)

private const val THEME_MIME_TYPE = "application/json"
private const val THEME_FILE_NAME = "minimalist-theme.json"
private val THEME_IMPORT_TYPES = arrayOf("application/json", "text/plain")
private val FONT_MIME_TYPES = arrayOf("font/ttf", "font/otf", "application/octet-stream")
