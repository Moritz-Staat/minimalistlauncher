package de.moritzstaat.launcher.data.app

/** One row in the app list, already carrying the label the user actually sees. */
data class AppEntry(
    val key: AppKey,
    val label: String,
    val systemLabel: String,
    val isWorkProfile: Boolean,
) {
    /** Section header of the alphabet bar this entry belongs to. */
    val section: String get() = AppSorting.sectionFor(label)
}
