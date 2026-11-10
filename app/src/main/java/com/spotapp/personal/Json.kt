package com.spotapp.personal

// A small JSON reader and writer, so backup files can be checked without Android libraries.
// Objects become Map<String, Any?>, arrays List<Any?>, numbers Long (whole) or Double, plus String, Boolean and null.
// The reader is strict: anything left over after the value, or nested deeper than MAX_DEPTH, is rejected.

class JsonError(message: String) : Exception(message)

object Json {
    private const val MAX_DEPTH = 40

    fun parse(text: String): Any? {
        val p = Parser(text)
        p.skip()
        val v = p.value(0)
        p.skip()
        if (p.i != text.length) throw JsonError("Extra text after the end.")
        return v
    }

    private class Parser(val s: String) {
        var i = 0

        fun skip() {
            while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r' || s[i] == '\t')) i++
        }

        fun value(depth: Int): Any? {
            if (depth > MAX_DEPTH) throw JsonError("Too deeply nested.")
            if (i >= s.length) throw JsonError("Unexpected end.")
            return when (val c = s[i]) {
                '{' -> obj(depth)
                '[' -> arr(depth)
                '"' -> str()
                't' -> lit("true", true)
                'f' -> lit("false", false)
                'n' -> lit("null", null)
                else -> if (c == '-' || c in '0'..'9') num() else throw JsonError("Unexpected character.")
            }
        }

        fun lit(word: String, v: Any?): Any? {
            if (!s.startsWith(word, i)) throw JsonError("Unexpected text.")
            i += word.length
            return v
        }

        fun obj(depth: Int): Map<String, Any?> {
            i++
            val out = LinkedHashMap<String, Any?>()
            skip()
            if (i < s.length && s[i] == '}') { i++; return out }
            while (true) {
                skip()
                if (i >= s.length || s[i] != '"') throw JsonError("Expected a name.")
                val k = str()
                skip()
                if (i >= s.length || s[i] != ':') throw JsonError("Expected a colon.")
                i++
                skip()
                if (out.containsKey(k)) throw JsonError("Repeated name.")
                out[k] = value(depth + 1)
                skip()
                if (i >= s.length) throw JsonError("Unexpected end.")
                if (s[i] == ',') { i++; continue }
                if (s[i] == '}') { i++; return out }
                throw JsonError("Expected a comma or end of object.")
            }
        }

        fun arr(depth: Int): List<Any?> {
            i++
            val out = ArrayList<Any?>()
            skip()
            if (i < s.length && s[i] == ']') { i++; return out }
            while (true) {
                skip()
                out.add(value(depth + 1))
                skip()
                if (i >= s.length) throw JsonError("Unexpected end.")
                if (s[i] == ',') { i++; continue }
                if (s[i] == ']') { i++; return out }
                throw JsonError("Expected a comma or end of list.")
            }
        }

        fun str(): String {
            i++
            val sb = StringBuilder()
            while (true) {
                if (i >= s.length) throw JsonError("Text is not closed.")
                val c = s[i++]
                when {
                    c == '"' -> return sb.toString()
                    c == '\\' -> {
                        if (i >= s.length) throw JsonError("Text is not closed.")
                        when (val e = s[i++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) throw JsonError("Bad escape.")
                                val hex = s.substring(i, i + 4)
                                if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) throw JsonError("Bad escape.")
                                sb.append(hex.toInt(16).toChar())
                                i += 4
                            }
                            else -> throw JsonError("Bad escape '$e'.")
                        }
                    }
                    c < ' ' -> throw JsonError("Control character in text.")
                    else -> sb.append(c)
                }
            }
        }

        fun num(): Any {
            val start = i
            if (s[i] == '-') i++
            if (i >= s.length) throw JsonError("Bad number.")
            if (s[i] == '0') i++ else if (s[i] in '1'..'9') { while (i < s.length && s[i] in '0'..'9') i++ } else throw JsonError("Bad number.")
            var whole = true
            if (i < s.length && s[i] == '.') {
                whole = false
                i++
                val d = i
                while (i < s.length && s[i] in '0'..'9') i++
                if (i == d) throw JsonError("Bad number.")
            }
            if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
                whole = false
                i++
                if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
                val d = i
                while (i < s.length && s[i] in '0'..'9') i++
                if (i == d) throw JsonError("Bad number.")
            }
            val t = s.substring(start, i)
            if (whole) {
                val l = t.toLongOrNull()
                if (l != null) return l
            }
            val d = t.toDoubleOrNull() ?: throw JsonError("Bad number.")
            if (d.isNaN() || d.isInfinite()) throw JsonError("Bad number.")
            return d
        }
    }

    fun write(v: Any?): String {
        val sb = StringBuilder()
        put(sb, v)
        return sb.toString()
    }

    private fun put(sb: StringBuilder, v: Any?) {
        when (v) {
            null -> sb.append("null")
            is String -> quote(sb, v)
            is Boolean -> sb.append(if (v) "true" else "false")
            is Int, is Long -> sb.append(v.toString())
            is Double -> {
                if (v.isNaN() || v.isInfinite()) throw JsonError("Number cannot be saved.")
                if (v == Math.floor(v) && Math.abs(v) < 1e15) sb.append(v.toLong().toString()) else sb.append(v.toString())
            }
            is Map<*, *> -> {
                sb.append('{')
                var first = true
                for ((k, x) in v) {
                    if (!first) sb.append(',')
                    first = false
                    quote(sb, k as String)
                    sb.append(':')
                    put(sb, x)
                }
                sb.append('}')
            }
            is List<*> -> {
                sb.append('[')
                for ((n, x) in v.withIndex()) {
                    if (n > 0) sb.append(',')
                    put(sb, x)
                }
                sb.append(']')
            }
            else -> throw JsonError("Cannot save this value.")
        }
    }

    private fun quote(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c < ' ' -> sb.append("\\u").append(String.format("%04x", c.code))
                else -> sb.append(c)
            }
        }
        sb.append('"')
    }
}
