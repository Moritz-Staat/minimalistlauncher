package de.moritzstaat.launcher.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    if (config.clockStyle == ClockStyle.DotMatrix) {
        DotMatrixClock(
            now = now,
            is24Hour = is24Hour,
            dateText = now.format(dateFormatter).takeIf { config.showDate },
            onClick = onClockClick,
            modifier = modifier,
        )
        return
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
            ClockStyle.DotMatrix -> Unit
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

/**
 * The block clock: date above, digits centred, both filling the width.
 *
 * This style breaks the launcher's left aligned rule on purpose - centred with the date on top
 * is what makes it read as the lock screen clock it imitates, and splitting that into two more
 * settings would only invite half-combinations that look wrong.
 */
@Composable
private fun DotMatrixClock(
    now: LocalDateTime,
    is24Hour: Boolean,
    dateText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val digits = DotMatrixDigits.digitsFor(now.hour, now.minute, is24Hour)
    val grid = remember(digits) { DotMatrixDigits.grid(digits) }
    val columns = DotMatrixDigits.columnsFor(digits)
    val blockColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (dateText != null) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                // The grid is square-celled, so the box has to carry the glyph proportions.
                .aspectRatio(columns.toFloat() / DotMatrixDigits.HEIGHT),
        ) {
            drawDotMatrix(grid, columns, blockColor)
        }
    }
}

/** One filled square per set cell, with a hairline gap so the grid stays visible. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDotMatrix(
    grid: List<BooleanArray>,
    columns: Int,
    color: Color,
) {
    val cell = size.width / columns
    val block = cell * BLOCK_FILL
    val inset = (cell - block) / 2f

    grid.forEachIndexed { row, cells ->
        for (column in 0 until columns) {
            if (!cells[column]) continue
            drawRect(
                color = color,
                topLeft = Offset(column * cell + inset, row * cell + inset),
                size = Size(block, block),
            )
        }
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

/** Fraction of a cell that is painted; the rest is the gap that makes it a grid. */
private const val BLOCK_FILL = 0.82f

// The text styles all start from displayLarge, so a font the user picked reaches the clock too.
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
