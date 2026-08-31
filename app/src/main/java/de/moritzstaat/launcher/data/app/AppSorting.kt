package de.moritzstaat.launcher.data.app

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * Pure sorting and sectioning rules for the app list. Deliberately free of Android types so
 * the behaviour can be covered by plain JUnit tests.
 *
 * Rules:
 *  - German collation, so "Ä" sorts under "A" instead of behind "Z".
 *  - Everything that does not start with a letter lands in one trailing section "#".
 */
object AppSorting {

    const val OTHER_SECTION = "#"

    /**
     * Letters that [Normalizer] does not decompose but that German readers still expect
     * under their base letter.
     */
    private val FOLDED_LETTERS = mapOf(
        'ß' to 'S', 'ẞ' to 'S',
        'Ø' to 'O', 'ø' to 'O',
        'Æ' to 'A', 'æ' to 'A',
        'Œ' to 'O', 'œ' to 'O',
        'Ł' to 'L', 'ł' to 'L',
        'Đ' to 'D', 'đ' to 'D',
        'Ð' to 'D', 'ð' to 'D',
        'Þ' to 'T', 'þ' to 'T',
        'İ' to 'I', 'ı' to 'I',
    )

    /**
     * Secondary strength: umlauts stay distinguishable from their base letter, but case does
     * not decide the order. "Gmail" and "gmail" are then separated by the plain string
     * comparison in [comparator], which keeps the result stable.
     */
    fun germanCollator(locale: Locale = Locale.GERMANY): Collator =
        Collator.getInstance(locale).apply { strength = Collator.SECONDARY }

    /** Section header for a label: a single upper case base letter, or [OTHER_SECTION]. */
    fun sectionFor(label: String): String {
        val first = label.trimStart().firstOrNull() ?: return OTHER_SECTION
        FOLDED_LETTERS[first]?.let { return it.toString() }
        val decomposed = Normalizer.normalize(first.toString(), Normalizer.Form.NFD)
        val base = decomposed.firstOrNull { !isCombiningMark(it) } ?: return OTHER_SECTION
        FOLDED_LETTERS[base]?.let { return it.toString() }
        if (!base.isLetter()) return OTHER_SECTION
        return base.uppercaseChar().toString()
    }

    private fun isCombiningMark(c: Char): Boolean = when (Character.getType(c).toByte()) {
        Character.NON_SPACING_MARK,
        Character.COMBINING_SPACING_MARK,
        Character.ENCLOSING_MARK,
        -> true

        else -> false
    }

    /** Comparator used for every app list in the launcher. */
    fun <T> comparator(locale: Locale = Locale.GERMANY, labelOf: (T) -> String): Comparator<T> {
        val collator = germanCollator(locale)
        return Comparator { a, b ->
            val labelA = labelOf(a)
            val labelB = labelOf(b)
            val otherA = sectionFor(labelA) == OTHER_SECTION
            val otherB = sectionFor(labelB) == OTHER_SECTION
            when {
                otherA && !otherB -> 1
                !otherA && otherB -> -1
                else -> {
                    val byLabel = collator.compare(labelA, labelB)
                    if (byLabel != 0) byLabel else labelA.compareTo(labelB)
                }
            }
        }
    }

    fun <T> sorted(items: List<T>, locale: Locale = Locale.GERMANY, labelOf: (T) -> String): List<T> =
        items.sortedWith(comparator(locale, labelOf))

    /** Distinct sections of an already sorted list, in list order. */
    fun <T> sections(sortedItems: List<T>, labelOf: (T) -> String): List<String> {
        val result = ArrayList<String>()
        for (item in sortedItems) {
            val section = sectionFor(labelOf(item))
            if (result.lastOrNull() != section) result.add(section)
        }
        return result
    }
}
