package de.moritzstaat.launcher.ui.home

/**
 * A 5×7 block font for the ten digits, drawn rather than typeset.
 *
 * Nothing's own clock uses their Ndot typeface. Shipping that file would be a licensing
 * question and would leave every other phone without it, so the grid is defined here and
 * painted as squares. The patterns are the whole glyph set, which makes them testable and
 * keeps the clock working on any device.
 *
 * There is no colon on purpose: the lock screen this imitates separates hours and minutes by a
 * wider gap, and a colon in a block font reads as two stray dots.
 */
object DotMatrixDigits {

    const val WIDTH = 5
    const val HEIGHT = 7

    /** Blank columns between the hour pair and the minute pair. */
    const val PAIR_GAP = 3

    /** Blank columns between two digits of the same pair. */
    const val DIGIT_GAP = 1

    private val GLYPHS: Map<Char, List<String>> = mapOf(
        '0' to listOf(".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
        '1' to listOf("..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."),
        '2' to listOf(".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"),
        '3' to listOf("####.", "....#", "....#", ".###.", "....#", "....#", "####."),
        '4' to listOf("...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."),
        '5' to listOf("#####", "#....", "####.", "....#", "....#", "#...#", ".###."),
        '6' to listOf("..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###."),
        '7' to listOf("#####", "....#", "...#.", "..#..", "..#..", "..#..", "..#.."),
        '8' to listOf(".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."),
        '9' to listOf(".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##.."),
    )

    /** True where a block is painted. Indexed [row][column]. */
    fun glyph(digit: Char): List<String> = GLYPHS[digit] ?: BLANK

    /**
     * Lays out a four digit time as one grid.
     *
     * @param digits exactly four characters, e.g. "1646". A leading space is allowed for the
     *   12 hour format, where "9:05" has no tens digit and must not be padded with a zero.
     * @return rows of booleans, [HEIGHT] of them, each [columnsFor] long.
     */
    fun grid(digits: String): List<BooleanArray> {
        require(digits.length == 4) { "expected four digits, got '$digits'" }
        val columns = columnsFor(digits)
        val rows = List(HEIGHT) { BooleanArray(columns) }

        var offset = 0
        digits.forEachIndexed { index, digit ->
            if (digit != ' ') {
                val glyph = glyph(digit)
                for (row in 0 until HEIGHT) {
                    for (column in 0 until WIDTH) {
                        if (glyph[row][column] == '#') rows[row][offset + column] = true
                    }
                }
            }
            offset += WIDTH + gapAfter(index)
        }
        return rows
    }

    fun columnsFor(digits: String): Int {
        var columns = digits.length * WIDTH
        for (index in 0 until digits.length - 1) columns += gapAfter(index)
        return columns
    }

    /** The wide gap sits where the colon would be. */
    private fun gapAfter(index: Int): Int = when (index) {
        1 -> PAIR_GAP
        3 -> 0
        else -> DIGIT_GAP
    }

    /**
     * The four characters for one time.
     *
     * In the 12 hour format the leading zero becomes a space: "9:05" is nine, not "09".
     */
    fun digitsFor(hour: Int, minute: Int, is24Hour: Boolean): String {
        val shown = when {
            is24Hour -> hour
            hour % 12 == 0 -> 12
            else -> hour % 12
        }
        val hours = if (is24Hour) {
            shown.toString().padStart(2, '0')
        } else {
            shown.toString().padStart(2, ' ')
        }
        return hours + minute.toString().padStart(2, '0')
    }

    private val BLANK = List(HEIGHT) { ".".repeat(WIDTH) }
}
