package com.spotapp.personal

// Reads and checks a backup file. Nothing here touches the database: the whole file is checked first,
// and the store writes it only when every row passes. Bad input is rejected, never repaired.
// Backup versions 1 to 5 are read (the same files the iPhone app makes), version 5 is written.

const val BACKUP_VERSION = 5

class BLocation(val id: Long, val name: String, val icon: String, val color: String)
class BType(val id: Long, val name: String, val icon: String)
class BExercise(
    val id: Long, val name: String, val icon: String, var equipment: String, val movement: String,
    val cues: String, val archived: Int, val link: String,
)
class BExLoc(val exerciseId: Long, val locationId: Long, val machine: String)
class BWorkout(
    val id: Long, val title: String, val day: String, val done: Int, val note: String, val locationId: Long?, val typeId: Long?,
)
class BWe(
    val id: Long, val workoutId: Long, val exerciseId: Long, val position: Int, val status: String, val note: String, val locationId: Long?,
)
class BSet(val id: Long, val weId: Long, val position: Int, val weight: Double, val reps: Int, val done: Int)
class BTag(val id: Long, val name: String, val kind: String, var parentId: Long?)
class BLink(val id: Long, val tagId: Long, val workoutId: Long?, val weId: Long?, val severity: Int?)
class BPart(val exerciseId: Long, val part: String)
class BExTag(val id: Long, val name: String)
class BExExTag(val exerciseId: Long, val tagId: Long)

class BackupData(
    val locations: List<BLocation>, val types: List<BType>, val exercises: List<BExercise>, val exLocs: List<BExLoc>,
    val workouts: List<BWorkout>, val wes: List<BWe>, val sets: List<BSet>, val tags: List<BTag>, val links: List<BLink>,
    val parts: List<BPart>, val exTags: List<BExTag>, val exExTags: List<BExExTag>, val settings: Map<String, String>,
)

@Suppress("UNCHECKED_CAST")
private fun rows(v: Any?, label: String, max: Int): List<Map<String, Any?>> {
    if (v !is List<*>) throw InputError("Backup is missing $label.")
    if (v.size > max) throw InputError("Backup has too many $label.")
    for (r in v) if (r !is Map<*, *>) throw InputError("Backup $label are not valid.")
    return v as List<Map<String, Any?>>
}

private fun id(v: Any?, l: String): Long = cleanInt(v, l, 1, 1_000_000_000)
private fun optId(v: Any?, l: String): Long? = if (v == null) null else id(v, l)
private fun bad(): Nothing = throw InputError("Backup links do not match.")

fun parseBackup(text: String): BackupData {
    if (text.isEmpty()) throw InputError("The backup file is empty.")
    if (text.length > Limits.MAX_BACKUP_BYTES) throw InputError("The backup file is too large.")
    val raw: Any? = try { Json.parse(text) } catch (e: JsonError) { throw InputError("This is not a Spot backup file.") }
    if (raw !is Map<*, *>) throw InputError("This is not a Spot backup file.")
    @Suppress("UNCHECKED_CAST")
    val o = raw as Map<String, Any?>
    if (o["app"] != "spot") throw InputError("This is not a Spot backup file.")
    val version = (o["version"] as? Long)?.toInt()
    if (version == null || version < 1 || version > 5) throw InputError("This backup was made by a different version of Spot.")
    val old = version == 1
    val pre3 = version <= 2
    val pre4 = version < 4
    val pre5 = version < 5

    val ex = rows(o["exercises"], "exercises", 5000).map { r ->
        val gym = if (old) cleanOptionalText(r["gym"], "Gym", Limits.GYM) else ""
        val machine = if (old) cleanOptionalText(r["machine"], "Machine", Limits.MACHINE) else ""
        Triple(
            BExercise(
                id = id(r["id"], "Exercise id"),
                name = cleanText(r["name"], "Name", 1, Limits.NAME),
                icon = if (old) "" else cleanIcon(r["icon"]),
                equipment = if (pre3) "" else cleanOneOf(r["equipment"], "Equipment", EQUIPMENT),
                movement = if (pre4) "" else cleanOneOf(r["movement"], "Push or pull", MOVEMENT),
                cues = cleanOptionalText(r["cues"], "Cues", Limits.CUE * Limits.CUES + Limits.CUES, true),
                archived = cleanInt(r["archived"], "Archived", 0, 1).toInt(),
                link = if (pre5) "" else (r["link"] as? String)?.let { cleanLink(it) } ?: throw InputError("Link must be text."),
            ),
            gym, machine,
        )
    }
    val exercises = ex.map { it.first }

    var loc: List<BLocation> = emptyList()
    var types: List<BType> = emptyList()
    var exLocs: List<BExLoc> = emptyList()
    if (old) {
        // version 1 kept the machine and gym on the exercise itself
        val byName = HashMap<String, Long>()
        val l = ArrayList<BLocation>()
        val x = ArrayList<BExLoc>()
        for ((e, gym, machine) in ex) {
            if (gym == "") continue
            val key = gym.lowercase()
            if (!byName.containsKey(key)) {
                val nid = (l.size + 1).toLong()
                byName[key] = nid
                l.add(BLocation(nid, gym, guessPlaceIcon(gym), ""))
            }
            x.add(BExLoc(e.id, byName.getValue(key), machine))
        }
        loc = l
        exLocs = x
    } else {
        loc = rows(o["locations"], "locations", 2000).map { r ->
            BLocation(
                id(r["id"], "Location id"), cleanText(r["name"], "Location name", 1, Limits.LOCATION),
                if (pre5) guessPlaceIcon(r["name"].toString()) else cleanPlaceIcon(r["icon"]),
                if (pre5) "" else cleanPlaceColor(r["color"]),
            )
        }
        types = rows(o["workout_types"], "workout types", 2000).map { r ->
            BType(id(r["id"], "Type id"), cleanText(r["name"], "Type name", 1, Limits.TYPE_NAME), cleanIcon(r["icon"]))
        }
        exLocs = rows(o["exercise_locations"], "exercise locations", 100000).map { r ->
            BExLoc(id(r["exercise_id"], "Exercise"), id(r["location_id"], "Location"), cleanOptionalText(r["machine"], "Machine", Limits.MACHINE))
        }
    }
    for (e in exercises) if (e.equipment == "" && e.movement != "") throw InputError("An exercise has push or pull set without equipment.")
    if (pre3) {
        val withPlace = exLocs.map { it.exerciseId }.toSet()
        for (e in exercises) if (e.id in withPlace) e.equipment = "machine"
    }
    val wk = rows(o["workouts"], "workouts", 20000).map { r ->
        BWorkout(
            id(r["id"], "Workout id"), cleanText(r["title"], "Title", 1, Limits.TITLE), cleanDay(r["day"]),
            cleanInt(r["done"], "Done", 0, 1).toInt(), cleanOptionalText(r["note"], "Note", Limits.NOTE, true),
            if (old) null else optId(r["location_id"], "Location"), if (old) null else optId(r["type_id"], "Workout type"),
        )
    }
    val we = rows(o["workout_exercises"], "workout exercises", 200000).map { r ->
        BWe(
            id(r["id"], "Item id"), id(r["workout_id"], "Workout"), id(r["exercise_id"], "Exercise"),
            cleanInt(r["position"], "Position", 0, 100000).toInt(), cleanOneOf(r["status"], "Status", listOf("todo", "done", "skipped")),
            cleanOptionalText(r["note"], "Note", Limits.NOTE, true), if (pre3) null else optId(r["location_id"], "Location"),
        )
    }
    val st = rows(o["sets"], "sets", 1000000).map { r ->
        BSet(
            id(r["id"], "Set id"), id(r["we_id"], "Item"), cleanInt(r["position"], "Position", 0, 100000).toInt(),
            cleanNum(r["weight"], "Weight", 0.0, Limits.MAX_WEIGHT), cleanInt(r["reps"], "Reps", 0, Limits.MAX_REPS).toInt(),
            cleanInt(r["done"], "Done", 0, 1).toInt(),
        )
    }
    val ebp = if (pre5) emptyList() else rows(o["exercise_body_parts"], "body parts", 100000).map { r ->
        BPart(id(r["exercise_id"], "Exercise"), cleanOneOf(r["part"], "Body part", BODY_PARTS))
    }
    val ext = if (pre5) emptyList() else rows(o["ex_tags"], "exercise tags", 200).map { r ->
        BExTag(id(r["id"], "Tag id"), cleanText(r["name"], "Tag name", 1, Limits.EX_TAG))
    }
    val exl = if (pre5) emptyList() else rows(o["exercise_ex_tags"], "exercise tag links", 100000).map { r ->
        BExExTag(id(r["exercise_id"], "Exercise"), id(r["tag_id"], "Tag"))
    }
    val tg = ArrayList(rows(o["tags"], "tags", 2000).map { r ->
        BTag(
            id(r["id"], "Tag id"), cleanText(r["name"], "Tag name", 1, Limits.TAG),
            cleanOneOf(r["kind"], "Tag type", listOf("health", "general")), if (pre3) null else optId(r["parent_id"], "Group"),
        )
    })
    if (pre3) {
        // older files had two kinds and no groups: they become the groups Health and Notes
        val used = tg.map { it.name.lowercase() }.toMutableSet()
        fun unique(base: String): String {
            var n = base
            var i = 2
            while (n.lowercase() in used) n = "$base ${i++}"
            used.add(n.lowercase())
            return n
        }
        val top = tg.maxOfOrNull { it.id } ?: 0L
        val hid = top + 1
        val nid = top + 2
        for (t in tg) t.parentId = if (t.kind == "health") hid else nid
        tg.add(BTag(hid, unique("Health"), "health", null))
        tg.add(BTag(nid, unique("Notes"), "general", null))
    }
    val tl = rows(o["tag_links"], "tag links", 500000).map { r ->
        val workoutId = optId(r["workout_id"], "Workout")
        val weId = optId(r["we_id"], "Item")
        if ((workoutId == null) == (weId == null)) throw InputError("Backup tag links are not valid.")
        BLink(
            id(r["id"], "Link id"), id(r["tag_id"], "Tag"), workoutId, weId,
            r["severity"]?.let { cleanInt(it, "Severity", 1, 5).toInt() },
        )
    }

    val exIds = exercises.map { it.id }.toSet()
    val wkIds = wk.map { it.id }.toSet()
    val weIds = we.map { it.id }.toSet()
    val tgIds = tg.map { it.id }.toSet()
    if (exIds.size != exercises.size || wkIds.size != wk.size || weIds.size != we.size || tgIds.size != tg.size) {
        throw InputError("Backup has repeated ids.")
    }
    val locIds = loc.map { it.id }.toSet()
    val typeIds = types.map { it.id }.toSet()
    if (locIds.size != loc.size || typeIds.size != types.size) throw InputError("Backup has repeated ids.")
    if (loc.map { it.name.lowercase() }.toSet().size != loc.size) throw InputError("Backup has repeated location names.")
    if (types.map { it.name.lowercase() }.toSet().size != types.size) throw InputError("Backup has repeated workout type names.")
    val pairs = HashSet<String>()
    for (r in exLocs) {
        val key = "${r.exerciseId}:${r.locationId}"
        if (key in pairs || r.exerciseId !in exIds || r.locationId !in locIds) throw InputError("Backup links do not match.")
        pairs.add(key)
    }
    for (r in wk) {
        if (r.locationId != null && r.locationId !in locIds) bad()
        if (r.typeId != null && r.typeId !in typeIds) bad()
    }
    for (r in we) if (r.workoutId !in wkIds || r.exerciseId !in exIds) bad()
    for (r in st) if (r.weId !in weIds) bad()
    for (r in tl) {
        if (r.tagId !in tgIds) bad()
        if (r.workoutId != null && r.workoutId !in wkIds) bad()
        if (r.weId != null && r.weId !in weIds) bad()
    }
    val tagById = tg.associateBy { it.id }
    for (t in tg) {
        var depth = 1
        var p = t.parentId
        while (p != null) {
            val x = tagById[p]
            if (x == null || depth > 3) throw InputError("Backup tags are not valid.")
            if (x.kind != t.kind) throw InputError("Backup tags are not valid.")
            depth += 1
            p = x.parentId
        }
        if (depth > 3) throw InputError("Backup tags are not valid.")
    }
    for (r in tl) {
        val t = tagById[r.tagId]
        if (t != null && t.parentId == null) throw InputError("Backup tags are not valid.")
    }
    for (r in we) if (r.locationId != null && r.locationId !in locIds) bad()
    val extIds = ext.map { it.id }.toSet()
    if (extIds.size != ext.size || ext.map { it.name.lowercase() }.toSet().size != ext.size) throw InputError("Backup has repeated exercise tags.")
    val bpPairs = HashSet<String>()
    for (r in ebp) {
        val key = "${r.exerciseId}:${r.part}"
        if (key in bpPairs || r.exerciseId !in exIds) bad()
        bpPairs.add(key)
    }
    val etPairs = HashSet<String>()
    for (r in exl) {
        val key = "${r.exerciseId}:${r.tagId}"
        if (key in etPairs || r.exerciseId !in exIds || r.tagId !in extIds) bad()
        etPairs.add(key)
    }
    if (tg.map { it.name.lowercase() }.toSet().size != tg.size) throw InputError("Backup has repeated tag names.")

    val settings = LinkedHashMap<String, String>()
    val s = o["settings"]
    if (s is Map<*, *>) {
        for (k in SETTING_KEYS) {
            if (k == "onboarded") continue
            val v = s[k]
            if (validSettingValue(k, v)) settings[k] = v as String
        }
    }
    return BackupData(loc, types, exercises, exLocs, wk, we, st, tg, tl, ebp, ext, exl, settings)
}

fun cleanIcon(v: Any?): String {
    val t = cleanOptionalText(v, "Icon", Limits.ICON)
    if (!Regex("^[a-z0-9_]*$").matches(t)) throw InputError("Icon is not valid.")
    return t
}
fun cleanPlaceIcon(v: Any?): String = if (v == "" || v == null) "" else cleanOneOf(v, "Place picture", PLACE_ICONS)
fun cleanPlaceColor(v: Any?): String = if (v == "" || v == null) "" else cleanOneOf(v, "Place color", PLACE_COLORS)

// The order tags must be inserted in so every parent exists before its children.
fun tagInsertOrder(tags: List<BTag>): List<BTag> {
    val byId = tags.associateBy { it.id }
    fun depthOf(t: BTag): Int {
        var d = 1
        var p = t.parentId
        while (p != null) { d += 1; p = byId.getValue(p).parentId }
        return d
    }
    return tags.sortedBy { depthOf(it) }
}
