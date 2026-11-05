package com.spotapp.personal

fun num(n: Double): String =
    if (n == Math.floor(n) && !n.isInfinite()) n.toLong().toString() else (Math.round(n * 100.0) / 100.0).toString()

// "60 × 10, 10, 8" when the weight repeats; "60 × 10 · 55 × 8" when it changes.
fun summarize(sets: List<SetLite>, unit: String = ""): String {
    if (sets.isEmpty()) return "No sets"
    val weights = ArrayList<Double>()
    val reps = ArrayList<MutableList<Int>>()
    for (s in sets) {
        if (weights.isNotEmpty() && weights.last() == s.weight) {
            reps.last().add(s.reps)
        } else {
            weights.add(s.weight)
            reps.add(mutableListOf(s.reps))
        }
    }
    val body = weights.indices.joinToString(" · ") { "${num(weights[it])} × ${reps[it].joinToString(", ")}" }
    return if (unit.isNotEmpty()) "$body $unit" else body
}

fun tagLabel(t: TagLink): String = if (t.severity != null) "${t.name} ${t.severity}/5" else t.name

fun hasHealthTag(s: Session): Boolean = s.tags.any { it.kind == "health" }
fun topWeight(s: Session): Double = s.sets.maxOfOrNull { it.weight } ?: 0.0
fun totalReps(s: Session): Int = s.sets.sumOf { it.reps }

data class QuickPick(val key: String, val label: String, val session: Session)

// The "where I left off" choices. Each points at one real earlier session.
fun quickPicks(sessions: List<Session>, tagId: Long?): List<QuickPick> {
    val out = ArrayList<QuickPick>()
    if (sessions.isNotEmpty()) out.add(QuickPick("last", "Last time", sessions[0]))
    val full = sessions.firstOrNull { !hasHealthTag(it) }
    if (full != null) out.add(QuickPick("full", "Last full-strength", full))
    if (tagId != null) {
        val t = sessions.firstOrNull { s -> s.tags.any { it.tagId == tagId } }
        if (t != null) out.add(QuickPick("tag", "Last with that tag", t))
    }
    var best: Session? = null
    for (s in sessions) {
        val b = best
        if (b == null || topWeight(s) > topWeight(b) || (topWeight(s) == topWeight(b) && totalReps(s) > totalReps(b))) best = s
    }
    if (best != null) out.add(QuickPick("best", "Best", best))
    return out
}
