package com.spotapp.personal

// Workouts, the exercises inside them, and their sets.

private const val SELECT_ROW = """SELECT w.id, w.title, w.day, w.done,
        (SELECT COUNT(*) FROM workout_exercises e WHERE e.workout_id = w.id) AS ex_count,
        (SELECT COUNT(*) FROM workout_exercises e WHERE e.workout_id = w.id AND e.status = 'done') AS ex_done,
        l.name AS loc, t.name AS type_name, t.icon AS icon, l.icon AS place_icon, l.color AS place_color
     FROM workouts w
     LEFT JOIN locations l ON l.id = w.location_id
     LEFT JOIN workout_types t ON t.id = w.type_id"""

fun mapWorkoutRow(c: android.database.Cursor) = WorkoutRow(
    id = c.long(0), title = c.str(1), day = c.str(2), done = c.getInt(3) == 1, exCount = c.getInt(4), exDone = c.getInt(5),
    locationName = c.str(6), typeName = c.str(7), icon = c.str(8), placeIcon = c.str(9), placeColor = c.str(10),
)

fun Store.listWorkouts(): List<WorkoutRow> =
    q.map("$SELECT_ROW ORDER BY w.day DESC, w.id DESC LIMIT 400") { mapWorkoutRow(it) }

// The title is made, not typed: "Thu 5 Nov · Gym A", with " · 2" when there is more than one that day and place.
private fun Store.makeTitle(day: String, locationId: Long?, selfId: Long?): String {
    var base = titleDay(day)
    if (locationId != null) {
        val name = q.first("SELECT name FROM locations WHERE id = ?", locationId) { it.str(0) }
        if (name != null) base += " · $name"
    }
    val n = q.count("SELECT COUNT(*) FROM workouts WHERE day = ? AND location_id IS ? AND id IS NOT ?", day, locationId, selfId)
    val count = n + 1
    return if (count > 1) "$base · $count" else base
}

private const val SET_NUMS = "weight, reps, intensity, incline, minutes, distance, other"

private fun numsOf(c: android.database.Cursor, from: Int = 0) = SetNums(
    weight = c.dbl(from), reps = c.getInt(from + 1), intensity = c.dbl(from + 2), incline = c.dbl(from + 3),
    minutes = c.dbl(from + 4), distance = c.dbl(from + 5), other = c.dbl(from + 6),
)

private fun Store.insertSet(weId: Long, position: Int, n: SetNums) {
    q.exec(
        "INSERT INTO sets (we_id, position, weight, reps, intensity, incline, minutes, distance, other, done) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
        weId, position, n.weight, n.reps, n.intensity, n.incline, n.minutes, n.distance, n.other,
    )
}

// The sets a new exercise starts with: its own Starting sets choice, else the one in Settings (see planSets).
private fun Store.seedSets(weId: Long, exerciseId: Long) {
    val ex = q.first("SELECT equipment, tracks, start_mode FROM exercises WHERE id = ?", exerciseId) { Triple(it.str(0), it.str(1), it.str(2)) }
    val settings = getSettings()
    val own = ex?.third?.takeIf { it == "last" || it == "default" || it == "one" }
    val global = settings["startSets"]?.takeIf { it == "default" || it == "one" } ?: "last"
    val mode = own ?: global
    val defaultCount = (settings["defaultSets"]?.toIntOrNull() ?: 4).coerceIn(1, Limits.MAX_SETS)
    val countsSets = ex == null || "reps" in exerciseTracks(ex.first, ex.second)
    // Start from the most recent logged session of this exercise.
    val last = q.first(
        """SELECT e.id FROM workout_exercises e JOIN workouts w ON w.id = e.workout_id
           WHERE e.exercise_id = ? AND e.id <> ? AND EXISTS (SELECT 1 FROM sets s WHERE s.we_id = e.id AND s.done = 1)
           ORDER BY w.day DESC, e.id DESC LIMIT 1""",
        exerciseId, weId,
    ) { it.long(0) }
    val previous = if (last != null) q.map("SELECT $SET_NUMS FROM sets WHERE we_id = ? AND done = 1 ORDER BY position, id", last) { numsOf(it) } else emptyList()
    var p = 0
    for (n in planSets(mode, previous, defaultCount, countsSets)) insertSet(weId, p++, n)
}

private fun Store.addExercisesTx(workoutId: Long, exerciseIds: List<Long>) {
    val max = q.first("SELECT MAX(position) FROM workout_exercises WHERE workout_id = ?", workoutId) { if (it.isNull(0)) -1 else it.getInt(0) } ?: -1
    var pos = max + 1
    for (exId in exerciseIds) {
        if (q.first("SELECT id FROM exercises WHERE id = ?", exId) { 1 } == null) throw InputError("That exercise no longer exists.")
        val weId = q.insert("INSERT INTO workout_exercises (workout_id, exercise_id, position, status) VALUES (?, ?, ?, 'todo')", workoutId, exId, pos++)
        seedSets(weId, exId)
    }
}

fun Store.createWorkout(day: String, locationId: Long?, typeId: Long?, exerciseIds: List<Long>): Long {
    val d = cleanDay(day)
    val loc = locationId?.let { cleanInt(it, "Location", 1, 1_000_000_000) }
    val typ = typeId?.let { cleanInt(it, "Workout type", 1, 1_000_000_000) }
    val ids = exerciseIds.map { cleanInt(it, "Exercise", 1, 1_000_000_000) }
    if (ids.size > 40) throw InputError("At most 40 exercises in one workout.")
    return q.tx {
        requireRow("locations", loc, "That location")
        requireRow("workout_types", typ, "That workout type")
        val title = makeTitle(d, loc, null)
        val id = q.insert("INSERT INTO workouts (title, day, location_id, type_id) VALUES (?, ?, ?, ?)", title, d, loc, typ)
        addExercisesTx(id, ids)
        id
    }
}

fun Store.addExercisesToWorkout(workoutId: Long, exerciseIds: List<Long>) {
    val ids = exerciseIds.map { cleanInt(it, "Exercise", 1, 1_000_000_000) }
    val wid = cleanInt(workoutId, "Workout", 1, 1_000_000_000)
    q.tx { addExercisesTx(wid, ids) }
}

fun Store.getWorkout(id: Long): WorkoutFull? {
    val w = q.first(
        """SELECT w.id, w.title, w.day, w.done, w.note, w.location_id, w.type_id, l.name, t.name, t.icon
           FROM workouts w LEFT JOIN locations l ON l.id = w.location_id LEFT JOIN workout_types t ON t.id = w.type_id
           WHERE w.id = ?""",
        id,
    ) { c -> Triple(c.long(0), listOf(c.str(1), c.str(2), c.str(4), c.str(7), c.str(8), c.str(9)), listOf(c.getInt(3), c.longOrNull(5), c.longOrNull(6))) }
        ?: return null
    val (wid, txt, misc) = w
    val done = (misc[0] as Int) == 1
    val wLoc = misc[1] as Long?
    val wType = misc[2] as Long?
    val paths = tagPaths()
    val weRows = q.map("SELECT id, position, status, note, exercise_id, location_id FROM workout_exercises WHERE workout_id = ? ORDER BY position, id", wid) {
        listOf(it.long(0), it.getInt(1), it.str(2), it.str(3), it.long(4), it.longOrNull(5))
    }
    val items = ArrayList<WeFull>()
    // The exercises and the sets are read in a few queries for the whole workout, not a handful per exercise.
    val exes = exercisesAt(weRows.map { (it[4] as Long) to ((it[5] as Long?) ?: wLoc) })
    val setsByWe = HashMap<Long, MutableList<SetRow>>()
    q.map(
        """SELECT s.we_id, s.id, s.position, s.done, s.weight, s.reps, s.intensity, s.incline, s.minutes, s.distance, s.other
           FROM sets s JOIN workout_exercises e ON e.id = s.we_id WHERE e.workout_id = ? ORDER BY s.position, s.id""",
        wid,
    ) {
        val n = numsOf(it, 4)
        setsByWe.getOrPut(it.long(0)) { ArrayList() }.add(SetRow(it.long(1), it.getInt(2), n.weight, n.reps, it.getInt(3) == 1, n))
    }
    for ((i, r) in weRows.withIndex()) {
        val weId = r[0] as Long
        val effLoc = (r[5] as Long?) ?: wLoc
        val ex = exes[i] ?: continue
        val sets = setsByWe[weId] ?: emptyList<SetRow>()
        val status = when (r[2] as String) { "done" -> "done"; "skipped" -> "skipped"; else -> "todo" }
        items.add(WeFull(weId, r[1] as Int, status, r[3] as String, ex, sets, getLinks(TagTarget.OfItem(weId), paths), effLoc))
    }
    return WorkoutFull(
        wid, txt[0], txt[1], done, txt[2], getLinks(TagTarget.OfWorkout(wid), paths), items,
        wLoc, txt[3], wType, txt[4], txt[5],
    )
}

fun Store.setWorkoutDone(id: Long, done: Boolean) {
    q.exec("UPDATE workouts SET done = ? WHERE id = ?", done, cleanInt(id, "Workout", 1, 1_000_000_000))
}

fun Store.updateWorkoutMeta(id: Long, day: String, locationId: Long?, typeId: Long?) {
    val wid = cleanInt(id, "Workout", 1, 1_000_000_000)
    val d = cleanDay(day)
    val loc = locationId?.let { cleanInt(it, "Location", 1, 1_000_000_000) }
    val typ = typeId?.let { cleanInt(it, "Workout type", 1, 1_000_000_000) }
    q.tx {
        requireRow("locations", loc, "That location")
        requireRow("workout_types", typ, "That workout type")
        val title = makeTitle(d, loc, wid)
        q.exec("UPDATE workouts SET title = ?, day = ?, location_id = ?, type_id = ? WHERE id = ?", title, d, loc, typ, wid)
    }
}

fun Store.deleteWorkout(id: Long) {
    q.exec("DELETE FROM workouts WHERE id = ?", cleanInt(id, "Workout", 1, 1_000_000_000))
}

fun Store.setWeStatus(weId: Long, status: String) {
    cleanOneOf(status, "Status", listOf("todo", "done", "skipped"))
    q.exec("UPDATE workout_exercises SET status = ? WHERE id = ?", status, cleanInt(weId, "Item", 1, 1_000_000_000))
}

fun Store.setWeNote(weId: Long, note: String) {
    val n = cleanOptionalText(note, "Note", Limits.NOTE, true)
    q.exec("UPDATE workout_exercises SET note = ? WHERE id = ?", n, cleanInt(weId, "Item", 1, 1_000_000_000))
}

fun Store.setWorkoutNote(id: Long, note: String) {
    val n = cleanOptionalText(note, "Note", Limits.NOTE, true)
    q.exec("UPDATE workouts SET note = ? WHERE id = ?", n, cleanInt(id, "Workout", 1, 1_000_000_000))
}

// Which place this exercise is done at in this workout (null goes back to the workout's place).
fun Store.setWeLocation(weId: Long, locationId: Long?) {
    val id = cleanInt(weId, "Item", 1, 1_000_000_000)
    val loc = locationId?.let { cleanInt(it, "Location", 1, 1_000_000_000) }
    q.tx {
        requireRow("locations", loc, "That location")
        q.exec("UPDATE workout_exercises SET location_id = ? WHERE id = ?", loc, id)
    }
}

// Saves a new order for the exercises of one workout. The list must hold exactly the workout's own items, each once.
fun Store.reorderWorkoutExercises(workoutId: Long, weIds: List<Long>) {
    val wid = cleanInt(workoutId, "Workout", 1, 1_000_000_000)
    val ids = weIds.map { cleanInt(it, "Item", 1, 1_000_000_000) }
    q.tx {
        val own = q.map("SELECT id FROM workout_exercises WHERE workout_id = ?", wid) { it.long(0) }
        if (own.size != ids.size || ids.toSet().size != ids.size || !ids.all { it in own }) {
            throw InputError("The order does not match this workout.")
        }
        for ((p, id) in ids.withIndex()) q.exec("UPDATE workout_exercises SET position = ? WHERE id = ? AND workout_id = ?", p, id, wid)
    }
}

fun Store.removeWe(weId: Long) {
    q.exec("DELETE FROM workout_exercises WHERE id = ?", cleanInt(weId, "Item", 1, 1_000_000_000))
}

// ---------- sets ----------

fun Store.addSet(weId: Long): Long {
    val id = cleanInt(weId, "Item", 1, 1_000_000_000)
    if (q.count("SELECT COUNT(*) FROM sets WHERE we_id = ?", id) >= Limits.MAX_SETS) throw InputError("At most ${Limits.MAX_SETS} sets.")
    val last = q.first("SELECT $SET_NUMS, position FROM sets WHERE we_id = ? ORDER BY position DESC, id DESC LIMIT 1", id) { numsOf(it) to it.getInt(7) }
    val n = last?.first ?: BLANK_SET
    return q.insert(
        "INSERT INTO sets (we_id, position, weight, reps, intensity, incline, minutes, distance, other, done) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
        id, if (last != null) last.second + 1 else 0, n.weight, n.reps, n.intensity, n.incline, n.minutes, n.distance, n.other,
    )
}

// Which number, and its limit, for each thing that can be written down. The column names are fixed here, never taken from input.
private class NumField(val key: String, val col: String, val label: String, val max: Double, val whole: Boolean)
private val NUM_FIELDS = listOf(
    NumField("weight", "weight", "Weight", Limits.MAX_WEIGHT, false),
    NumField("reps", "reps", "Reps", Limits.MAX_REPS.toDouble(), true),
    NumField("intensity", "intensity", "Intensity", Limits.MAX_INTENSITY, false),
    NumField("incline", "incline", "Incline", Limits.MAX_INCLINE, false),
    NumField("minutes", "minutes", "Time", Limits.MAX_MINUTES, false),
    NumField("distance", "distance", "Distance", Limits.MAX_DISTANCE, false),
    NumField("other", "other", "Other", Limits.MAX_OTHER, false),
)

// Writes one number of a set. The number is held: later sets of this exercise that are not done yet and had the same old number follow it.
fun Store.updateSetNumber(setId: Long, key: String, value: Double) {
    val sid = cleanInt(setId, "Set", 1, 1_000_000_000)
    val f = NUM_FIELDS.firstOrNull { it.key == key } ?: throw InputError("That is not something Spot writes down.")
    val v = if (f.whole) cleanInt(Math.round(value), f.label, 0, f.max.toLong()).toDouble() else cleanNum(value, f.label, 0.0, f.max)
    q.tx {
        val cur = q.first("SELECT we_id, position, ${f.col} FROM sets WHERE id = ?", sid) { Triple(it.long(0), it.getInt(1), it.dbl(2)) }
        q.exec("UPDATE sets SET ${f.col} = ? WHERE id = ?", v, sid)
        if (cur != null && cur.third != v) {
            q.exec(
                "UPDATE sets SET ${f.col} = ? WHERE we_id = ? AND done = 0 AND ${f.col} = ? AND id <> ? AND (position > ? OR (position = ? AND id > ?))",
                v, cur.first, cur.third, sid, cur.second, cur.second, sid,
            )
        }
    }
}

fun Store.setSetDone(setId: Long, done: Boolean) {
    q.exec("UPDATE sets SET done = ? WHERE id = ?", done, cleanInt(setId, "Set", 1, 1_000_000_000))
}

// "Done with this exercise": sets that were never logged are dropped (only when at least one set was logged).
fun Store.dropUnloggedSets(weId: Long) {
    val id = cleanInt(weId, "Item", 1, 1_000_000_000)
    if (q.count("SELECT COUNT(*) FROM sets WHERE we_id = ? AND done = 1", id) > 0) q.exec("DELETE FROM sets WHERE we_id = ? AND done = 0", id)
}

fun Store.deleteSet(setId: Long) {
    q.exec("DELETE FROM sets WHERE id = ?", cleanInt(setId, "Set", 1, 1_000_000_000))
}

// ---------- one workout item (used by Load past, Notes and Tags screens) ----------

fun Store.getWeInfo(weId: Long): WeInfo? {
    val r = q.first(
        """SELECT e.id, e.workout_id, e.exercise_id, e.note, w.location_id FROM workout_exercises e
           JOIN workouts w ON w.id = e.workout_id WHERE e.id = ?""",
        weId,
    ) { listOf(it.long(0), it.long(1), it.long(2), it.str(3), it.longOrNull(4)) } ?: return null
    val ex = getExercise(r[2] as Long, r[4] as Long?) ?: return null
    return WeInfo(r[0] as Long, r[1] as Long, r[3] as String, ex)
}

fun Store.getWorkoutTitle(id: Long): String? = q.first("SELECT title FROM workouts WHERE id = ?", id) { it.str(0) }
