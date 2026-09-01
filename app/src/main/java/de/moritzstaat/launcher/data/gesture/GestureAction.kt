package de.moritzstaat.launcher.data.gesture

import de.moritzstaat.launcher.data.app.AppKey

/**
 * What a gesture does.
 *
 * Stored as one short string per gesture, so a new action never needs a database migration.
 * Anything unreadable decodes to [None] rather than throwing: a settings file from an older
 * build must not keep the home screen from starting.
 */
sealed interface GestureAction {

    fun encode(): String

    data object None : GestureAction {
        override fun encode(): String = "none"
    }

    data object OpenAppList : GestureAction {
        override fun encode(): String = "app_list"
    }

    data object OpenSearch : GestureAction {
        override fun encode(): String = "search"
    }

    data object OpenSettings : GestureAction {
        override fun encode(): String = "settings"
    }

    /** Pulls down the notification shade. Needs the accessibility service. */
    data object ExpandNotifications : GestureAction {
        override fun encode(): String = "notifications"
    }

    /** Turns the screen off. Needs the accessibility service. */
    data object LockScreen : GestureAction {
        override fun encode(): String = "lock"
    }

    /** Starts one app; [appKey] is a flattened [AppKey]. */
    data class LaunchApp(val appKey: String) : GestureAction {
        override fun encode(): String = PREFIX_APP + appKey

        val key: AppKey? get() = AppKey.parse(appKey)
    }

    companion object {
        const val PREFIX_APP = "app:"

        fun decode(text: String): GestureAction = when {
            text.startsWith(PREFIX_APP) -> text.removePrefix(PREFIX_APP)
                .takeIf { AppKey.parse(it) != null }
                ?.let(::LaunchApp)
                ?: None

            else -> SIMPLE.firstOrNull { it.encode() == text } ?: None
        }

        /** Everything the settings offer besides "start an app". */
        val SIMPLE: List<GestureAction> = listOf(
            None,
            OpenAppList,
            OpenSearch,
            OpenSettings,
            ExpandNotifications,
            LockScreen,
        )
    }
}

/**
 * The gestures the home screen listens for.
 *
 * Swiping up is deliberately absent: it opens the app list and is the one gesture the launcher
 * cannot give away.
 */
enum class Gesture(val storageKey: String, val label: String, val default: GestureAction) {
    DoubleTap("double_tap", "Doppeltippen", GestureAction.LockScreen),
    SwipeDown("swipe_down", "Nach unten wischen", GestureAction.ExpandNotifications),
    SwipeLeft("swipe_left", "Nach links wischen", GestureAction.None),
    SwipeRight("swipe_right", "Nach rechts wischen", GestureAction.None),
    LongPress("long_press", "Lange auf den Hintergrund druecken", GestureAction.OpenSettings),
}
