package com.spotapp.personal

import java.time.Instant

// Backup and restore. The file is the same shape the iPhone app makes (version 5), so it moves between phones.

private fun Store.table(sql: String, vararg names: String): List<Map<String, Any?>> =
    q.map(sql) { c ->
        val m = LinkedHashMap<String, Any?>()
        for ((i, n) in names.withIndex()) {
            m[n] = if (c.isNull(i)) null else when (c.getType(i)) {
                android.database.Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                android.database.Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                else -> c.getString(i)
            }
        }
        m
    }

fun Store.exportAll(): String {
    val data = linkedMapOf<String, Any?>(
        "app" to "spot",
        "version" to BACKUP_VERSION.toLong(),
        "exportedAt" to Instant.now().toString(),
        "locations" to table("SELECT id, name, icon, color FROM locations ORDER BY id", "id", "name", "icon", "color"),
        "workout_types" to table("SELECT id, name, icon FROM workout_types ORDER BY id", "id", "name", "icon"),
        "exercises" to table(
            "SELECT id, name, icon, equipment, movement, cues, archived, link FROM exercises ORDER BY id",
            "id", "name", "icon", "equipment", "movement", "cues", "archived", "link",
        ),
        "exercise_body_parts" to table("SELECT exercise_id, part FROM exercise_body_parts ORDER BY exercise_id, part", "exercise_id", "part"),
        "ex_tags" to table("SELECT id, name FROM ex_tags ORDER BY id", "id", "name"),
        "exercise_ex_tags" to table("SELECT exercise_id, tag_id FROM exercise_ex_tags ORDER BY exercise_id, tag_id", "exercise_id", "tag_id"),
        "exercise_locations" to table(
            "SELECT exercise_id, location_id, machine FROM exercise_locations ORDER BY exercise_id, location_id",
            "exercise_id", "location_id", "machine",
        ),
        "workouts" to table(
            "SELECT id, title, day, done, note, location_id, type_id FROM workouts ORDER BY id",
            "id", "title", "day", "done", "note", "location_id", "type_id",
        ),
        "workout_exercises" to table(
            "SELECT id, workout_id, exercise_id, position, status, note, location_id FROM workout_exercises ORDER BY id",
            "id", "workout_id", "exercise_id", "position", "status", "note", "location_id",
        ),
        "sets" to table("SELECT id, we_id, position, weight, reps, done FROM sets ORDER BY id", "id", "we_id", "position", "weight", "reps", "done"),
        "tags" to table("SELECT id, name, kind, parent_id FROM tags ORDER BY id", "id", "name", "kind", "parent_id"),
        "tag_links" to table(
            "SELECT id, tag_id, workout_id, we_id, severity FROM tag_links ORDER BY id",
            "id", "tag_id", "workout_id", "we_id", "severity",
        ),
        "settings" to getSettings(),
    )
    return Json.write(data)
}

class Restored(val workouts: Int, val exercises: Int)

// Checks the whole file first. Nothing is written unless every row passes.
fun Store.importAll(text: String): Restored {
    val d = parseBackup(text)
    q.tx {
        for (t in listOf(
            "tag_links", "sets", "workout_exercises", "workouts", "tags", "exercise_locations", "exercise_body_parts",
            "exercise_ex_tags", "ex_tags", "exercises", "locations", "workout_types",
        )) {
            // Fixed table names from this list only.
            q.exec("DELETE FROM $t")
        }
        for (l in d.locations) q.exec("INSERT INTO locations (id, name, icon, color) VALUES (?, ?, ?, ?)", l.id, l.name, l.icon, l.color)
        for (t in d.types) q.exec("INSERT INTO workout_types (id, name, icon) VALUES (?, ?, ?)", t.id, t.name, t.icon)
        for (e in d.exercises) {
            q.exec(
                "INSERT INTO exercises (id, name, icon, equipment, movement, cues, archived, link) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                e.id, e.name, e.icon, e.equipment, e.movement, e.cues, e.archived, e.link,
            )
        }
        for (t in d.exTags) q.exec("INSERT INTO ex_tags (id, name) VALUES (?, ?)", t.id, t.name)
        for (r in d.parts) q.exec("INSERT INTO exercise_body_parts (exercise_id, part) VALUES (?, ?)", r.exerciseId, r.part)
        for (r in d.exExTags) q.exec("INSERT INTO exercise_ex_tags (exercise_id, tag_id) VALUES (?, ?)", r.exerciseId, r.tagId)
        for (x in d.exLocs) q.exec("INSERT INTO exercise_locations (exercise_id, location_id, machine) VALUES (?, ?, ?)", x.exerciseId, x.locationId, x.machine)
        for (w in d.workouts) {
            q.exec(
                "INSERT INTO workouts (id, title, day, done, note, location_id, type_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                w.id, w.title, w.day, w.done, w.note, w.locationId, w.typeId,
            )
        }
        for (r in d.wes) {
            q.exec(
                "INSERT INTO workout_exercises (id, workout_id, exercise_id, position, status, note, location_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                r.id, r.workoutId, r.exerciseId, r.position, r.status, r.note, r.locationId,
            )
        }
        for (s in d.sets) q.exec("INSERT INTO sets (id, we_id, position, weight, reps, done) VALUES (?, ?, ?, ?, ?, ?)", s.id, s.weId, s.position, s.weight, s.reps, s.done)
        // groups first so every parent exists before its children
        for (t in tagInsertOrder(d.tags)) q.exec("INSERT INTO tags (id, name, kind, parent_id) VALUES (?, ?, ?, ?)", t.id, t.name, t.kind, t.parentId)
        for (l in d.links) q.exec("INSERT INTO tag_links (id, tag_id, workout_id, we_id, severity) VALUES (?, ?, ?, ?, ?)", l.id, l.tagId, l.workoutId, l.weId, l.severity)
        for ((k, v) in d.settings) q.exec("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)", k, v)
    }
    return Restored(d.workouts.size, d.exercises.size)
}
