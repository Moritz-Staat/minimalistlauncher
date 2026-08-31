package de.moritzstaat.launcher.data.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppKeyTest {

    @Test
    fun `flatten and parse round trip`() {
        val key = AppKey("com.example.app", "com.example.app.Main", 0L)
        assertEquals(key, AppKey.parse(key.flatten()))
    }

    @Test
    fun `work profile serial survives the round trip`() {
        val key = AppKey("com.example.app", "com.example.app.Main", 11L)
        assertEquals(key, AppKey.parse(key.flatten()))
    }

    @Test
    fun `inner class names with dollar signs survive`() {
        val key = AppKey("com.example.app", "com.example.app.Main\$Alias", 0L)
        assertEquals(key, AppKey.parse(key.flatten()))
    }

    @Test
    fun `malformed input returns null`() {
        assertNull(AppKey.parse(""))
        assertNull(AppKey.parse("nothing"))
        assertNull(AppKey.parse("com.example/Main"))
        assertNull(AppKey.parse("com.example/Main#nope"))
        assertNull(AppKey.parse("#5"))
    }
}
