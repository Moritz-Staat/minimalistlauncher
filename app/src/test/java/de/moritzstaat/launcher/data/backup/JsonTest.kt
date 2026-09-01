package de.moritzstaat.launcher.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonTest {

    @Test
    fun `an object survives a round trip`() {
        val value = jsonOf(
            "clockStyle" to "Large".toJson(),
            "showDate" to "true".toJson(),
            "favorites" to listOf("a/b#0", "c/d#0").toJson(),
        )
        val decoded = JsonReader.read(JsonWriter.write(value))
        assertEquals(value, decoded)
    }

    @Test
    fun `nested objects survive a round trip`() {
        val value = jsonOf(
            "theme" to jsonOf("accent" to "#FF8AB4F8".toJson()),
            "apps" to JsonValue.Arr(listOf(jsonOf("key" to "a/b#0".toJson()))),
        )
        assertEquals(value, JsonReader.read(JsonWriter.write(value)))
    }

    @Test
    fun `quotes backslashes and line breaks are escaped`() {
        val value = jsonOf("label" to "a\"b\\c\nd\te".toJson())
        val text = JsonWriter.write(value)
        assertEquals(value, JsonReader.read(text))
    }

    @Test
    fun `empty containers round trip`() {
        assertEquals(
            jsonOf("a" to JsonValue.Arr(emptyList()), "b" to JsonValue.Obj(emptyMap())),
            JsonReader.read(JsonWriter.write(jsonOf(
                "a" to JsonValue.Arr(emptyList()),
                "b" to JsonValue.Obj(emptyMap()),
            ))),
        )
    }

    @Test
    fun `numbers and booleans written by other tools arrive as strings`() {
        val decoded = JsonReader.read("""{"count": 3, "on": true, "off": null}""")
        val entries = decoded?.asObject()
        assertEquals("3", entries?.get("count")?.asString())
        assertEquals("true", entries?.get("on")?.asString())
        assertEquals("null", entries?.get("off")?.asString())
    }

    @Test
    fun `unicode escapes are decoded`() {
        val decoded = JsonReader.read("""{"a": "Öffi"}""")
        assertEquals("Öffi", decoded?.asObject()?.get("a")?.asString())
    }

    @Test
    fun `malformed input returns null instead of throwing`() {
        assertNull(JsonReader.read(""))
        assertNull(JsonReader.read("{"))
        assertNull(JsonReader.read("""{"a": }"""))
        assertNull(JsonReader.read("""{"a": "b"} trailing"""))
        assertNull(JsonReader.read("""{"a" "b"}"""))
    }

    @Test
    fun `string list helper reads only strings`() {
        val value = JsonValue.Arr(listOf("a".toJson(), JsonValue.Obj(emptyMap()), "b".toJson()))
        assertEquals(listOf("a", "b"), value.asStringList())
    }

    @Test
    fun `whitespace between tokens does not matter`() {
        val decoded = JsonReader.read("  {\n \"a\" :\t[ \"x\" ,\n\"y\" ] }\n")
        assertEquals(listOf("x", "y"), decoded?.asObject()?.get("a")?.asStringList())
    }
}
