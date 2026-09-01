package de.moritzstaat.launcher.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherBackupTest {

    private val backup = LauncherBackup(
        preferences = mapOf(
            "dark_theme" to JsonValue.Str("false"),
            "accent_argb" to JsonValue.Str("-7686664"),
            "media_apps" to JsonValue.Arr(
                listOf(JsonValue.Str("com.example.music/com.example.music.Main#0")),
            ),
        ),
        favorites = listOf("a/a.Main#0", "b/b.Main#0"),
        hidden = listOf("c/c.Main#0"),
        labels = mapOf("a/a.Main#0" to "Telefon"),
        icons = listOf(BackupIcon("a/a.Main#0", "com.pack.icons", "phone")),
        notificationRedacted = listOf("com.example.messenger"),
        folders = listOf(BackupFolder("Arbeit", listOf("d/d.Main#0", "e/e.Main#0"))),
    )

    @Test
    fun `a backup survives encoding and decoding`() {
        assertEquals(backup, LauncherBackup.decode(backup.encode()))
    }

    @Test
    fun `the favourite order is part of the backup`() {
        val decoded = requireNotNull(LauncherBackup.decode(backup.encode()))

        assertEquals(listOf("a/a.Main#0", "b/b.Main#0"), decoded.favorites)
    }

    @Test
    fun `a set valued preference stays a list`() {
        val decoded = requireNotNull(LauncherBackup.decode(backup.encode()))

        assertEquals(
            listOf("com.example.music/com.example.music.Main#0"),
            decoded.preferences["media_apps"]?.asStringList(),
        )
    }

    @Test
    fun `an empty backup is still a valid file`() {
        val empty = LauncherBackup()

        val decoded = requireNotNull(LauncherBackup.decode(empty.encode()))

        assertEquals(empty, decoded)
    }

    @Test
    fun `missing sections restore as empty instead of failing`() {
        val decoded = requireNotNull(LauncherBackup.decode("""{"version":"1"}"""))

        assertTrue(decoded.favorites.isEmpty())
        assertTrue(decoded.folders.isEmpty())
        assertTrue(decoded.preferences.isEmpty())
    }

    @Test
    fun `a file that is not a backup is rejected`() {
        assertNull(LauncherBackup.decode(""))
        assertNull(LauncherBackup.decode("<html>"))
        assertNull(LauncherBackup.decode("""{"clockStyle":"Large"}"""))
    }

    @Test
    fun `half written entries are skipped, the rest survives`() {
        val text = """
            {
              "version": "1",
              "icons": [
                {"appKey": "a/a.Main#0", "iconPack": "com.pack", "drawable": "phone"},
                {"appKey": "b/b.Main#0"}
              ],
              "folders": [
                {"name": "Arbeit", "apps": ["d/d.Main#0"]},
                {"apps": ["x/x.Main#0"]}
              ]
            }
        """.trimIndent()

        val decoded = requireNotNull(LauncherBackup.decode(text))

        assertEquals(1, decoded.icons.size)
        assertEquals(1, decoded.folders.size)
        assertEquals("Arbeit", decoded.folders.first().name)
    }
}
