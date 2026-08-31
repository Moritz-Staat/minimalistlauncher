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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import de.moritzstaat.launcher.ui.common.rememberCurrentDateTime
import de.moritzstaat.launcher.ui.common.rememberIs24Hour
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Large, left aligned clock with the date underneath. Stage 12 adds the alternative styles;
 * the layout contract (left aligned, date directly below) stays the same.
 */
@Composable
fun ClockBlock(
    onClockClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
) {
    val now by rememberCurrentDateTime()
    val is24Hour = rememberIs24Hour()

    val timeFormatter = remember(is24Hour) {
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm", Locale.GERMANY)
    }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMANY)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClockClick),
    ) {
        Text(
            text = now.format(timeFormatter),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
        )
        if (showDate) {
            Text(
                text = now.format(dateFormatter),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
    }
}
