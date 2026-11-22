package com.spotapp.personal

// The one place the screens go for data. All methods are plain (blocking) calls: AppState runs them on one
// background thread, so they never run on the main thread and never overlap.
// Every input is checked here again, whatever the screen already did.

class Store(val q: Q) {

    // ---------- settings ----------

    fun getSettings(): Map<String, String> {
        val out = LinkedHashMap(SETTING_DEFAULTS)
        q.map("SELECT key, value FROM settings") { it.str(0) to it.str(1) }.forEach { (k, v) ->
            if (validSettingValue(k, v)) out[k] = v
        }
        return out
    }

    fun setSetting(key: String, value: String) {
        cleanOneOf(key, "Setting", SETTING_KEYS)
        if (!validSettingValue(key, value)) throw InputError("Setting value is not a valid choice.")
        q.exec("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)", key, value)
    }

    // ---------- places and workout types ----------

    fun requireRow(table: String, id: Long?, label: String) {
        if (id == null) return
        // The table name is one of two fixed words chosen here, never typed by the person.
        val sql = when (table) {
            "locations" -> "SELECT id FROM locations WHERE id = ?"
            "workout_types" -> "SELECT id FROM workout_types WHERE id = ?"
            else -> throw IllegalArgumentException("Unknown table")
        }
        if (q.first(sql, id) { 1 } == null) throw InputError("$label no longer exists.")
    }

    fun listLocations(): List<Location> =
        q.map("SELECT id, name, icon, color FROM locations ORDER BY name COLLATE NOCASE") {
            Location(it.long(0), it.str(1), it.str(2), it.str(3))
        }

    fun saveLocation(id: Long?, name: String, icon: String, color: String): Long {
        val n = cleanText(name, "Location name", 1, Limits.LOCATION)
        val ic = cleanPlaceIcon(icon)
        val co = cleanPlaceColor(color)
        val dup = q.first("SELECT id FROM locations WHERE name = ? COLLATE NOCASE", n) { it.long(0) }
        if (dup != null && dup != id) throw InputError("A location with that name already exists.")
        if (id != null) {
            val i = cleanInt(id, "Location", 1, 1_000_000_000)
            q.exec("UPDATE locations SET name = ?, icon = ?, color = ? WHERE id = ?", n, ic, co, i)
            return i
        }
        return q.insert("INSERT INTO locations (name, icon, color) VALUES (?, ?, ?)", n, ic, co)
    }

    fun deleteLocation(id: Long) {
        q.exec("DELETE FROM locations WHERE id = ?", cleanInt(id, "Location", 1, 1_000_000_000))
    }

    fun listWorkoutTypes(): List<WorkoutType> =
        q.map("SELECT id, name, icon FROM workout_types ORDER BY name COLLATE NOCASE") { WorkoutType(it.long(0), it.str(1), it.str(2)) }

    fun saveWorkoutType(id: Long?, name: String, icon: String): Long {
        val n = cleanText(name, "Type name", 1, Limits.TYPE_NAME)
        val ic = cleanIcon(icon)
        val dup = q.first("SELECT id FROM workout_types WHERE name = ? COLLATE NOCASE", n) { it.long(0) }
        if (dup != null && dup != id) throw InputError("A workout type with that name already exists.")
        if (id != null) {
            val i = cleanInt(id, "Workout type", 1, 1_000_000_000)
            q.exec("UPDATE workout_types SET name = ?, icon = ? WHERE id = ?", n, ic, i)
            return i
        }
        return q.insert("INSERT INTO workout_types (name, icon) VALUES (?, ?)", n, ic)
    }

    fun deleteWorkoutType(id: Long) {
        q.exec("DELETE FROM workout_types WHERE id = ?", cleanInt(id, "Workout type", 1, 1_000_000_000))
    }

    // ---------- exercises ----------

    private class ExRow(
        val id: Long, val name: String, val icon: String, val equipment: String, val movement: String,
        val cues: String, val archived: Boolean, val link: String, val startMode: String, val tracks: String, val otherLabel: String,
    )

    private fun mapExercises(rows: List<ExRow>, locationId: Long?): List<Exercise> = mapExercisesAt(rows.map { it to locationId })

    // Each exercise comes with the place it is being used at (null when none). The shared tables are read once for the whole list.
    private fun mapExercisesAt(rows: List<Pair<ExRow, Long?>>): List<Exercise> {
        if (rows.isEmpty()) return emptyList()
        val s = getSettings()
        val unit = if (s["unit"] == "kg") "kg" else "lb"
        val step = s["step"]?.toDoubleOrNull()?.takeIf { it > 0 } ?: 5.0
        val mach = q.map(
            """SELECT x.exercise_id, x.location_id, l.name, x.machine FROM exercise_locations x
               JOIN locations l ON l.id = x.location_id ORDER BY l.name COLLATE NOCASE""",
        ) { Pair(it.long(0), ExMachine(it.long(1), it.str(2), it.str(3))) }
        val machByEx = mach.groupBy({ it.first }, { it.second })
        val parts = q.map("SELECT exercise_id, part FROM exercise_body_parts") { it.long(0) to it.str(1) }
        val partsByEx = parts.groupBy({ it.first }, { it.second })
        val tagRows = q.map("SELECT exercise_id, tag_id FROM exercise_ex_tags") { it.long(0) to it.long(1) }
        val tagsByEx = tagRows.groupBy({ it.first }, { it.second })
        return rows.map { (r, locationId) ->
            val mine = machByEx[r.id] ?: emptyList()
            val pick = if (locationId != null) mine.firstOrNull { it.locationId == locationId } else mine.firstOrNull()
            val mineParts = partsByEx[r.id] ?: emptyList()
            Exercise(
                id = r.id, name = r.name, icon = r.icon,
                equipment = if (r.equipment in EQUIPMENT) r.equipment else "",
                movement = if (r.movement in MOVEMENT) r.movement else "",
                machine = pick?.machine ?: "", gym = pick?.locationName ?: "", unit = unit, step = step,
                cues = if (r.cues.isEmpty()) emptyList() else r.cues.split("\n").filter { it.isNotEmpty() },
                archived = r.archived, machines = mine, link = r.link,
                bodyParts = BODY_PARTS.filter { b -> b in mineParts },
                exTagIds = tagsByEx[r.id] ?: emptyList(),
                tracks = exerciseTracks(r.equipment, r.tracks),
                startMode = if (r.startMode in START_MODES) r.startMode else "",
                otherLabel = r.otherLabel,
            )
        }
    }

    // One workout's exercises in one go: (exercise id, place) pairs in, the matching exercises out (null when an exercise is gone).
    fun exercisesAt(pairs: List<Pair<Long, Long?>>): List<Exercise?> {
        if (pairs.isEmpty()) return emptyList()
        val byId = q.map("SELECT $exCols FROM exercises") { exRow(it) }.associateBy { it.id }
        val found = pairs.mapNotNull { (id, loc) -> byId[id]?.let { it to loc } }
        val made = mapExercisesAt(found).iterator()
        return pairs.map { (id, _) -> if (byId.containsKey(id)) made.next() else null }
    }

    private val exCols = "id, name, icon, equipment, movement, cues, archived, link, start_mode, tracks, other_label"
    private fun exRow(c: android.database.Cursor) =
        ExRow(c.long(0), c.str(1), c.str(2), c.str(3), c.str(4), c.str(5), c.getInt(6) == 1, c.str(7), c.str(8), c.str(9), c.str(10))

    fun listExercises(includeArchived: Boolean = false): List<Exercise> {
        val rows = q.map(
            if (includeArchived) "SELECT $exCols FROM exercises ORDER BY archived, name COLLATE NOCASE"
            else "SELECT $exCols FROM exercises WHERE archived = 0 ORDER BY name COLLATE NOCASE",
        ) { exRow(it) }
        return mapExercises(rows, null)
    }

    fun getExercise(id: Long, locationId: Long? = null): Exercise? {
        val r = q.first("SELECT $exCols FROM exercises WHERE id = ?", id) { exRow(it) } ?: return null
        return mapExercises(listOf(r), locationId).firstOrNull()
    }

    class ExerciseInput(
        val id: Long?, val name: String, val icon: String, val equipment: String, val movement: String,
        val machines: List<Pair<Long, String>>, val cues: List<String>, val link: String, val bodyParts: List<String>, val exTagIds: List<Long>,
        val tracks: List<String> = emptyList(), val startMode: String = "", val otherLabel: String = "",
    )

    class CheckedExercise(
        val name: String, val icon: String, val equipment: String, val movement: String, val machines: List<Pair<Long, String>>,
        val cues: List<String>, val link: String, val bodyParts: List<String>, val exTagIds: List<Long>,
        val tracks: String, val startMode: String, val otherLabel: String,
    )

    fun checkExerciseInput(input: ExerciseInput): CheckedExercise {
        val name = cleanText(input.name, "Name", 1, Limits.NAME)
        val icon = cleanIcon(input.icon)
        val equipment = cleanOneOf(input.equipment, "Equipment", EQUIPMENT)
        val movement = if (equipment == "") "" else cleanOneOf(input.movement, "Push or pull", MOVEMENT)
        if (input.machines.size > 30) throw InputError("At most 30 locations on one exercise.")
        val seen = HashSet<Long>()
        val machines = (if (needsPlace(equipment)) input.machines else emptyList()).map { m ->
            val lid = cleanInt(m.first, "Location", 1, 1_000_000_000)
            if (!seen.add(lid)) throw InputError("A location appears twice.")
            lid to cleanOptionalText(m.second, "Machine", Limits.MACHINE)
        }
        val cues = input.cues.map { cleanOptionalText(it, "Cue", Limits.CUE) }.filter { it.isNotEmpty() }
        if (cues.size > Limits.CUES) throw InputError("At most ${Limits.CUES} cues.")
        val link = cleanLink(input.link)
        val bodyParts = input.bodyParts.map { cleanOneOf(it, "Body part", BODY_PARTS) }.distinct()
        val tagIds = input.exTagIds.map { cleanInt(it, "Tag", 1, 1_000_000_000) }.distinct()
        if (tagIds.size > Limits.EX_TAGS) throw InputError("At most ${Limits.EX_TAGS} tags on one exercise.")
        val picked = input.tracks.map { cleanOneOf(it, "What to write down", TRACKS) }.toSet()
        val tracks = if (hasTrackChoice(equipment)) TRACKS.filter { it in picked }.joinToString(",") else ""
        val startMode = cleanOneOf(input.startMode, "Starting sets", START_MODES)
        val otherLabel = cleanOptionalText(input.otherLabel, "Name for Other", Limits.OTHER_LABEL)
        return CheckedExercise(name, icon, equipment, movement, machines, cues, link, bodyParts, tagIds, tracks, startMode, otherLabel)
    }

    fun saveExercise(input: ExerciseInput): Long {
        val v = checkExerciseInput(input)
        val cues = v.cues.joinToString("\n")
        return q.tx {
            for (m in v.machines) requireRow("locations", m.first, "That location")
            val id: Long
            if (input.id != null) {
                id = cleanInt(input.id, "Exercise", 1, 1_000_000_000)
                q.exec(
                    "UPDATE exercises SET name = ?, icon = ?, equipment = ?, movement = ?, cues = ?, link = ?, start_mode = ?, tracks = ?, other_label = ? WHERE id = ?",
                    v.name, v.icon, v.equipment, v.movement, cues, v.link, v.startMode, v.tracks, v.otherLabel, id,
                )
            } else {
                id = q.insert(
                    "INSERT INTO exercises (name, icon, equipment, movement, cues, link, start_mode, tracks, other_label) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    v.name, v.icon, v.equipment, v.movement, cues, v.link, v.startMode, v.tracks, v.otherLabel,
                )
            }
            q.exec("DELETE FROM exercise_locations WHERE exercise_id = ?", id)
            for (m in v.machines) q.exec("INSERT INTO exercise_locations (exercise_id, location_id, machine) VALUES (?, ?, ?)", id, m.first, m.second)
            q.exec("DELETE FROM exercise_body_parts WHERE exercise_id = ?", id)
            for (b in v.bodyParts) q.exec("INSERT INTO exercise_body_parts (exercise_id, part) VALUES (?, ?)", id, b)
            q.exec("DELETE FROM exercise_ex_tags WHERE exercise_id = ?", id)
            for (t in v.exTagIds) {
                if (q.first("SELECT id FROM ex_tags WHERE id = ?", t) { 1 } == null) throw InputError("That tag no longer exists.")
                q.exec("INSERT INTO exercise_ex_tags (exercise_id, tag_id) VALUES (?, ?)", id, t)
            }
            id
        }
    }

    // Removes the exercise and everything logged for it (its sets, and its place in past workouts).
    fun deleteExercise(id: Long) {
        q.exec("DELETE FROM exercises WHERE id = ?", cleanInt(id, "Exercise", 1, 1_000_000_000))
    }

    fun setArchived(id: Long, archived: Boolean) {
        q.exec("UPDATE exercises SET archived = ? WHERE id = ?", archived, cleanInt(id, "Exercise", 1, 1_000_000_000))
    }

    // ---------- exercise tags (the person's own kinds, for example "Rehab" or "Warm-up") ----------

    fun listExTags(): List<ExTag> = q.map("SELECT id, name FROM ex_tags ORDER BY name COLLATE NOCASE") { ExTag(it.long(0), it.str(1)) }

    fun saveExTag(id: Long?, name: String): Long {
        val n = cleanText(name, "Tag name", 1, Limits.EX_TAG)
        val dup = q.first("SELECT id FROM ex_tags WHERE name = ? COLLATE NOCASE", n) { it.long(0) }
        if (dup != null && dup != id) throw InputError("A tag with that name already exists.")
        if (id != null) {
            val i = cleanInt(id, "Tag", 1, 1_000_000_000)
            q.exec("UPDATE ex_tags SET name = ? WHERE id = ?", n, i)
            return i
        }
        if (q.count("SELECT COUNT(*) FROM ex_tags") >= 100) throw InputError("At most 100 exercise tags.")
        return q.insert("INSERT INTO ex_tags (name) VALUES (?)", n)
    }

    fun deleteExTag(id: Long) {
        q.exec("DELETE FROM ex_tags WHERE id = ?", cleanInt(id, "Tag", 1, 1_000_000_000))
    }
}
