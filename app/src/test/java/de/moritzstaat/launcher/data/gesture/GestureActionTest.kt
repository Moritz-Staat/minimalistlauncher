package de.moritzstaat.launcher.data.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureActionTest {

    @Test
    fun `every simple action survives encoding`() {
        GestureAction.SIMPLE.forEach { action ->
            assertEquals(action, GestureAction.decode(action.encode()))
        }
    }

    @Test
    fun `an app action carries its key`() {
        val action = GestureAction.LaunchApp("com.example.app/com.example.app.Main#0")

        val decoded = GestureAction.decode(action.encode())

        assertEquals(action, decoded)
        assertEquals(
            "com.example.app",
            (decoded as GestureAction.LaunchApp).key?.packageName,
        )
    }

    @Test
    fun `a broken app key decodes to nothing rather than to a dead action`() {
        assertEquals(GestureAction.None, GestureAction.decode("app:nonsense"))
        assertEquals(GestureAction.None, GestureAction.decode("app:"))
    }

    @Test
    fun `unknown entries from another build decode to nothing`() {
        assertEquals(GestureAction.None, GestureAction.decode("open_the_pod_bay_doors"))
        assertEquals(GestureAction.None, GestureAction.decode(""))
    }

    @Test
    fun `the encoded names are distinct`() {
        val names = GestureAction.SIMPLE.map { it.encode() }

        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `every gesture has a distinct storage key and a default that decodes back`() {
        val keys = Gesture.entries.map { it.storageKey }
        assertEquals(keys.size, keys.toSet().size)

        Gesture.entries.forEach { gesture ->
            assertEquals(gesture.default, GestureAction.decode(gesture.default.encode()))
            assertTrue(gesture.label.isNotBlank())
        }
    }

    @Test
    fun `the defaults keep the launcher usable without any system service`() {
        // Long pressing the background is the only way into the settings, so it must not
        // default to something that needs a permission the user has not granted yet.
        assertEquals(GestureAction.OpenSettings, Gesture.LongPress.default)
        assertNotNull(Gesture.entries.first { it.default == GestureAction.None })
    }
}
