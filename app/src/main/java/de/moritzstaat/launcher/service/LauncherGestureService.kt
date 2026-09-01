package de.moritzstaat.launcher.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Runs the two global actions the launcher cannot reach on its own: pulling down the
 * notification shade and turning the screen off.
 *
 * An accessibility service is the documented way to both. The alternative for locking would be
 * a device admin, which is far more invasive, and the shade cannot be opened any other way at
 * all. The service listens for nothing and reads no window content — it only exists so that
 * [performGlobalAction] has somewhere to live.
 */
class LauncherGestureService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        @Volatile
        private var instance: LauncherGestureService? = null

        /** True while the user has the service switched on and it is bound. */
        val isRunning: Boolean get() = instance != null

        fun expandNotifications(): Boolean = perform(GLOBAL_ACTION_NOTIFICATIONS)

        fun lockScreen(): Boolean = perform(GLOBAL_ACTION_LOCK_SCREEN)

        /** @return false when the service is not running, so the caller can say why. */
        private fun perform(action: Int): Boolean =
            instance?.let { runCatching { it.performGlobalAction(action) }.getOrDefault(false) }
                ?: false
    }
}
