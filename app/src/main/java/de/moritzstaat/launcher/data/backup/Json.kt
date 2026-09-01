package de.moritzstaat.launcher.data.backup

/**
 * The subset of JSON the launcher writes and reads: objects, arrays and strings.
 *
 * Numbers and booleans are stored as strings, which keeps the model and the parser small
 * enough to be obviously correct, and the files stay readable by hand.
 */
sealed interface JsonValue {

    @JvmInline
    value class Str(val value: String) : JsonValue

    @JvmInline
    value class Arr(val values: List<JsonValue>) : JsonValue

    @JvmInline
    value class Obj(val entries: Map<String, JsonValue>) : JsonValue
}

/** Convenience accessors that return null rather than throwing on a malformed file. */
fun JsonValue.asString(): String? = (this as? JsonValue.Str)?.value

fun JsonValue.asObject(): Map<String, JsonValue>? = (this as? JsonValue.Obj)?.entries

fun JsonValue.asArray(): List<JsonValue>? = (this as? JsonValue.Arr)?.values

fun JsonValue.asStringList(): List<String> =
    asArray()?.mapNotNull { it.asString() } ?: emptyList()

fun jsonOf(vararg pairs: Pair<String, JsonValue>): JsonValue.Obj = JsonValue.Obj(pairs.toMap())

fun String.toJson(): JsonValue.Str = JsonValue.Str(this)

fun List<String>.toJson(): JsonValue.Arr = JsonValue.Arr(map { JsonValue.Str(it) })

/** Writes [JsonValue] trees. Indented, because these files end up in a text editor. */
object JsonWriter {

    fun write(value: JsonValue): String = StringBuilder().also { write(value, it, 0) }.toString()

    private fun write(value: JsonValue, out: StringBuilder, depth: Int) {
        when (value) {
            is JsonValue.Str -> out.append(quote(value.value))
            is JsonValue.Arr -> writeArray(value, out, depth)
            is JsonValue.Obj -> writeObject(value, out, depth)
        }
    }

    private fun writeArray(value: JsonValue.Arr, out: StringBuilder, depth: Int) {
        if (value.values.isEmpty()) {
            out.append("[]")
            return
        }
        out.append("[\n")
        value.values.forEachIndexed { index, element ->
            indent(out, depth + 1)
            write(element, out, depth + 1)
            if (index != value.values.lastIndex) out.append(',')
            out.append('\n')
        }
        indent(out, depth)
        out.append(']')
    }

    private fun writeObject(value: JsonValue.Obj, out: StringBuilder, depth: Int) {
        if (value.entries.isEmpty()) {
            out.append("{}")
            return
        }
        out.append("{\n")
        val entries = value.entries.entries.toList()
        entries.forEachIndexed { index, (key, element) ->
            indent(out, depth + 1)
            out.append(quote(key)).append(": ")
            write(element, out, depth + 1)
            if (index != entries.lastIndex) out.append(',')
            out.append('\n')
        }
        indent(out, depth)
        out.append('}')
    }

    private fun indent(out: StringBuilder, depth: Int) {
        repeat(depth) { out.append("  ") }
    }

    private fun quote(text: String): String {
        val out = StringBuilder(text.length + 2)
        out.append('"')
        for (char in text) {
            when (char) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> if (char < ' ') {
                    out.append("\\u").append(char.code.toString(16).padStart(4, '0'))
                } else {
                    out.append(char)
                }
            }
        }
        out.append('"')
        return out.toString()
    }
}

/** Reads what [JsonWriter] writes, and tolerates the numbers and booleans other tools add. */
object JsonReader {

    /** Returns null for anything that is not valid JSON, rather than throwing at the caller. */
    fun read(text: String): JsonValue? = runCatching { Parser(text).parseDocument() }.getOrNull()

    private class Parser(private val text: String) {
        private var index = 0

        fun parseDocument(): JsonValue {
            skipWhitespace()
            val value = parseValue()
            skipWhitespace()
            require(index >= text.length) { "trailing content at $index" }
            return value
        }

        private fun parseValue(): JsonValue {
            skipWhitespace()
            return when (peek()) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonValue.Str(parseString())
                else -> JsonValue.Str(parseLiteral())
            }
        }

        private fun parseObject(): JsonValue {
            expect('{')
            val entries = LinkedHashMap<String, JsonValue>()
            skipWhitespace()
            if (peek() == '}') {
                index++
                return JsonValue.Obj(entries)
            }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                entries[key] = parseValue()
                skipWhitespace()
                when (val next = next()) {
                    ',' -> Unit
                    '}' -> return JsonValue.Obj(entries)
                    else -> throw IllegalArgumentException("unexpected $next")
                }
            }
        }

        private fun parseArray(): JsonValue {
            expect('[')
            val values = ArrayList<JsonValue>()
            skipWhitespace()
            if (peek() == ']') {
                index++
                return JsonValue.Arr(values)
            }
            while (true) {
                values += parseValue()
                skipWhitespace()
                when (val next = next()) {
                    ',' -> Unit
                    ']' -> return JsonValue.Arr(values)
                    else -> throw IllegalArgumentException("unexpected $next")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val out = StringBuilder()
            while (true) {
                when (val char = next()) {
                    '"' -> return out.toString()
                    '\\' -> out.append(parseEscape())
                    else -> out.append(char)
                }
            }
        }

        private fun parseEscape(): Char = when (val marker = next()) {
            '"' -> '"'
            '\\' -> '\\'
            '/' -> '/'
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> {
                require(index + 4 <= text.length) { "truncated escape" }
                val code = text.substring(index, index + 4).toInt(16)
                index += 4
                code.toChar()
            }

            else -> throw IllegalArgumentException("unknown escape $marker")
        }

        /** Numbers, true, false and null, all kept as their literal text. */
        private fun parseLiteral(): String {
            val start = index
            while (index < text.length && text[index] !in LITERAL_TERMINATORS) index++
            val literal = text.substring(start, index).trim()
            require(literal.isNotEmpty()) { "empty value at $start" }
            return literal
        }

        private fun skipWhitespace() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        private fun peek(): Char {
            require(index < text.length) { "unexpected end" }
            return text[index]
        }

        private fun next(): Char {
            require(index < text.length) { "unexpected end" }
            return text[index++]
        }

        private fun expect(char: Char) {
            require(next() == char) { "expected $char" }
        }
    }

    private val LITERAL_TERMINATORS = charArrayOf(',', '}', ']', ' ', '\n', '\r', '\t')
}
