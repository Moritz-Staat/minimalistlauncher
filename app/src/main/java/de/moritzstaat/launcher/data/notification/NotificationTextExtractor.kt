package de.moritzstaat.launcher.data.notification

/**
 * Turns a notification into the one line that fits under an app name.
 *
 * Pure on purpose: the platform Bundle is unpacked in [NotificationRepository], the rules for
 * what a preview actually says live here and are covered by unit tests.
 */
object NotificationTextExtractor {

    /** Shown instead of the content when the user asked for this app to stay private. */
    const val REDACTED = "Neue Nachricht"

    /**
     * @param redacted when true the content is never read, only the fact that something arrived.
     * @return the preview line, or null when there is nothing worth showing.
     */
    fun preview(content: NotificationContent, redacted: Boolean = false): String? {
        if (redacted) return REDACTED

        lastMessage(content)?.let { return it }

        val title = content.title.cleanOrNull()
        val body = content.text.cleanOrNull() ?: content.bigText.cleanOrNull()

        return when {
            title != null && body != null && !body.startsWith(title) -> "$title: $body"
            body != null -> body
            title != null -> title
            else -> null
        }
    }

    /** MessagingStyle notifications carry the whole thread; only the newest line is shown. */
    private fun lastMessage(content: NotificationContent): String? {
        val message = content.messages.lastOrNull { !it.text.isNullOrBlank() } ?: return null
        val text = message.text.cleanOrNull() ?: return null
        val sender = message.sender.cleanOrNull()
        val conversation = content.conversationTitle.cleanOrNull()

        // In a one-to-one chat the sender and the conversation are the same person; repeating
        // the name would waste half of the single line available.
        return when {
            sender == null -> text
            conversation != null && conversation == sender -> text
            else -> "$sender: $text"
        }
    }

    /** Collapses the whitespace a notification may carry and drops empty results. */
    private fun CharSequence?.cleanOrNull(): String? {
        if (this == null) return null
        val collapsed = toString().replace(WHITESPACE, " ").trim()
        return collapsed.ifEmpty { null }
    }

    private val WHITESPACE = Regex("\\s+")
}
