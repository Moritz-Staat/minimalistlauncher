package de.moritzstaat.launcher.data.icon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconPackParserTest {

    private fun parse(xml: String): IconPackFilter =
        IconPackParser.parse(xml.byteInputStream())

    @Test
    fun `items map components to drawables`() {
        val filter = parse(
            """
            <resources>
              <item component="ComponentInfo{com.whatsapp/com.whatsapp.Main}" drawable="whatsapp" />
              <item component="ComponentInfo{com.spotify.music/.MainActivity}" drawable="spotify" />
            </resources>
            """.trimIndent(),
        )
        assertEquals("whatsapp", filter.componentToDrawable["com.whatsapp/com.whatsapp.Main"])
        assertEquals(
            "spotify",
            filter.componentToDrawable["com.spotify.music/com.spotify.music.MainActivity"],
        )
    }

    @Test
    fun `the pack recipe is read`() {
        val filter = parse(
            """
            <resources>
              <iconback img1="back_one" img2="back_two" />
              <iconmask img1="mask" />
              <iconupon img1="upon" />
              <scale factor="0.75" />
            </resources>
            """.trimIndent(),
        )
        assertEquals(listOf("back_one", "back_two"), filter.iconBacks)
        assertEquals("mask", filter.iconMask)
        assertEquals("upon", filter.iconUpon)
        assertEquals(0.75f, filter.scale, 1e-4f)
    }

    @Test
    fun `the first entry for a component wins`() {
        val filter = parse(
            """
            <resources>
              <item component="ComponentInfo{a/b}" drawable="first" />
              <item component="ComponentInfo{a/b}" drawable="second" />
            </resources>
            """.trimIndent(),
        )
        assertEquals("first", filter.componentToDrawable["a/b"])
    }

    @Test
    fun `malformed entries are skipped instead of failing the pack`() {
        val filter = parse(
            """
            <resources>
              <item drawable="orphan" />
              <item component="ComponentInfo{broken}" drawable="broken" />
              <item component=":BACK" drawable="ignored" />
              <item component="ComponentInfo{a/b}" drawable="good" />
            </resources>
            """.trimIndent(),
        )
        assertEquals(1, filter.componentToDrawable.size)
        assertEquals("good", filter.componentToDrawable["a/b"])
    }

    @Test
    fun `a truncated file still yields what was read so far`() {
        val filter = parse(
            """
            <resources>
              <item component="ComponentInfo{a/b}" drawable="good" />
              <item component="ComponentInfo{c/
            """.trimIndent(),
        )
        assertEquals("good", filter.componentToDrawable["a/b"])
    }

    @Test
    fun `an empty pack is recognisable as empty`() {
        assertTrue(parse("<resources></resources>").isEmpty)
    }

    @Test
    fun `component parsing handles both spellings`() {
        assertEquals(
            "com.example/com.example.Main",
            IconPackFilterBuilder.parseComponent("ComponentInfo{com.example/com.example.Main}"),
        )
        assertEquals(
            "com.example/com.example.Main",
            IconPackFilterBuilder.parseComponent("com.example/com.example.Main"),
        )
        assertEquals(
            "com.example/com.example.Main",
            IconPackFilterBuilder.parseComponent("ComponentInfo{com.example/.Main}"),
        )
        assertNull(IconPackFilterBuilder.parseComponent(":MASK"))
        assertNull(IconPackFilterBuilder.parseComponent("nothing"))
        assertNull(IconPackFilterBuilder.parseComponent("a/"))
    }
}
