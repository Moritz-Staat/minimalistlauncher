package de.moritzstaat.launcher.data.icon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IconPackMatcherTest {

    private val names = listOf(
        "whatsapp",
        "google_maps",
        "spotify",
        "deutsche_bahn",
        "oeffi",
        "com_example_weird",
    )

    @Test
    fun `an exactly named drawable is used`() {
        assertEquals(
            "whatsapp",
            IconPackMatcher.findDrawable(names, "WhatsApp", "com.whatsapp"),
        )
    }

    @Test
    fun `underscores in drawable names match spaces in labels`() {
        assertEquals(
            "google_maps",
            IconPackMatcher.findDrawable(names, "Google Maps", "com.google.android.apps.maps"),
        )
    }

    @Test
    fun `a package tail is enough when the label does not match`() {
        assertEquals(
            "spotify",
            IconPackMatcher.findDrawable(names, "Musik", "com.spotify.music"),
        )
    }

    @Test
    fun `umlauts in the label still find the folded drawable name`() {
        assertEquals(
            "oeffi",
            IconPackMatcher.findDrawable(names, "Öffi", "de.schildbach.oeffi"),
        )
    }

    @Test
    fun `a loose resemblance is rejected rather than guessed`() {
        assertNull(IconPackMatcher.findDrawable(names, "Taschenrechner", "com.android.calculator2"))
    }

    @Test
    fun `an empty pack matches nothing`() {
        assertNull(IconPackMatcher.findDrawable(emptyList(), "WhatsApp", "com.whatsapp"))
    }

    @Test
    fun `an empty label falls back to the package`() {
        assertEquals("whatsapp", IconPackMatcher.findDrawable(names, "", "com.whatsapp"))
    }
}
