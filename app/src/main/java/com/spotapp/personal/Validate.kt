package com.spotapp.personal

// Input checks used at every boundary: text fields, restore files, route parameters.
// Bad input is rejected with a plain message. It is never "repaired".

class InputError(message: String) : Exception(message)

private val CONTROL = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]")

fun cleanText(v: Any?, label: String, min: Int, max: Int, allowNewlines: Boolean = false): String {
    if (v !is String) throw InputError("$label must be text.")
    val t = v.trim()
    if (t.length < min) throw InputError(if (min == 1) "$label cannot be empty." else "$label is too short.")
    if (t.length > max) throw InputError("$label can be at most $max characters.")
    val probe = if (allowNewlines) t.replace(Regex("[\\n\\r\\t]"), " ") else t
    if (CONTROL.containsMatchIn(probe) || (!allowNewlines && Regex("[\\n\\r\\t]").containsMatchIn(t))) {
        throw InputError("$label has characters that are not allowed.")
    }
    return t
}

fun cleanOptionalText(v: Any?, label: String, max: Int, allowNewlines: Boolean = false): String {
    if (v == null || v == "") return ""
    return cleanText(v, label, 0, max, allowNewlines)
}

fun cleanInt(v: Any?, label: String, min: Long, max: Long): Long {
    val n: Long = when (v) {
        is Int -> v.toLong()
        is Long -> v
        is Double -> if (v == Math.floor(v) && !v.isInfinite()) v.toLong() else throw InputError("$label must be a whole number.")
        else -> throw InputError("$label must be a whole number.")
    }
    if (n < min || n > max) throw InputError("$label must be between $min and $max.")
    return n
}

fun cleanNum(v: Any?, label: String, min: Double, max: Double): Double {
    val n: Double = when (v) {
        is Int -> v.toDouble()
        is Long -> v.toDouble()
        is Double -> v
        else -> throw InputError("$label must be a number.")
    }
    if (n.isNaN() || n.isInfinite()) throw InputError("$label must be a number.")
    if (n < min || n > max) throw InputError("$label must be between ${min.toLong()} and ${max.toLong()}.")
    return n
}

fun cleanOneOf(v: Any?, label: String, allowed: List<String>): String {
    if (v !is String || v !in allowed) throw InputError("$label is not a valid choice.")
    return v
}

fun cleanDay(v: Any?, label: String = "Date"): String {
    if (v !is String || !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(v)) throw InputError("$label is not a valid date.")
    val y = v.substring(0, 4).toInt()
    val m = v.substring(5, 7).toInt()
    val d = v.substring(8, 10).toInt()
    val valid = try {
        val ld = java.time.LocalDate.of(y, m, d)
        ld.year == y && ld.monthValue == m && ld.dayOfMonth == d
    } catch (e: Exception) {
        false
    }
    if (!valid || y < 2000 || y > 2100) throw InputError("$label is not a valid date.")
    return v
}

// Parses a number typed into a field. Returns null when it is not a usable number.
fun parseTyped(s: String): Double? {
    val t = s.trim().replace(',', '.')
    if (t.isEmpty() || t == ".") return null
    if (!Regex("^\\d{0,5}(\\.\\d{0,2})?$").matches(t)) return null
    return t.toDoubleOrNull()
}
