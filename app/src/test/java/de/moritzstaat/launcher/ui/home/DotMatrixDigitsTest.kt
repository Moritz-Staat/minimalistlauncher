package de.moritzstaat.launcher.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DotMatrixDigitsTest {

    @Test
    fun `every digit is a complete five by seven glyph`() {
        ('0'..'9').forEach { digit ->
            val glyph = DotMatrixDigits.glyph(digit)
            assertEquals("Höhe von $digit", DotMatrixDigits.HEIGHT, glyph.size)
            glyph.forEachIndexed { row, line ->
                assertEquals("Breite von $digit, Zeile $row", DotMatrixDigits.WIDTH, line.length)
                assertTrue(
                    "unerwartetes Zeichen in $digit: $line",
                    line.all { it == '#' || it == '.' },
                )
            }
        }
    }

    @Test
    fun `every digit paints something and none fills the whole box`() {
        ('0'..'9').forEach { digit ->
            val painted = DotMatrixDigits.glyph(digit).sumOf { line -> line.count { it == '#' } }
            assertTrue("$digit ist leer", painted > 0)
            assertTrue(
                "$digit ist ein Block",
                painted < DotMatrixDigits.WIDTH * DotMatrixDigits.HEIGHT,
            )
        }
    }

    @Test
    fun `the glyphs are distinguishable from one another`() {
        val seen = ('0'..'9').map { DotMatrixDigits.glyph(it).joinToString("/") }

        assertEquals(seen.size, seen.toSet().size)
    }

    @Test
    fun `an unknown character is blank rather than an exception`() {
        val glyph = DotMatrixDigits.glyph('x')

        assertEquals(DotMatrixDigits.HEIGHT, glyph.size)
        assertTrue(glyph.all { line -> line.none { it == '#' } })
    }

    @Test
    fun `the grid is as wide as four digits plus the gaps`() {
        // 4 × 5 columns, one narrow gap inside each pair, one wide gap between the pairs.
        val expected = 4 * DotMatrixDigits.WIDTH +
            2 * DotMatrixDigits.DIGIT_GAP + DotMatrixDigits.PAIR_GAP

        assertEquals(expected, DotMatrixDigits.columnsFor("1646"))
        assertEquals(DotMatrixDigits.HEIGHT, DotMatrixDigits.grid("1646").size)
        assertEquals(expected, DotMatrixDigits.grid("1646").first().size)
    }

    @Test
    fun `the wide gap sits between the hour and the minute pair`() {
        val rows = DotMatrixDigits.grid("8888")
        // Column 11 onwards is the wide gap: 5 + 1 + 5 = 11.
        val gapStart = 2 * DotMatrixDigits.WIDTH + DotMatrixDigits.DIGIT_GAP

        for (column in gapStart until gapStart + DotMatrixDigits.PAIR_GAP) {
            assertTrue(
                "Spalte $column sollte leer sein",
                rows.all { !it[column] },
            )
        }
    }

    @Test
    fun `a leading space leaves its columns empty`() {
        val rows = DotMatrixDigits.grid(" 905")

        for (column in 0 until DotMatrixDigits.WIDTH) {
            assertTrue(rows.all { !it[column] })
        }
        // The nine right after it is painted.
        assertTrue(rows.any { it[DotMatrixDigits.WIDTH + DotMatrixDigits.DIGIT_GAP + 1] })
    }

    @Test
    fun `a grid that is not four characters is refused`() {
        listOf("", "1", "164", "16465").forEach { input ->
            val failed = runCatching { DotMatrixDigits.grid(input) }.isFailure
            assertTrue("'$input' haette scheitern muessen", failed)
        }
    }

    @Test
    fun `the twenty four hour format keeps the leading zero`() {
        assertEquals("0905", DotMatrixDigits.digitsFor(9, 5, is24Hour = true))
        assertEquals("1646", DotMatrixDigits.digitsFor(16, 46, is24Hour = true))
        assertEquals("0000", DotMatrixDigits.digitsFor(0, 0, is24Hour = true))
    }

    @Test
    fun `the twelve hour format pads with a space and has no zero hour`() {
        assertEquals(" 905", DotMatrixDigits.digitsFor(9, 5, is24Hour = false))
        assertEquals(" 446", DotMatrixDigits.digitsFor(16, 46, is24Hour = false))
        assertEquals("1200", DotMatrixDigits.digitsFor(0, 0, is24Hour = false))
        assertEquals("1230", DotMatrixDigits.digitsFor(12, 30, is24Hour = false))
    }

    @Test
    fun `midnight and noon never render as zero in the twelve hour format`() {
        (0..23).forEach { hour ->
            val digits = DotMatrixDigits.digitsFor(hour, 0, is24Hour = false)
            assertFalse("Stunde $hour ergab '$digits'", digits.startsWith(" 0"))
            assertFalse("Stunde $hour ergab '$digits'", digits.startsWith("00"))
        }
    }
}
