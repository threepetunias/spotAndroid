package com.spotapp.personal

// Input checks used at every boundary: text fields, restore files, deep links, intent extras.
// Bad input is rejected with a plain message. It is never "repaired".

class InputError(message: String) : Exception(message)

private val CONTROL = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]")
private val NEWLINES = Regex("[\\n\\r\\t]")

fun cleanText(v: Any?, label: String, min: Int, max: Int, allowNewlines: Boolean = false): String {
    if (v !is String) throw InputError("$label must be text.")
    val t = v.trim()
    if (t.length < min) throw InputError(if (min == 1) "$label cannot be empty." else "$label is too short.")
    if (t.length > max) throw InputError("$label can be at most $max characters.")
    val probe = if (allowNewlines) t.replace(NEWLINES, " ") else t
    if (CONTROL.containsMatchIn(probe) || (!allowNewlines && NEWLINES.containsMatchIn(t))) {
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
        is Double -> if (!v.isNaN() && !v.isInfinite() && v == Math.floor(v) && Math.abs(v) < 9.0e15) v.toLong() else throw InputError("$label must be a whole number.")
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

fun cleanOneOf(v: Any?, label: String, allowed: Collection<String>): String {
    if (v !is String || v !in allowed) throw InputError("$label is not a valid choice.")
    return v
}

private val DAY_SHAPE = Regex("^\\d{4}-\\d{2}-\\d{2}$")

fun cleanDay(v: Any?, label: String = "Date"): String {
    if (v !is String || !DAY_SHAPE.matches(v)) throw InputError("$label is not a valid date.")
    val y = v.substring(0, 4).toInt()
    val m = v.substring(5, 7).toInt()
    val d = v.substring(8, 10).toInt()
    val valid = try {
        val ld = java.time.LocalDate.of(y, m, d)
        ld.year == y && ld.monthValue == m && ld.dayOfMonth == d
    } catch (e: java.time.DateTimeException) {
        false
    }
    if (!valid || y < 2000 || y > 2100) throw InputError("$label is not a valid date.")
    return v
}

// Deep link and intent ids arrive as text. Returns a positive id or throws.
fun paramId(v: String?, label: String = "Item"): Long {
    if (v == null || !Regex("^\\d{1,9}$").matches(v)) throw InputError("$label link is not valid.")
    val n = v.toLong()
    if (n < 1) throw InputError("$label link is not valid.")
    return n
}

fun paramIdOrNull(v: String?): Long? = try { paramId(v) } catch (e: InputError) { null }

// Parses a number typed into a field. Returns null when it is not a usable number.
fun parseTyped(s: String): Double? {
    val t = s.trim().replace(',', '.')
    if (t.isEmpty() || t == ".") return null
    if (!Regex("^\\d{0,5}(\\.\\d{0,2})?$").matches(t)) return null
    return t.toDoubleOrNull()
}

// A web link saved on an exercise. Only http and https are accepted, so nothing else can be opened from the app.
fun cleanLink(v: Any?, label: String = "Link"): String {
    if (v == null || v == "") return ""
    val t = cleanText(v, label, 0, 300)
    if (t.isEmpty()) return ""
    val ok = Regex("^https?://[^\\s/$.?#][^\\s]*$", RegexOption.IGNORE_CASE).matches(t) &&
        Regex("^https?://[^/\\s]+\\.[^/\\s]{2,}", RegexOption.IGNORE_CASE).containsMatchIn(t)
    if (!ok) throw InputError("$label must start with http:// or https:// and be a web address.")
    return t
}

fun cleanHex(v: Any?, label: String = "Color"): String {
    if (v !is String || !Regex("^#[0-9A-Fa-f]{6}$").matches(v)) throw InputError("$label is not a valid color.")
    return v.uppercase()
}
