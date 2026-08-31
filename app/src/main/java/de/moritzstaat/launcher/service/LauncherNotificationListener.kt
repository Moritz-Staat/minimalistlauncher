package de.moritzstaat.launcher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.notification.NotificationRepository

/**
 * Feeds the notification previews of the app list and the media widget of stage 8.
 *
 * The service is optional. Without the access the system never binds it, nothing is published
 * and the launcher works exactly as before, only without previews.
 */
class LauncherNotificationListener : NotificationListenerService() {

    private val repository: NotificationRepository?
        get() = (applicationContext as? LauncherApplication)?.services?.notificationRepository

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        repository?.setConnected(true)
        publish()
    }

    override fun onListenerDisconnected() {
        repository?.setConnected(false)
        if (instance === this) instance = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        publish()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        publish()
    }

    private fun publish() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        repository?.submit(active.toList())
    }

    companion object {
        /**
         * The bound service instance, or null while the access is not granted. Only used to
         * cancel a notification the user swiped away; everything else flows through the
         * repository.
         */
        @Volatile
        private var instance: LauncherNotificationListener? = null

        fun dismiss(key: String) {
            runCatching { instance?.cancelNotification(key) }
        }

        fun dismissAll(keys: Collection<String>) {
            val service = instance ?: return
            runCatching { service.cancelNotifications(keys.toTypedArray()) }
        }
    }
}
