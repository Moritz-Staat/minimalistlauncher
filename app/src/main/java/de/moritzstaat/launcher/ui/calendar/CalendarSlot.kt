package de.moritzstaat.launcher.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moritzstaat.launcher.data.calendar.CalendarEvent
import de.moritzstaat.launcher.data.calendar.EventFormatter
import de.moritzstaat.launcher.ui.common.rememberCurrentDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The next appointments under the clock. Collapses to nothing when the calendar is switched
 * off, unreadable or simply empty, so the home screen keeps its shape.
 */
@Composable
fun CalendarSlot(modifier: Modifier = Modifier) {
    val viewModel: CalendarViewModel = viewModel()
    val events by viewModel.events.collectAsStateWithLifecycle()

    // The labels are relative ("in 20 Min."), so they follow the minute tick of the clock.
    val now by rememberCurrentDateTime()
    val zoned = ZonedDateTime.of(now, ZoneId.systemDefault())

    // Filtered again on every minute tick, so an event drops off the moment it is over.
    val visible = events.filter { EventFormatter.isUpcoming(it, zoned) }
        .take(CalendarViewModel.MAX_EVENTS)

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    AnimatedVisibility(
        visible = visible.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            visible.forEach { event ->
                EventRow(
                    event = event,
                    now = zoned,
                    onClick = { viewModel.open(event) },
                )
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent, now: ZonedDateTime, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(DOT_SIZE)
                .background(dotColor(event), CircleShape),
        )
        Text(
            text = EventFormatter.label(event, now),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = event.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Calendars without a colour of their own get the theme accent rather than a black dot. */
@Composable
private fun dotColor(event: CalendarEvent): Color =
    if (event.colorArgb == 0) MaterialTheme.colorScheme.primary else Color(event.colorArgb)

private val DOT_SIZE = 8.dp
