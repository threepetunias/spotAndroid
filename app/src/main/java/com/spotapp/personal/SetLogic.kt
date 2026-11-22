package com.spotapp.personal

// What is written down for each set (or round) of an exercise, how many a new exercise starts with, and where a started exercise goes.
// Pure functions with no Android classes, so they are tested. They match the iPhone app (src/workout/fields.ts and planSets).

val BLANK_SET = SetNums(weight = 0.0, reps = 10)

// last: the rows of the last logged session (the default number of blank sets if there is none).
// default: the default number of sets, filled from the last session where there is one.
// one: a single set, starting from the first set of the last session.
fun planSets(mode: String, previous: List<SetNums>, defaultCount: Int, countsSets: Boolean): List<SetNums> {
    val first = previous.firstOrNull()
    if (mode == "one") return listOf(first ?: BLANK_SET)
    // An exercise that is not written down as sets and reps (most cardio) starts as one round.
    val want = if (countsSets) defaultCount else 1
    if (mode == "last" && previous.isNotEmpty()) return previous
    return List(want) { i -> previous.getOrNull(i) ?: previous.lastOrNull() ?: BLANK_SET }
}

// One thing that can be written down, with its limits and button step.
data class FieldDef(val key: String, val label: String, val unit: String, val step: Double, val max: Double, val integer: Boolean)

// Distance follows the weight unit: pounds go with miles, kilograms with kilometers.
fun distanceUnit(unit: String): String = if (unit == "kg") "km" else "mi"

fun fieldsFor(tracks: List<String>, otherLabel: String, unit: String, weightStep: Double): List<FieldDef> {
    val out = ArrayList<FieldDef>()
    if ("weight" in tracks) out.add(FieldDef("weight", "Weight", unit, if (weightStep > 0) weightStep else 1.0, Limits.MAX_WEIGHT, false))
    if ("reps" in tracks) out.add(FieldDef("reps", "Reps", "reps", 1.0, Limits.MAX_REPS.toDouble(), true))
    if ("time" in tracks) out.add(FieldDef("minutes", "Time", "min", 1.0, Limits.MAX_MINUTES, false))
    if ("distance" in tracks) out.add(FieldDef("distance", "Distance", distanceUnit(unit), 0.1, Limits.MAX_DISTANCE, false))
    if ("intensity" in tracks) out.add(FieldDef("intensity", "Intensity", "level", 1.0, Limits.MAX_INTENSITY, false))
    if ("incline" in tracks) out.add(FieldDef("incline", "Incline", "%", 0.5, Limits.MAX_INCLINE, false))
    if ("other" in tracks) out.add(FieldDef("other", otherLabel.ifEmpty { "Other" }, "", 1.0, Limits.MAX_OTHER, false))
    return out
}

// The word for one row: a set when reps are written down, a round otherwise.
fun rowWord(tracks: List<String>): String = if ("reps" in tracks) "set" else "round"

fun numberOf(n: SetNums, key: String): Double = when (key) {
    "weight" -> n.weight
    "reps" -> n.reps.toDouble()
    "intensity" -> n.intensity
    "incline" -> n.incline
    "minutes" -> n.minutes
    "distance" -> n.distance
    else -> n.other
}

fun withNumber(n: SetNums, key: String, v: Double): SetNums = when (key) {
    "weight" -> n.copy(weight = v)
    "reps" -> n.copy(reps = v.toInt())
    "intensity" -> n.copy(intensity = v)
    "incline" -> n.copy(incline = v)
    "minutes" -> n.copy(minutes = v)
    "distance" -> n.copy(distance = v)
    else -> n.copy(other = v)
}

private fun n2(v: Double): String = if (v == Math.floor(v) && Math.abs(v) < 1e9) v.toLong().toString() else (Math.round(v * 100) / 100.0).toString()

// "60 lb × 10" for weight and reps; otherwise the written-down things in a short line, such as "30 min · 2.5 mi · level 6".
fun setLine(s: SetNums, tracks: List<String>, otherLabel: String, unit: String): String {
    if ("weight" in tracks && "reps" in tracks && tracks.size == 2) return "${n2(s.weight)} $unit × ${s.reps}"
    val parts = ArrayList<String>()
    if ("weight" in tracks) parts.add("${n2(s.weight)} $unit")
    if ("reps" in tracks) parts.add("${s.reps} ${if (s.reps == 1) "rep" else "reps"}")
    if ("time" in tracks) parts.add("${n2(s.minutes)} min")
    if ("distance" in tracks) parts.add("${n2(s.distance)} ${distanceUnit(unit)}")
    if ("intensity" in tracks) parts.add("level ${n2(s.intensity)}")
    if ("incline" in tracks) parts.add("${n2(s.incline)}% incline")
    if ("other" in tracks) parts.add("${otherLabel.ifEmpty { "Other" }} ${n2(s.other)}")
    return parts.joinToString(" · ")
}

// Rounds to two places so + and - never drift (0.1 + 0.2), and keeps the number inside its limits.
fun bumped(value: Double, step: Double, max: Double, sign: Int): Double {
    val next = Math.round((value + sign * step) * 100) / 100.0
    return Math.min(max, Math.max(0.0, next))
}

// Where a started exercise goes: right after the last finished one. Null when it should stay where it is.
fun moveUpTarget(status: List<String>, at: Int): Int? {
    if (at < 0 || at >= status.size) return null
    var last = -1
    status.forEachIndexed { i, s -> if (i != at && s == "done") last = i }
    val target = last + 1
    // Only ever move it up the list, and only when that changes something.
    return if (target < at) target else null
}

fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> {
    if (from < 0 || from >= list.size || to < 0 || to >= list.size || from == to) return list
    val copy = list.toMutableList()
    val one = copy.removeAt(from)
    copy.add(to, one)
    return copy
}
