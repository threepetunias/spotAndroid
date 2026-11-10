package com.spotapp.personal

// Past sessions ("where I left off"), records, and the Log search.

fun Store.pastSessions(exerciseId: Long, excludeWeId: Long): List<Session> {
    val rows = q.map(
        """SELECT e.id, w.id, w.title, w.day, COALESCE(e.location_id, w.location_id), l.name
           FROM workout_exercises e JOIN workouts w ON w.id = e.workout_id
           LEFT JOIN locations l ON l.id = COALESCE(e.location_id, w.location_id)
           WHERE e.exercise_id = ? AND e.id <> ?
             AND EXISTS (SELECT 1 FROM sets s WHERE s.we_id = e.id AND s.done = 1)
           ORDER BY w.day DESC, e.id DESC LIMIT 60""",
        exerciseId, excludeWeId,
    ) { listOf(it.long(0), it.long(1), it.str(2), it.str(3), it.longOrNull(4), it.str(5)) }
    val paths = tagPaths()
    return rows.map { r ->
        val weId = r[0] as Long
        val wid = r[1] as Long
        val sets = q.map("SELECT weight, reps FROM sets WHERE we_id = ? AND done = 1 ORDER BY position, id", weId) { SetLite(it.dbl(0), it.getInt(1)) }
        val tags = getLinks(TagTarget.OfWorkout(wid), paths) + getLinks(TagTarget.OfItem(weId), paths)
        Session(weId, wid, r[2] as String, r[3] as String, sets, tags, r[4] as Long?, r[5] as String)
    }
}

// Replaces the not-yet-logged sets of an exercise with a copy of an earlier session.
fun Store.loadSession(weId: Long, fromWeId: Long) {
    val to = cleanInt(weId, "Item", 1, 1_000_000_000)
    val from = cleanInt(fromWeId, "Item", 1, 1_000_000_000)
    q.tx {
        val src = q.map("SELECT weight, reps FROM sets WHERE we_id = ? AND done = 1 ORDER BY position, id", from) { it.dbl(0) to it.getInt(1) }
        if (src.isEmpty()) throw InputError("That session has no logged sets.")
        q.exec("DELETE FROM sets WHERE we_id = ? AND done = 0", to)
        var p = (q.first("SELECT MAX(position) FROM sets WHERE we_id = ?", to) { if (it.isNull(0)) -1 else it.getInt(0) } ?: -1) + 1
        for ((w, r) in src) q.exec("INSERT INTO sets (we_id, position, weight, reps, done) VALUES (?, ?, ?, ?, 0)", to, p++, w, r)
    }
}

// ---------- PRs (heaviest weight per exercise) ----------

private class PrSet(
    val weight: Double, val reps: Int, val day: String, val exerciseId: Long, val name: String, val icon: String,
    val locId: Long?, val loc: String,
)

private fun Store.prSets(locationId: Long?): List<PrSet> {
    val where = if (locationId == null) "" else " AND COALESCE(e.location_id, w.location_id) = ?"
    val sql = """SELECT s.weight, s.reps, w.day, e.exercise_id, x.name, x.icon, COALESCE(e.location_id, w.location_id), l.name
         FROM sets s JOIN workout_exercises e ON e.id = s.we_id JOIN workouts w ON w.id = e.workout_id
         JOIN exercises x ON x.id = e.exercise_id
         LEFT JOIN locations l ON l.id = COALESCE(e.location_id, w.location_id)
         WHERE s.done = 1 AND s.weight > 0$where
         ORDER BY w.day, e.id, s.position, s.id"""
    val map = { c: android.database.Cursor -> PrSet(c.dbl(0), c.getInt(1), c.str(2), c.long(3), c.str(4), c.str(5), c.longOrNull(6), c.str(7)) }
    return if (locationId == null) q.map(sql, f = map) else q.map(sql, cleanInt(locationId, "Location", 1, 1_000_000_000), f = map)
}

private fun toPr(r: PrSet, unit: String) = PrRow(r.exerciseId, r.name, r.icon, r.weight, r.reps, r.day, r.locId, r.loc, unit)

class Prs(val best: List<PrRow>, val latest: List<PrRow>)

// best: heaviest set per exercise (ties: more reps, then the later one).
// latest: for each exercise, the set that last beat everything before it. Newest first.
fun Store.listPrs(locationId: Long? = null): Prs {
    val unit = if (getSettings()["unit"] == "kg") "kg" else "lb"
    val best = LinkedHashMap<Long, PrSet>()
    val last = LinkedHashMap<Long, PrSet>()
    for (r in prSets(locationId)) {
        val b = best[r.exerciseId]
        if (b == null || r.weight > b.weight) last[r.exerciseId] = r
        if (b == null || r.weight > b.weight || (r.weight == b.weight && r.reps >= b.reps)) best[r.exerciseId] = r
    }
    return Prs(
        best.values.map { toPr(it, unit) }.sortedBy { it.name.lowercase() },
        last.values.map { toPr(it, unit) }.sortedWith(compareByDescending<PrRow> { it.day }.thenBy { it.name.lowercase() }),
    )
}

// ---------- Log: search and filter workouts ----------

private fun likeEscape(t: String): String = t.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

// The Log never hides workouts below this number silently: the screen says so when it is reached.
const val LOG_LIMIT = 5000

fun Store.searchWorkouts(f: LogFilter): List<WorkoutRow> {
    val where = ArrayList<String>()
    val args = ArrayList<Any?>()
    f.from?.let { where.add("w.day >= ?"); args.add(cleanDay(it)) }
    f.to?.let { where.add("w.day <= ?"); args.add(cleanDay(it)) }
    f.locationId?.let { where.add("w.location_id = ?"); args.add(cleanInt(it, "Location", 1, 1_000_000_000)) }
    f.typeId?.let { where.add("w.type_id = ?"); args.add(cleanInt(it, "Workout type", 1, 1_000_000_000)) }
    if (f.tagIds.isNotEmpty()) {
        val ids = f.tagIds.take(200).map { cleanInt(it, "Tag", 1, 1_000_000_000) }
        val ph = ids.joinToString(",") { "?" }
        where.add(
            """(EXISTS (SELECT 1 FROM tag_links tl WHERE tl.workout_id = w.id AND tl.tag_id IN ($ph))
               OR EXISTS (SELECT 1 FROM tag_links tl JOIN workout_exercises e ON e.id = tl.we_id WHERE e.workout_id = w.id AND tl.tag_id IN ($ph)))""",
        )
        args.addAll(ids); args.addAll(ids)
    }
    val text = f.text.trim().take(60)
    if (text.isNotEmpty()) {
        val like = "%${likeEscape(text)}%"
        where.add(
            """(w.title LIKE ? ESCAPE '\' OR w.note LIKE ? ESCAPE '\' OR l.name LIKE ? ESCAPE '\' OR t.name LIKE ? ESCAPE '\'
               OR EXISTS (SELECT 1 FROM workout_exercises e JOIN exercises x ON x.id = e.exercise_id WHERE e.workout_id = w.id AND x.name LIKE ? ESCAPE '\'))""",
        )
        repeat(5) { args.add(like) }
    }
    val clause = if (where.isEmpty()) "" else "WHERE " + where.joinToString(" AND ")
    // The placeholders above are only "?" marks; every value is bound. LOG_LIMIT is a fixed number.
    return q.map(
        """SELECT w.id, w.title, w.day, w.done,
            (SELECT COUNT(*) FROM workout_exercises e WHERE e.workout_id = w.id),
            (SELECT COUNT(*) FROM workout_exercises e WHERE e.workout_id = w.id AND e.status = 'done'),
            l.name, t.name, t.icon, l.icon, l.color
           FROM workouts w LEFT JOIN locations l ON l.id = w.location_id LEFT JOIN workout_types t ON t.id = w.type_id
           $clause ORDER BY w.day DESC, w.id DESC LIMIT $LOG_LIMIT""",
        *args.toTypedArray(),
    ) { mapWorkoutRow(it) }
}
