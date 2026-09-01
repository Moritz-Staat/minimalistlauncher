package de.moritzstaat.launcher.ui.shell

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns which overlay is currently layered over the home screen. Kept separate from the
 * feature view models so that both the back gesture and [de.moritzstaat.launcher.MainActivity]
 * can collapse the shell without knowing anything about the individual screens.
 */
class ShellViewModel : ViewModel() {

    private val _overlay = MutableStateFlow(OverlayTarget.None)
    val overlay: StateFlow<OverlayTarget> = _overlay.asStateFlow()

    private val _collapse = MutableStateFlow(0)

    /**
     * Counts requests to return to the resting state.
     *
     * A counter rather than a flag: two home presses in a row have to arrive as two events, and
     * the root screen holds state this view model knows nothing about - pop-ups, dialogs, the
     * open pause screen - so it has to be told rather than asked.
     */
    val collapse: StateFlow<Int> = _collapse.asStateFlow()

    /**
     * Everything shut, back to the home screen.
     *
     * Sent on the home press and when the screen turns off; [closeOverlays] alone only ever
     * dealt with the overlay this view model owns.
     */
    fun collapseAll() {
        _overlay.value = OverlayTarget.None
        _collapse.value += 1
    }

    fun open(target: OverlayTarget) {
        _overlay.value = target
    }

    /** Returns true when something was actually closed. */
    fun closeOverlays(): Boolean {
        if (_overlay.value == OverlayTarget.None) return false
        _overlay.value = OverlayTarget.None
        return true
    }
}
