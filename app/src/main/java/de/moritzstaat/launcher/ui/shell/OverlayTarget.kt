package de.moritzstaat.launcher.ui.shell

/** Everything that can be layered on top of the home screen. Only one at a time. */
enum class OverlayTarget {
    None,
    AppList,
    Search,
    Settings,
}
