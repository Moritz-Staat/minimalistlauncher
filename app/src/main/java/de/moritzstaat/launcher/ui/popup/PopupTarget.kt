package de.moritzstaat.launcher.ui.popup

import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.app.AppFolder

/** What a pop-up is showing, plus where on screen it was opened from. */
sealed interface PopupTarget {

    val anchorTopPx: Int?

    data class App(
        val entry: AppEntry,
        override val anchorTopPx: Int?,
    ) : PopupTarget

    data class Folder(
        val folder: AppFolder,
        override val anchorTopPx: Int?,
    ) : PopupTarget
}
