package de.moritzstaat.launcher.data.notification

import android.app.PendingIntent

/** One line of a MessagingStyle notification. */
data class MessageLine(
    val sender: String?,
    val text: String?,
)

/** The text carrying parts of a notification, lifted out of the platform Bundle. */
data class NotificationContent(
    val title: String? = null,
    val text: String? = null,
    val bigText: String? = null,
    val conversationTitle: String? = null,
    val messages: List<MessageLine> = emptyList(),
)

/** What the app list shows for one package. */
data class NotificationSummary(
    val packageName: String,
    val count: Int,
    val preview: String,
    val newestKey: String,
    val contentIntent: PendingIntent?,
    val keys: List<String>,
)
