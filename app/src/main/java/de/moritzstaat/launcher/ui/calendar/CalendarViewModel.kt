package de.moritzstaat.launcher.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.calendar.CalendarEvent
import de.moritzstaat.launcher.data.calendar.CalendarInfo
import de.moritzstaat.launcher.data.calendar.EventFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** The next few appointments, filtered down to the calendars the user picked. */
class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as LauncherApplication).services
    private val repository = services.calendarRepository

    val events: StateFlow<List<CalendarEvent>> = combine(
        repository.events,
        services.settings.calendarEnabled,
        services.settings.calendarIds,
    ) { events, enabled, calendarIds ->
        if (!enabled) return@combine emptyList()
        val now = ZonedDateTime.now()
        events.asSequence()
            .filter { calendarIds.isEmpty() || it.calendarId.toString() in calendarIds }
            .filter { EventFormatter.isUpcoming(it, now) }
            // A few spare ones: the list is filtered again per minute tick while it is shown,
            // and an event that has just ended should be replaced, not simply dropped.
            .take(MAX_EVENTS * 2)
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _calendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
    val calendars: StateFlow<List<CalendarInfo>> = _calendars.asStateFlow()

    val selectedCalendarIds: StateFlow<Set<String>> = services.settings.calendarIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    val enabled: StateFlow<Boolean> = services.settings.calendarEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    init {
        refresh()
    }

    fun hasPermission(): Boolean = repository.hasPermission()

    /** Called whenever the launcher comes back to the front: a started event reads differently. */
    fun refresh() {
        repository.start()
        viewModelScope.launch { _calendars.value = repository.calendars() }
    }

    fun open(event: CalendarEvent) = repository.open(event)

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            services.settings.setCalendarEnabled(enabled)
            if (enabled) refresh()
        }
    }

    /**
     * An empty selection means every calendar, which is what a first run should show. Taking
     * one away therefore starts from the full list, otherwise the first tap would leave the
     * user with exactly the calendar they just switched off.
     */
    fun toggleCalendar(id: Long) {
        viewModelScope.launch {
            val all = _calendars.value.map { it.id.toString() }.toSet()
            val current = selectedCalendarIds.value.ifEmpty { all }
            val key = id.toString()
            val next = if (key in current) current - key else current + key
            services.settings.setCalendarIds(if (next == all) emptySet() else next)
        }
    }

    companion object {
        /** More than three appointments is a calendar app, not a home screen. */
        const val MAX_EVENTS = 3

        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
