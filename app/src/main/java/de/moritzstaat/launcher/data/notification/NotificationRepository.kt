package de.moritzstaat.launcher.data.notification

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The notifications the launcher currently shows, grouped by package.
 *
 * Fed by [de.moritzstaat.launcher.service.LauncherNotificationListener]. When the listener has
 * no permission nothing ever arrives here and every consumer simply sees an empty map, which
 * is why the launcher stays fully usable without the notification access.
 */
class NotificationRepository {

    private val _summaries = MutableStateFlow<Map<String, NotificationSummary>>(emptyMap())
    val summaries: StateFlow<Map<String, NotificationSummary>> = _summaries.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /** Packages the user asked to show without content. */
    @Volatile
    private var redactedPackages: Set<String> = emptySet()

    private var lastRaw: List<StatusBarNotification> = emptyList()

    fun setConnected(connected: Boolean) {
        _isConnected.value = connected
        if (!connected) {
            lastRaw = emptyList()
            _summaries.value = emptyMap()
        }
    }

    fun setRedactedPackages(packages: Set<String>) {
        redactedPackages = packages
        rebuild()
    }

    /** Replaces everything with the listener's current view of the world. */
    fun submit(notifications: List<StatusBarNotification>) {
        lastRaw = notifications.filter { it.isShowable() }
        rebuild()
    }

    private fun rebuild() {
        _summaries.value = lastRaw
            .groupBy { it.packageName }
            .mapValues { (packageName, group) ->
                // Newest first, so the preview is the one that just arrived.
                val sorted = group.sortedByDescending { it.postTime }
                val newest = sorted.first()
                val preview = NotificationTextExtractor.preview(
                    content = newest.notification.extras.toContent(),
                    redacted = packageName in redactedPackages,
                ).orEmpty()
                NotificationSummary(
                    packageName = packageName,
                    count = sorted.size,
                    preview = preview,
                    newestKey = newest.key,
                    contentIntent = newest.notification.contentIntent,
                    keys = sorted.map { it.key },
                )
            }
            .filterValues { it.preview.isNotEmpty() }
    }
}

/**
 * Ongoing events are progress bars, playing media and running downloads. They never change and
 * would sit in the list forever, so they are dropped.
 */
internal fun StatusBarNotification.isShowable(): Boolean {
    val flags = notification.flags
    if (flags and Notification.FLAG_ONGOING_EVENT != 0) return false
    if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
    return true
}

/** Lifts the text carrying extras out of the platform Bundle. */
internal fun Bundle.toContent(): NotificationContent = NotificationContent(
    title = getCharSequence(Notification.EXTRA_TITLE)?.toString(),
    text = getCharSequence(Notification.EXTRA_TEXT)?.toString(),
    bigText = getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
    conversationTitle = getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString(),
    messages = readMessages(),
)

private fun Bundle.readMessages(): List<MessageLine> {
    val raw = getParcelableArray(Notification.EXTRA_MESSAGES, Bundle::class.java) ?: return emptyList()
    return raw.mapNotNull { bundle ->
        val text = bundle.getCharSequence(KEY_MESSAGE_TEXT)?.toString()
        val sender = bundle.getCharSequence(KEY_MESSAGE_SENDER)?.toString()
        if (text == null && sender == null) null else MessageLine(sender, text)
    }
}

/** Keys used by Notification.MessagingStyle.Message; they have no public constants. */
private const val KEY_MESSAGE_TEXT = "text"
private const val KEY_MESSAGE_SENDER = "sender"
