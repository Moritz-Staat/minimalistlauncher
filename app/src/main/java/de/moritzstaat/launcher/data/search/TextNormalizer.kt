package de.moritzstaat.launcher.data.search

import java.text.Normalizer

/**
 * A label folded down to something searchable: lower case, without diacritics, with the German
 * sharp s spelled out. [sourceIndices] maps every character back to the character of the
 * original string it came from, so matches can still be highlighted in the untouched label.
 */
class NormalizedText(
    val value: String,
    private val sourceIndices: IntArray,
    private val wordStarts: BooleanArray,
) {
    val length: Int get() = value.length

    operator fun get(index: Int): Char = value[index]

    fun sourceIndex(index: Int): Int = sourceIndices[index]

    fun isWordStart(index: Int): Boolean = wordStarts[index]

    /** Indices at which a new word or camel case hump begins. */
    fun wordStartIndices(): List<Int> = wordStarts.indices.filter { wordStarts[it] }
}

object TextNormalizer {

    fun normalize(text: String): NormalizedText {
        val builder = StringBuilder(text.length)
        val sources = ArrayList<Int>(text.length)
        val starts = ArrayList<Boolean>(text.length)

        var previousOriginal: Char? = null
        for (index in text.indices) {
            val original = text[index]
            val folded = fold(original)
            if (folded.isEmpty()) {
                previousOriginal = original
                continue
            }
            val isStart = isWordStart(original, previousOriginal)
            for ((offset, char) in folded.withIndex()) {
                builder.append(char)
                sources.add(index)
                starts.add(offset == 0 && isStart)
            }
            previousOriginal = original
        }

        return NormalizedText(
            value = builder.toString(),
            sourceIndices = sources.toIntArray(),
            wordStarts = starts.toBooleanArray(),
        )
    }

    /**
     * A word starts at the beginning, after any separator, and at a camel case hump such as
     * the P in "PlayStore".
     */
    private fun isWordStart(current: Char, previous: Char?): Boolean {
        if (previous == null) return true
        if (!previous.isLetterOrDigit()) return true
        if (current.isUpperCase() && !previous.isUpperCase()) return true
        if (current.isDigit() && !previous.isDigit()) return true
        return false
    }

    /** Folds one character; may return zero characters for combining marks. */
    private fun fold(char: Char): String {
        SPECIAL_FOLDS[char]?.let { return it }
        val decomposed = Normalizer.normalize(char.toString(), Normalizer.Form.NFD)
        val builder = StringBuilder(decomposed.length)
        for (c in decomposed) {
            if (isCombiningMark(c)) continue
            builder.append(c.lowercaseChar())
        }
        return builder.toString()
    }

    private fun isCombiningMark(c: Char): Boolean = when (Character.getType(c).toByte()) {
        Character.NON_SPACING_MARK,
        Character.COMBINING_SPACING_MARK,
        Character.ENCLOSING_MARK,
        -> true

        else -> false
    }

    private val SPECIAL_FOLDS = mapOf(
        'ß' to "ss", 'ẞ' to "ss",
        'Æ' to "ae", 'æ' to "ae",
        'Œ' to "oe", 'œ' to "oe",
        'Ø' to "o", 'ø' to "o",
        'Ł' to "l", 'ł' to "l",
        'Đ' to "d", 'đ' to "d",
        'Ð' to "d", 'ð' to "d",
        'Þ' to "th", 'þ' to "th",
        'ı' to "i",
    )
}
