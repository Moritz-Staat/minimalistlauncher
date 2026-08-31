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
