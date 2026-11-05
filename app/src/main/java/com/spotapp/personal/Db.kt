package com.spotapp.personal

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

// All queries use bound parameters (? with an args array, ContentValues, or "id = ?" clauses).
// No SQL is built by joining user text. The schema matches the iPhone app so a backup file moves between phones.

data class SetLite(val weight: Double, val reps: Int)
data class Exercise(
    val id: Long, val name: String, val machine: String, val gym: String,
    val unit: String, val step: Double, val cues: List<String>, val archived: Boolean,
)
data class Tag(val id: Long, val name: String, val kind: String)
data class TagLink(val tagId: Long, val name: String, val kind: String, val severity: Int?)
data class SetRow(val id: Long, val position: Int, val weight: Double, val reps: Int, val done: Boolean)
data class WeFull(
    val id: Long, val position: Int, val status: String, val note: String,
    val exercise: Exercise, val sets: List<SetRow>, val tags: List<TagLink>,
)
data class WorkoutFull(
    val id: Long, val title: String, val day: String, val done: Boolean, val note: String,
    val tags: List<TagLink>, val items: List<WeFull>,
)
data class WorkoutRow(val id: Long, val title: String, val day: String, val done: Boolean, val exCount: Int, val exDone: Int)
data class Session(
    val weId: Long, val workoutId: Long, val workoutTitle: String, val day: String,
    val sets: List<SetLite>, val tags: List<TagLink>,
)
data class WeInfo(val id: Long, val workoutId: Long, val note: String, val exercise: Exercise)

object Limits {
    const val NAME = 60
    const val MACHINE = 40
    const val GYM = 60
    const val CUE = 80
    const val CUES = 12
    const val NOTE = 600
    const val TITLE = 60
    const val TAG = 30
    const val MAX_WEIGHT = 2000.0
    const val MAX_REPS = 500L
    const val MAX_SETS = 30
    const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
}

val SETTING_KEYS = listOf("theme", "palette", "voice", "onboarded", "tend", "unit")
val SETTING_VALUES: Map<String, List<String>> = mapOf(
    "theme" to listOf("paper", "evening", "system"),
    "palette" to listOf("citrus", "lagoon"),
    "voice" to listOf("whimsical", "cheeky", "gentle", "callout"),
    "onboarded" to listOf("0", "1"),
    "tend" to listOf("0", "1"),
    "unit" to listOf("lb", "kg"),
)
val SETTING_DEFAULTS: Map<String, String> = mapOf(
    "theme" to "system", "palette" to "lagoon", "voice" to "gentle", "onboarded" to "0", "tend" to "0", "unit" to "lb",
)

private val SCHEMA = listOf(
    """CREATE TABLE exercises (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        machine TEXT NOT NULL DEFAULT '',
        gym TEXT NOT NULL DEFAULT '',
        unit TEXT NOT NULL DEFAULT 'lb',
        step REAL NOT NULL DEFAULT 5,
        cues TEXT NOT NULL DEFAULT '',
        archived INTEGER NOT NULL DEFAULT 0
    )""",
    """CREATE TABLE workouts (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title TEXT NOT NULL,
        day TEXT NOT NULL,
        done INTEGER NOT NULL DEFAULT 0,
        note TEXT NOT NULL DEFAULT ''
    )""",
    """CREATE TABLE workout_exercises (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        workout_id INTEGER NOT NULL REFERENCES workouts(id) ON DELETE CASCADE,
        exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
        position INTEGER NOT NULL,
        status TEXT NOT NULL DEFAULT 'todo',
        note TEXT NOT NULL DEFAULT ''
    )""",
    """CREATE TABLE sets (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        we_id INTEGER NOT NULL REFERENCES workout_exercises(id) ON DELETE CASCADE,
        position INTEGER NOT NULL,
        weight REAL NOT NULL DEFAULT 0,
        reps INTEGER NOT NULL DEFAULT 0,
        done INTEGER NOT NULL DEFAULT 0
    )""",
    """CREATE TABLE tags (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL UNIQUE,
        kind TEXT NOT NULL DEFAULT 'general'
    )""",
    """CREATE TABLE tag_links (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
        workout_id INTEGER REFERENCES workouts(id) ON DELETE CASCADE,
        we_id INTEGER REFERENCES workout_exercises(id) ON DELETE CASCADE,
        severity INTEGER,
        CHECK ((workout_id IS NULL) <> (we_id IS NULL)),
        CHECK (severity IS NULL OR (severity >= 1 AND severity <= 5))
    )""",
    "CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)",
    "CREATE INDEX idx_we_workout ON workout_exercises(workout_id)",
    "CREATE INDEX idx_we_exercise ON workout_exercises(exercise_id)",
    "CREATE INDEX idx_sets_we ON sets(we_id)",
    "CREATE INDEX idx_links_workout ON tag_links(workout_id)",
    "CREATE INDEX idx_links_we ON tag_links(we_id)",
    "CREATE INDEX idx_workouts_day ON workouts(day)",
)

private class Helper(context: Context) : SQLiteOpenHelper(context, "spot.db", null, 1) {
    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        for (s in SCHEMA) db.execSQL(s)
    }

    // Numbered migrations go here. Add a new step at the end; never edit an old one.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
}

private inline fun <T> Cursor.mapAll(map: (Cursor) -> T): List<T> {
    use {
        val out = ArrayList<T>()
        while (moveToNext()) out.add(map(this))
        return out
    }
}

private inline fun <T> Cursor.first(map: (Cursor) -> T): T? {
    use {
        return if (moveToFirst()) map(this) else null
    }
}

private fun args(vararg a: Any?): Array<String> = a.map { it.toString() }.toTypedArray()

class Repo(context: Context) {
    private val helper = Helper(context.applicationContext)
    private val db: SQLiteDatabase get() = helper.writableDatabase

    private inline fun <T> tx(block: () -> T): T {
        val d = db
        d.beginTransaction()
        try {
            val r = block()
            d.setTransactionSuccessful()
            return r
        } finally {
            d.endTransaction()
        }
    }

    private fun q(sql: String, vararg a: Any?): Cursor = db.rawQuery(sql, args(*a))

    // ---------- settings ----------

    fun getSettings(): Map<String, String> {
        val out = SETTING_DEFAULTS.toMutableMap()
        q("SELECT key, value FROM settings").mapAll { c ->
            val k = c.getString(0)
            val v = c.getString(1)
            if (k in SETTING_KEYS && SETTING_VALUES[k]!!.contains(v)) out[k] = v
        }
        return out
    }

    fun setSetting(key: String, value: String) {
        cleanOneOf(key, "Setting", SETTING_KEYS)
        cleanOneOf(value, "Setting value", SETTING_VALUES[key]!!)
        val cv = ContentValues()
        cv.put("key", key)
        cv.put("value", value)
        db.insertWithOnConflict("settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // ---------- exercises ----------

    private fun mapExercise(c: Cursor): Exercise {
        val cues = c.getString(6)
        return Exercise(
            id = c.getLong(0), name = c.getString(1), machine = c.getString(2), gym = c.getString(3),
            unit = if (c.getString(4) == "kg") "kg" else "lb",
            step = c.getDouble(5).let { if (it > 0) it else 5.0 },
            cues = if (cues.isEmpty()) emptyList() else cues.split("\n").filter { it.isNotEmpty() },
            archived = c.getInt(7) == 1,
        )
    }

    private val exCols = "id, name, machine, gym, unit, step, cues, archived"

    fun listExercises(includeArchived: Boolean = false): List<Exercise> =
        q(
            if (includeArchived) "SELECT $exCols FROM exercises ORDER BY archived, name COLLATE NOCASE"
            else "SELECT $exCols FROM exercises WHERE archived = 0 ORDER BY name COLLATE NOCASE",
        ).mapAll { mapExercise(it) }

    fun getExercise(id: Long): Exercise? = q("SELECT $exCols FROM exercises WHERE id = ?", id).first { mapExercise(it) }

    fun listGyms(): List<String> =
        q("SELECT DISTINCT gym FROM exercises WHERE gym <> '' ORDER BY gym COLLATE NOCASE").mapAll { it.getString(0) }

    fun saveExercise(
        id: Long?, name: String, machine: String, gym: String, unit: String, step: Double, cues: List<String>,
    ): Long {
        val n = cleanText(name, "Name", 1, Limits.NAME)
        val m = cleanOptionalText(machine, "Machine", Limits.MACHINE)
        val g = cleanOptionalText(gym, "Gym", Limits.GYM)
        val u = cleanOneOf(unit, "Unit", listOf("lb", "kg"))
        val s = cleanNum(step, "Step", 0.25, 100.0)
        val cs = cues.map { cleanOptionalText(it, "Cue", Limits.CUE) }.filter { it.isNotEmpty() }
        if (cs.size > Limits.CUES) throw InputError("At most ${Limits.CUES} cues.")
        val cv = ContentValues()
        cv.put("name", n); cv.put("machine", m); cv.put("gym", g); cv.put("unit", u); cv.put("step", s)
        cv.put("cues", cs.joinToString("\n"))
        if (id != null) {
            db.update("exercises", cv, "id = ?", args(id))
            return id
        }
        return db.insertOrThrow("exercises", null, cv)
    }

    fun setArchived(id: Long, archived: Boolean) {
        val cv = ContentValues()
        cv.put("archived", if (archived) 1 else 0)
        db.update("exercises", cv, "id = ?", args(id))
    }

    // ---------- tags ----------

    fun listTags(): List<Tag> =
        q("SELECT id, name, kind FROM tags ORDER BY kind, name COLLATE NOCASE")
            .mapAll { Tag(it.getLong(0), it.getString(1), if (it.getString(2) == "health") "health" else "general") }

    fun saveTag(id: Long?, name: String, kind: String): Long {
        val n = cleanText(name, "Tag name", 1, Limits.TAG)
        val k = cleanOneOf(kind, "Tag type", listOf("health", "general"))
        val dup = q("SELECT id FROM tags WHERE name = ? COLLATE NOCASE", n).first { it.getLong(0) }
        if (dup != null && dup != id) throw InputError("A tag with that name already exists.")
        val cv = ContentValues()
        cv.put("name", n); cv.put("kind", k)
        if (id != null) {
            db.update("tags", cv, "id = ?", args(id))
            return id
        }
        return db.insertOrThrow("tags", null, cv)
    }

    fun deleteTag(id: Long) {
        db.delete("tags", "id = ?", args(id))
    }

    private fun linksFor(column: String, id: Long): List<TagLink> =
        q(
            "SELECT l.tag_id, t.name, t.kind, l.severity FROM tag_links l JOIN tags t ON t.id = l.tag_id " +
                "WHERE l.$column = ? ORDER BY t.kind, t.name COLLATE NOCASE",
            id,
        ).mapAll {
            TagLink(it.getLong(0), it.getString(1), if (it.getString(2) == "health") "health" else "general", if (it.isNull(3)) null else it.getInt(3))
        }

    // column is one of two fixed names, never user text
    fun getLinksForWorkout(workoutId: Long): List<TagLink> = linksFor("workout_id", workoutId)
    fun getLinksForWe(weId: Long): List<TagLink> = linksFor("we_id", weId)

    fun setLinks(isWorkout: Boolean, targetId: Long, links: List<Pair<Long, Int?>>) {
        for ((t, s) in links) {
            cleanInt(t, "Tag", 1, 1_000_000_000)
            if (s != null) cleanInt(s, "Severity", 1, 5)
        }
        tx {
            val col = if (isWorkout) "workout_id" else "we_id"
            db.delete("tag_links", "$col = ?", args(targetId))
            for ((t, s) in links) {
                val cv = ContentValues()
                cv.put("tag_id", t)
                cv.put(col, targetId)
                if (s == null) cv.putNull("severity") else cv.put("severity", s)
                db.insertOrThrow("tag_links", null, cv)
            }
        }
    }

    // ---------- workouts ----------

    fun listWorkouts(): List<WorkoutRow> =
        q(
            """SELECT w.id, w.title, w.day, w.done,
                (SELECT COUNT(*) FROM workout_exercises e WHERE e.workout_id = w.id),
                (SELECT COUNT(*) FROM workout_exercises e WHERE e.workout_id = w.id AND e.status = 'done')
               FROM workouts w ORDER BY w.day DESC, w.id DESC LIMIT 400""",
        ).mapAll { WorkoutRow(it.getLong(0), it.getString(1), it.getString(2), it.getInt(3) == 1, it.getInt(4), it.getInt(5)) }

    private fun seedSets(weId: Long, exerciseId: Long) {
        // Start from the most recent logged session of this exercise; else three plain sets.
        val last = q(
            """SELECT e.id FROM workout_exercises e JOIN workouts w ON w.id = e.workout_id
               WHERE e.exercise_id = ? AND e.id <> ? AND EXISTS (SELECT 1 FROM sets s WHERE s.we_id = e.id AND s.done = 1)
               ORDER BY w.day DESC, e.id DESC LIMIT 1""",
            exerciseId, weId,
        ).first { it.getLong(0) }
        if (last != null) {
            val rows = q("SELECT weight, reps FROM sets WHERE we_id = ? AND done = 1 ORDER BY position, id", last)
                .mapAll { SetLite(it.getDouble(0), it.getInt(1)) }
            rows.forEachIndexed { p, r -> insertSet(weId, p, r.weight, r.reps) }
            return
        }
        for (p in 0 until 3) insertSet(weId, p, 0.0, 10)
    }

    private fun insertSet(weId: Long, position: Int, weight: Double, reps: Int): Long {
        val cv = ContentValues()
        cv.put("we_id", weId); cv.put("position", position); cv.put("weight", weight); cv.put("reps", reps); cv.put("done", 0)
        return db.insertOrThrow("sets", null, cv)
    }

    private fun addExercisesTx(workoutId: Long, exerciseIds: List<Long>) {
        val m = q("SELECT MAX(position) FROM workout_exercises WHERE workout_id = ?", workoutId)
            .first { if (it.isNull(0)) -1 else it.getInt(0) } ?: -1
        var pos = m + 1
        for (exId in exerciseIds) {
            val cv = ContentValues()
            cv.put("workout_id", workoutId); cv.put("exercise_id", exId); cv.put("position", pos++); cv.put("status", "todo")
            val weId = db.insertOrThrow("workout_exercises", null, cv)
            seedSets(weId, exId)
        }
    }

    fun createWorkout(title: String, day: String, exerciseIds: List<Long>): Long {
        val t = cleanText(title, "Title", 1, Limits.TITLE)
        val d = cleanDay(day)
        exerciseIds.forEach { cleanInt(it, "Exercise", 1, 1_000_000_000) }
        if (exerciseIds.size > 40) throw InputError("At most 40 exercises in one workout.")
        return tx {
            val cv = ContentValues()
            cv.put("title", t); cv.put("day", d)
            val id = db.insertOrThrow("workouts", null, cv)
            addExercisesTx(id, exerciseIds)
            id
        }
    }

    fun addExercisesToWorkout(workoutId: Long, exerciseIds: List<Long>) {
        exerciseIds.forEach { cleanInt(it, "Exercise", 1, 1_000_000_000) }
        tx { addExercisesTx(workoutId, exerciseIds) }
    }

    fun getWorkout(id: Long): WorkoutFull? {
        val w = q("SELECT id, title, day, done, note FROM workouts WHERE id = ?", id)
            .first { arrayOf<Any>(it.getLong(0), it.getString(1), it.getString(2), it.getInt(3), it.getString(4)) } ?: return null
        val weRows = q(
            "SELECT id, position, status, note, exercise_id FROM workout_exercises WHERE workout_id = ? ORDER BY position, id", id,
        ).mapAll { arrayOf<Any>(it.getLong(0), it.getInt(1), it.getString(2), it.getString(3), it.getLong(4)) }
        val items = ArrayList<WeFull>()
        for (r in weRows) {
            val ex = getExercise(r[4] as Long) ?: continue
            val weId = r[0] as Long
            val sets = q("SELECT id, position, weight, reps, done FROM sets WHERE we_id = ? ORDER BY position, id", weId)
                .mapAll { SetRow(it.getLong(0), it.getInt(1), it.getDouble(2), it.getInt(3), it.getInt(4) == 1) }
            val status = when (r[2] as String) { "done" -> "done"; "skipped" -> "skipped"; else -> "todo" }
            items.add(WeFull(weId, r[1] as Int, status, r[3] as String, ex, sets, getLinksForWe(weId)))
        }
        return WorkoutFull(w[0] as Long, w[1] as String, w[2] as String, (w[3] as Int) == 1, w[4] as String, getLinksForWorkout(id), items)
    }

    fun setWorkoutDone(id: Long, done: Boolean) {
        val cv = ContentValues()
        cv.put("done", if (done) 1 else 0)
        db.update("workouts", cv, "id = ?", args(id))
    }

    fun deleteWorkout(id: Long) {
        db.delete("workouts", "id = ?", args(id))
    }

    fun setWeStatus(weId: Long, status: String) {
        cleanOneOf(status, "Status", listOf("todo", "done", "skipped"))
        val cv = ContentValues()
        cv.put("status", status)
        db.update("workout_exercises", cv, "id = ?", args(weId))
    }

    fun setWeNote(weId: Long, note: String) {
        val n = cleanOptionalText(note, "Note", Limits.NOTE, true)
        val cv = ContentValues()
        cv.put("note", n)
        db.update("workout_exercises", cv, "id = ?", args(weId))
    }

    fun removeWe(weId: Long) {
        db.delete("workout_exercises", "id = ?", args(weId))
    }

    fun getWeInfo(weId: Long): WeInfo? {
        val r = q("SELECT id, workout_id, exercise_id, note FROM workout_exercises WHERE id = ?", weId)
            .first { arrayOf<Any>(it.getLong(0), it.getLong(1), it.getLong(2), it.getString(3)) } ?: return null
        val ex = getExercise(r[2] as Long) ?: return null
        return WeInfo(r[0] as Long, r[1] as Long, r[3] as String, ex)
    }

    fun getWorkoutTitle(id: Long): String? = q("SELECT title FROM workouts WHERE id = ?", id).first { it.getString(0) }

    // ---------- sets ----------

    fun addSet(weId: Long): Long {
        val cnt = q("SELECT COUNT(*) FROM sets WHERE we_id = ?", weId).first { it.getInt(0) } ?: 0
        if (cnt >= Limits.MAX_SETS) throw InputError("At most ${Limits.MAX_SETS} sets.")
        val last = q("SELECT weight, reps, position FROM sets WHERE we_id = ? ORDER BY position DESC, id DESC LIMIT 1", weId)
            .first { arrayOf<Any>(it.getDouble(0), it.getInt(1), it.getInt(2)) }
        return insertSet(weId, if (last != null) (last[2] as Int) + 1 else 0, if (last != null) last[0] as Double else 0.0, if (last != null) last[1] as Int else 10)
    }

    fun updateSet(setId: Long, weight: Double? = null, reps: Int? = null, done: Boolean? = null) {
        val cv = ContentValues()
        if (weight != null) cv.put("weight", cleanNum(weight, "Weight", 0.0, Limits.MAX_WEIGHT))
        if (reps != null) cv.put("reps", cleanInt(reps, "Reps", 0, Limits.MAX_REPS))
        if (done != null) cv.put("done", if (done) 1 else 0)
        if (cv.size() > 0) db.update("sets", cv, "id = ?", args(setId))
    }

    fun deleteSet(setId: Long) {
        db.delete("sets", "id = ?", args(setId))
    }

    // ---------- past sessions ("where I left off") ----------

    fun pastSessions(exerciseId: Long, excludeWeId: Long): List<Session> {
        val rows = q(
            """SELECT e.id, w.id, w.title, w.day
               FROM workout_exercises e JOIN workouts w ON w.id = e.workout_id
               WHERE e.exercise_id = ? AND e.id <> ?
                 AND EXISTS (SELECT 1 FROM sets s WHERE s.we_id = e.id AND s.done = 1)
               ORDER BY w.day DESC, e.id DESC LIMIT 60""",
            exerciseId, excludeWeId,
        ).mapAll { arrayOf<Any>(it.getLong(0), it.getLong(1), it.getString(2), it.getString(3)) }
        return rows.map { r ->
            val weId = r[0] as Long
            val wId = r[1] as Long
            val sets = q("SELECT weight, reps FROM sets WHERE we_id = ? AND done = 1 ORDER BY position, id", weId)
                .mapAll { SetLite(it.getDouble(0), it.getInt(1)) }
            Session(weId, wId, r[2] as String, r[3] as String, sets, getLinksForWorkout(wId) + getLinksForWe(weId))
        }
    }

    // Replaces the not-yet-logged sets of an exercise with a copy of an earlier session.
    fun loadSession(weId: Long, fromWeId: Long) {
        tx {
            val src = q("SELECT weight, reps FROM sets WHERE we_id = ? AND done = 1 ORDER BY position, id", fromWeId)
                .mapAll { SetLite(it.getDouble(0), it.getInt(1)) }
            if (src.isEmpty()) throw InputError("That session has no logged sets.")
            db.delete("sets", "we_id = ? AND done = 0", args(weId))
            var p = (q("SELECT MAX(position) FROM sets WHERE we_id = ?", weId).first { if (it.isNull(0)) -1 else it.getInt(0) } ?: -1) + 1
            for (s in src) insertSet(weId, p++, s.weight, s.reps)
        }
    }

    // ---------- backup and restore (same file format as the iPhone app) ----------

    fun exportAll(): String {
        val o = JSONObject()
        o.put("app", "spot")
        o.put("version", 1)
        o.put("exportedAt", java.time.Instant.now().toString())
        o.put("exercises", dump("SELECT $exCols FROM exercises ORDER BY id", listOf("id", "name", "machine", "gym", "unit", "step", "cues", "archived")))
        o.put("workouts", dump("SELECT id, title, day, done, note FROM workouts ORDER BY id", listOf("id", "title", "day", "done", "note")))
        o.put("workout_exercises", dump("SELECT id, workout_id, exercise_id, position, status, note FROM workout_exercises ORDER BY id", listOf("id", "workout_id", "exercise_id", "position", "status", "note")))
        o.put("sets", dump("SELECT id, we_id, position, weight, reps, done FROM sets ORDER BY id", listOf("id", "we_id", "position", "weight", "reps", "done")))
        o.put("tags", dump("SELECT id, name, kind FROM tags ORDER BY id", listOf("id", "name", "kind")))
        o.put("tag_links", dump("SELECT id, tag_id, workout_id, we_id, severity FROM tag_links ORDER BY id", listOf("id", "tag_id", "workout_id", "we_id", "severity")))
        val s = JSONObject()
        for ((k, v) in getSettings()) s.put(k, v)
        o.put("settings", s)
        return o.toString()
    }

    private fun dump(sql: String, names: List<String>): JSONArray {
        val arr = JSONArray()
        db.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                val row = JSONObject()
                for (i in names.indices) {
                    when (c.getType(i)) {
                        Cursor.FIELD_TYPE_NULL -> row.put(names[i], JSONObject.NULL)
                        Cursor.FIELD_TYPE_INTEGER -> row.put(names[i], c.getLong(i))
                        Cursor.FIELD_TYPE_FLOAT -> row.put(names[i], c.getDouble(i))
                        else -> row.put(names[i], c.getString(i))
                    }
                }
                arr.put(row)
            }
        }
        return arr
    }

    private fun rowsOf(o: JSONObject, key: String, label: String, max: Int): List<JSONObject> {
        val a = o.opt(key)
        if (a !is JSONArray) throw InputError("Backup is missing $label.")
        if (a.length() > max) throw InputError("Backup has too many $label.")
        val out = ArrayList<JSONObject>()
        for (i in 0 until a.length()) {
            val r = a.opt(i)
            if (r !is JSONObject) throw InputError("Backup $label are not valid.")
            out.add(r)
        }
        return out
    }

    private fun idOf(v: Any?, label: String): Long = cleanInt(v, label, 1, 1_000_000_000)
    private fun nullable(v: Any?): Any? = if (v == null || v == JSONObject.NULL) null else v

    // Checks the whole file first. Nothing is written unless every row passes.
    fun importAll(text: String): Pair<Int, Int> {
        if (text.isEmpty()) throw InputError("The backup file is empty.")
        if (text.length > Limits.MAX_BACKUP_BYTES) throw InputError("The backup file is too large.")
        val o = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw InputError("This is not a Spot backup file.")
        }
        if (o.opt("app") != "spot") throw InputError("This is not a Spot backup file.")
        if (o.opt("version") != 1) throw InputError("This backup was made by a different version of Spot.")

        val ex = rowsOf(o, "exercises", "exercises", 5000).map { r ->
            arrayOf<Any>(
                idOf(r.opt("id"), "Exercise id"),
                cleanText(r.opt("name"), "Name", 1, Limits.NAME),
                cleanOptionalText(nullable(r.opt("machine")), "Machine", Limits.MACHINE),
                cleanOptionalText(nullable(r.opt("gym")), "Gym", Limits.GYM),
                cleanOneOf(r.opt("unit"), "Unit", listOf("lb", "kg")),
                cleanNum(r.opt("step"), "Step", 0.25, 100.0),
                cleanOptionalText(nullable(r.opt("cues")), "Cues", Limits.CUE * Limits.CUES + Limits.CUES, true),
                cleanInt(r.opt("archived"), "Archived", 0, 1),
            )
        }
        val wk = rowsOf(o, "workouts", "workouts", 20000).map { r ->
            arrayOf<Any>(
                idOf(r.opt("id"), "Workout id"),
                cleanText(r.opt("title"), "Title", 1, Limits.TITLE),
                cleanDay(r.opt("day")),
                cleanInt(r.opt("done"), "Done", 0, 1),
                cleanOptionalText(nullable(r.opt("note")), "Note", Limits.NOTE, true),
            )
        }
        val we = rowsOf(o, "workout_exercises", "workout exercises", 200000).map { r ->
            arrayOf<Any>(
                idOf(r.opt("id"), "Item id"),
                idOf(r.opt("workout_id"), "Workout"),
                idOf(r.opt("exercise_id"), "Exercise"),
                cleanInt(r.opt("position"), "Position", 0, 100000),
                cleanOneOf(r.opt("status"), "Status", listOf("todo", "done", "skipped")),
                cleanOptionalText(nullable(r.opt("note")), "Note", Limits.NOTE, true),
            )
        }
        val st = rowsOf(o, "sets", "sets", 1000000).map { r ->
            arrayOf<Any>(
                idOf(r.opt("id"), "Set id"),
                idOf(r.opt("we_id"), "Item"),
                cleanInt(r.opt("position"), "Position", 0, 100000),
                cleanNum(r.opt("weight"), "Weight", 0.0, Limits.MAX_WEIGHT),
                cleanInt(r.opt("reps"), "Reps", 0, Limits.MAX_REPS),
                cleanInt(r.opt("done"), "Done", 0, 1),
            )
        }
        val tg = rowsOf(o, "tags", "tags", 2000).map { r ->
            arrayOf<Any>(
                idOf(r.opt("id"), "Tag id"),
                cleanText(r.opt("name"), "Tag name", 1, Limits.TAG),
                cleanOneOf(r.opt("kind"), "Tag type", listOf("health", "general")),
            )
        }
        val tl = rowsOf(o, "tag_links", "tag links", 500000).map { r ->
            val wId = nullable(r.opt("workout_id"))?.let { idOf(it, "Workout") }
            val eId = nullable(r.opt("we_id"))?.let { idOf(it, "Item") }
            if ((wId == null) == (eId == null)) throw InputError("Backup tag links are not valid.")
            val sev = nullable(r.opt("severity"))?.let { cleanInt(it, "Severity", 1, 5) }
            arrayOf<Any?>(idOf(r.opt("id"), "Link id"), idOf(r.opt("tag_id"), "Tag"), wId, eId, sev)
        }

        val exIds = ex.map { it[0] as Long }.toSet()
        val wkIds = wk.map { it[0] as Long }.toSet()
        val weIds = we.map { it[0] as Long }.toSet()
        val tgIds = tg.map { it[0] as Long }.toSet()
        if (exIds.size != ex.size || wkIds.size != wk.size || weIds.size != we.size || tgIds.size != tg.size) {
            throw InputError("Backup has repeated ids.")
        }
        for (r in we) if ((r[1] as Long) !in wkIds || (r[2] as Long) !in exIds) throw InputError("Backup links do not match.")
        for (r in st) if ((r[1] as Long) !in weIds) throw InputError("Backup links do not match.")
        for (r in tl) {
            if ((r[1] as Long) !in tgIds) throw InputError("Backup links do not match.")
            val w = r[2] as Long?
            val e = r[3] as Long?
            if (w != null && w !in wkIds) throw InputError("Backup links do not match.")
            if (e != null && e !in weIds) throw InputError("Backup links do not match.")
        }
        if (tg.map { (it[1] as String).lowercase() }.toSet().size != tg.size) throw InputError("Backup has repeated tag names.")

        val settings = HashMap<String, String>()
        val so = o.optJSONObject("settings")
        if (so != null) {
            for (k in SETTING_KEYS) {
                if (k == "onboarded") continue
                val v = so.opt(k)
                if (v is String && SETTING_VALUES[k]!!.contains(v)) settings[k] = v
            }
        }

        tx {
            for (t in listOf("tag_links", "sets", "workout_exercises", "workouts", "tags", "exercises")) db.delete(t, null, null)
            for (e in ex) {
                val cv = ContentValues()
                cv.put("id", e[0] as Long); cv.put("name", e[1] as String); cv.put("machine", e[2] as String); cv.put("gym", e[3] as String)
                cv.put("unit", e[4] as String); cv.put("step", e[5] as Double); cv.put("cues", e[6] as String); cv.put("archived", e[7] as Long)
                db.insertOrThrow("exercises", null, cv)
            }
            for (w in wk) {
                val cv = ContentValues()
                cv.put("id", w[0] as Long); cv.put("title", w[1] as String); cv.put("day", w[2] as String); cv.put("done", w[3] as Long); cv.put("note", w[4] as String)
                db.insertOrThrow("workouts", null, cv)
            }
            for (r in we) {
                val cv = ContentValues()
                cv.put("id", r[0] as Long); cv.put("workout_id", r[1] as Long); cv.put("exercise_id", r[2] as Long); cv.put("position", r[3] as Long)
                cv.put("status", r[4] as String); cv.put("note", r[5] as String)
                db.insertOrThrow("workout_exercises", null, cv)
            }
            for (s in st) {
                val cv = ContentValues()
                cv.put("id", s[0] as Long); cv.put("we_id", s[1] as Long); cv.put("position", s[2] as Long); cv.put("weight", s[3] as Double)
                cv.put("reps", s[4] as Long); cv.put("done", s[5] as Long)
                db.insertOrThrow("sets", null, cv)
            }
            for (t in tg) {
                val cv = ContentValues()
                cv.put("id", t[0] as Long); cv.put("name", t[1] as String); cv.put("kind", t[2] as String)
                db.insertOrThrow("tags", null, cv)
            }
            for (l in tl) {
                val cv = ContentValues()
                cv.put("id", l[0] as Long); cv.put("tag_id", l[1] as Long)
                if (l[2] == null) cv.putNull("workout_id") else cv.put("workout_id", l[2] as Long)
                if (l[3] == null) cv.putNull("we_id") else cv.put("we_id", l[3] as Long)
                if (l[4] == null) cv.putNull("severity") else cv.put("severity", l[4] as Long)
                db.insertOrThrow("tag_links", null, cv)
            }
            for ((k, v) in settings) setSetting(k, v)
        }
        return Pair(wk.size, ex.size)
    }
}
