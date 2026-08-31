package de.moritzstaat.launcher.data.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationTextExtractorTest {

    @Test
    fun `title and text are joined`() {
        val content = NotificationContent(title = "Anna", text = "Bis gleich")
        assertEquals("Anna: Bis gleich", NotificationTextExtractor.preview(content))
    }

    @Test
    fun `a text that already repeats the title is not doubled`() {
        val content = NotificationContent(title = "Anna", text = "Anna hat geantwortet")
        assertEquals("Anna hat geantwortet", NotificationTextExtractor.preview(content))
    }

    @Test
    fun `big text fills in when there is no short text`() {
        val content = NotificationContent(title = "Newsletter", bigText = "Die lange Fassung")
        assertEquals("Newsletter: Die lange Fassung", NotificationTextExtractor.preview(content))
    }

    @Test
    fun `only a title still gives a preview`() {
        assertEquals("Akku fast leer", NotificationTextExtractor.preview(NotificationContent(title = "Akku fast leer")))
    }

    @Test
    fun `the newest message of a thread wins`() {
        val content = NotificationContent(
            title = "Gruppe",
            messages = listOf(
                MessageLine("Anna", "Erste"),
                MessageLine("Bert", "Zweite"),
            ),
        )
        assertEquals("Bert: Zweite", NotificationTextExtractor.preview(content))
    }

    @Test
    fun `a one to one chat does not repeat the name`() {
        val content = NotificationContent(
            conversationTitle = "Anna",
            messages = listOf(MessageLine("Anna", "Bis gleich")),
        )
        assertEquals("Bis gleich", NotificationTextExtractor.preview(content))
    }

    @Test
    fun `messages without a sender show just the text`() {
        val content = NotificationContent(messages = listOf(MessageLine(null, "Bis gleich")))
        assertEquals("Bis gleich", NotificationTextExtractor.preview(content))
    }

    @Test
    fun `empty trailing messages are skipped`() {
        val content = NotificationContent(
            messages = listOf(MessageLine("Anna", "Da"), MessageLine("Bert", "  ")),
        )
        assertEquals("Anna: Da", NotificationTextExtractor.preview(content))
    }

    @Test
    fun `line breaks collapse into single spaces`() {
        val content = NotificationContent(title = "Mail", text = "Zeile eins\n\nZeile zwei")
        assertEquals("Mail: Zeile eins Zeile zwei", NotificationTextExtractor.preview(content))
    }

    @Test
    fun `redaction never reads the content`() {
        val content = NotificationContent(
            title = "Anna",
            text = "Geheim",
            messages = listOf(MessageLine("Anna", "Auch geheim")),
        )
        assertEquals(
            NotificationTextExtractor.REDACTED,
            NotificationTextExtractor.preview(content, redacted = true),
        )
    }

    @Test
    fun `an empty notification produces no preview`() {
        assertNull(NotificationTextExtractor.preview(NotificationContent()))
        assertNull(NotificationTextExtractor.preview(NotificationContent(title = "   ", text = "")))
    }
}
