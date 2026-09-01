package de.moritzstaat.launcher.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import de.moritzstaat.launcher.data.settings.ClockStyle
import de.moritzstaat.launcher.data.settings.HourFormat
import de.moritzstaat.launcher.data.settings.ThemeConfig
import de.moritzstaat.launcher.ui.common.rememberCurrentDateTime
import de.moritzstaat.launcher.ui.common.rememberIs24Hour
import de.moritzstaat.launcher.ui.theme.LocalThemeConfig
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Left aligned clock with the date underneath. Which of the four styles is drawn comes from
 * the theme; the layout contract (left aligned, date directly below) is the same for all.
 */
@Composable
fun ClockBlock(
    onClockClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: ThemeConfig = LocalThemeConfig.current,
) {
    val now by rememberCurrentDateTime()
    val systemIs24Hour = rememberIs24Hour()
    val is24Hour = when (config.hourFormat) {
        HourFormat.System -> systemIs24Hour
        HourFormat.TwelveHour -> false
        HourFormat.TwentyFourHour -> true
    }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMANY)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClockClick),
    ) {
        when (config.clockStyle) {
            ClockStyle.Large -> TimeText(now, is24Hour, largeStyle())
            ClockStyle.Narrow -> TimeText(now, is24Hour, narrowStyle())
            ClockStyle.TwoLine -> TwoLineTime(now, is24Hour)
            ClockStyle.Text -> WordTime(now)
        }
        if (config.showDate) {
            Text(
                text = now.format(dateFormatter),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun TimeText(now: LocalDateTime, is24Hour: Boolean, style: TextStyle) {
    val formatter = remember(is24Hour) {
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm", Locale.GERMANY)
    }
    Text(
        text = now.format(formatter),
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Start,
    )
}

/** Hours over minutes, both tight against each other so the pair reads as one block. */
@Composable
private fun TwoLineTime(now: LocalDateTime, is24Hour: Boolean) {
    val hourFormatter = remember(is24Hour) {
        DateTimeFormatter.ofPattern(if (is24Hour) "HH" else "h", Locale.GERMANY)
    }
    val minuteFormatter = remember { DateTimeFormatter.ofPattern("mm", Locale.GERMANY) }

    Column {
        val style = twoLineStyle()
        Text(
            text = now.format(hourFormatter),
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = now.format(minuteFormatter),
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The time spelled out the way it is said, e.g. "viertel nach drei". */
@Composable
private fun WordTime(now: LocalDateTime) {
    Text(
        text = TextClockFormatter.format(now.hour, now.minute),
        style = wordStyle(),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Start,
    )
}

// All four styles start from displayLarge, so a font the user picked reaches the clock too.
@Composable
private fun clockBase(): TextStyle = MaterialTheme.typography.displayLarge

@Composable
private fun largeStyle(): TextStyle =
    clockBase().copy(fontSize = 72.sp, fontWeight = FontWeight.Normal)

@Composable
private fun narrowStyle(): TextStyle = clockBase().copy(
    fontSize = 60.sp,
    fontWeight = FontWeight.Light,
    letterSpacing = 0.06.em,
)

@Composable
private fun twoLineStyle(): TextStyle = clockBase().copy(
    fontSize = 56.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = 1.02.em,
)

@Composable
private fun wordStyle(): TextStyle = clockBase().copy(
    fontSize = 34.sp,
    fontWeight = FontWeight.Light,
    lineHeight = 1.15.em,
)
