package com.spotapp.personal

// Numbers behind the Home cards, Insights and the Records page. Every query uses bound parameters.

fun Store.setsByPart(from: String, to: String): Map<String, Int> {
    val rows = q.map(
        """SELECT b.part, COUNT(*) FROM sets s JOIN workout_exercises e ON e.id = s.we_id JOIN workouts w ON w.id = e.workout_id
           JOIN exercise_body_parts b ON b.exercise_id = e.exercise_id
           WHERE s.done = 1 AND w.day >= ? AND w.day <= ? GROUP BY b.part""",
        from, to,
    ) { it.str(0) to it.getInt(1) }.toMap()
    return BODY_PARTS.associateWith { rows[it] ?: 0 }
}

fun Store.gardenStats(p: Period, today: String = todayStr()): Map<String, Int> {
    val r = periodRange(p, 0, today)
    return setsByPart(r.from, r.to)
}

fun Store.volumeByPart(from: String, to: String): Map<String, Double> {
    val rows = q.map(
        """SELECT b.part, SUM(s.weight * s.reps) FROM sets s JOIN workout_exercises e ON e.id = s.we_id JOIN workouts w ON w.id = e.workout_id
           JOIN exercise_body_parts b ON b.exercise_id = e.exercise_id
           WHERE s.done = 1 AND w.day >= ? AND w.day <= ? GROUP BY b.part""",
        from, to,
    ) { it.str(0) to (if (it.isNull(1)) 0.0 else it.dbl(1)) }.toMap()
    return BODY_PARTS.associateWith { rows[it] ?: 0.0 }
}

// Push and pull sets between two days (only exercises marked push or pull).
fun Store.pushPull(from: String, to: String): Pair<Int, Int> {
    val rows = q.map(
        """SELECT x.movement, COUNT(*) FROM sets s JOIN workout_exercises e ON e.id = s.we_id JOIN workouts w ON w.id = e.workout_id
           JOIN exercises x ON x.id = e.exercise_id
           WHERE s.done = 1 AND x.movement IN ('push', 'pull') AND w.day >= ? AND w.day <= ? GROUP BY x.movement""",
        from, to,
    ) { it.str(0) to it.getInt(1) }.toMap()
    return (rows["push"] ?: 0) to (rows["pull"] ?: 0)
}

fun Store.balanceStats(p: Period, today: String = todayStr()): Pair<Int, Int> {
    val r = periodRange(p, 0, today)
    return pushPull(r.from, r.to)
}

fun Store.volumeTotal(from: String, to: String): Double =
    q.first(
        """SELECT SUM(s.weight * s.reps) FROM sets s JOIN workout_exercises e ON e.id = s.we_id JOIN workouts w ON w.id = e.workout_id
           WHERE s.done = 1 AND w.day >= ? AND w.day <= ?""",
        from, to,
    ) { if (it.isNull(0)) 0.0 else it.dbl(0) } ?: 0.0

fun Store.funTotal(p: Period, today: String = todayStr()): Double {
    val r = periodRange(p, 0, today)
    return volumeTotal(r.from, r.to)
}

// Days with at least one logged set, with the place that was used.
fun Store.trainedDays(from: String, to: String): List<TrainedDay> =
    q.map(
        """SELECT w.day, l.icon, l.color, l.name FROM workouts w LEFT JOIN locations l ON l.id = w.location_id
           WHERE w.day >= ? AND w.day <= ?
             AND EXISTS (SELECT 1 FROM workout_exercises e JOIN sets s ON s.we_id = e.id WHERE e.workout_id = w.id AND s.done = 1)
           ORDER BY w.day, w.id""",
        from, to,
    ) { TrainedDay(it.str(0), it.str(1), it.str(2), it.str(3)) }

fun Store.streakNow(today: String = todayStr()): Int =
    streakWeeks(trainedDays(addDays(today, -7L * 60), today).map { it.day }, today)

fun Store.listRecords(f: RecordFilter = RecordFilter(), today: String = todayStr()): List<RecordRow> {
    class SetBest(val weight: Double, val reps: Int, val day: String, val exerciseId: Long, val locId: Long?)
    val rows = q.map(
        """SELECT s.weight, s.reps, w.day, e.exercise_id, COALESCE(e.location_id, w.location_id)
           FROM sets s JOIN workout_exercises e ON e.id = s.we_id JOIN workouts w ON w.id = e.workout_id
           WHERE s.done = 1 AND s.weight > 0 ORDER BY w.day, e.id, s.position, s.id""",
    ) { SetBest(it.dbl(0), it.getInt(1), it.str(2), it.long(3), it.longOrNull(4)) }
    val best = LinkedHashMap<Long, SetBest>()
    for (r in rows) {
        val b = best[r.exerciseId]
        if (b == null || r.weight > b.weight || (r.weight == b.weight && r.reps >= b.reps)) best[r.exerciseId] = r
    }
    val exMap = listExercises(true).associateBy { it.id }
    val locMap = listLocations().associateBy { it.id }
    val range = periodRange(f.period ?: Period.ALL, 0, today)
    val text = f.text.trim().lowercase()
    val out = ArrayList<RecordRow>()
    for ((exId, r) in best) {
        val ex = exMap[exId] ?: continue
        val loc = r.locId?.let { locMap[it] }
        if (text.isNotEmpty() && !ex.name.lowercase().contains(text)) continue
        if (f.bodyParts.isNotEmpty() && f.bodyParts.none { it in ex.bodyParts }) continue
        if (f.locationIds.isNotEmpty() && (r.locId == null || r.locId !in f.locationIds)) continue
        if (f.equipment.isNotEmpty() && ex.equipment !in f.equipment) continue
        if (f.movement.isNotEmpty() && ex.movement != f.movement) continue
        if (f.exTagIds.isNotEmpty() && f.exTagIds.none { it in ex.exTagIds }) continue
        val isNew = r.day >= range.from && r.day <= range.to
        if (f.period != null && f.period != Period.ALL && !isNew) continue
        out.add(
            RecordRow(
                exId, ex.name, ex.icon, r.weight, r.reps, r.day, ex.unit, r.locId, loc?.name ?: "", loc?.icon ?: "", loc?.color ?: "",
                ex.bodyParts, ex.equipment, ex.movement, ex.exTagIds, isNew,
            ),
        )
    }
    return out.sortedWith(compareByDescending<RecordRow> { it.day }.thenBy { it.name.lowercase() })
}

// Used when a set is saved: is this weight a new record for the exercise? (Heavier than every earlier logged set.)
fun Store.isNewRecord(exerciseId: Long, weight: Double, excludeSetId: Long): Boolean {
    if (weight <= 0) return false
    val prior = q.first(
        """SELECT MAX(s.weight), COUNT(*) FROM sets s JOIN workout_exercises e ON e.id = s.we_id
           WHERE e.exercise_id = ? AND s.done = 1 AND s.id <> ?""",
        exerciseId, excludeSetId,
    ) { (if (it.isNull(0)) null else it.dbl(0)) to it.getInt(1) } ?: return false
    // The first time an exercise is logged is a starting point, not a record to celebrate.
    val best = prior.first ?: return false
    return prior.second > 0 && weight > best
}

// The last n weeks, oldest first, each with its weight lifted, sets by body part and days trained.
fun Store.weekSeries(n: Int, today: String = todayStr()): List<WeekPoint> =
    (n - 1 downTo 0).map { i ->
        val r = periodRange(Period.WEEK, -i, today)
        WeekPoint(r.from, r.to, volumeTotal(r.from, r.to), setsByPart(r.from, r.to), trainedDays(r.from, r.to).map { it.day }.toSet().size)
    }
