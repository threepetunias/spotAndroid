package com.spotapp.personal

// Types, limits and settings shared by the data layer and the screens. Matches the iPhone app so a backup moves between phones.

val EQUIPMENT = listOf("", "body_weight", "machine", "free_weights", "cable", "cardio", "other")
val EQUIPMENT_LABELS = mapOf(
    "" to "Not set", "body_weight" to "Body weight", "machine" to "Machine", "free_weights" to "Free weights",
    "cable" to "Cable", "cardio" to "Cardio machine", "other" to "Other",
)
// Equipment that is found in a particular place. Body weight and "not set" have no places.
fun needsPlace(e: String): Boolean = e != "" && e != "body_weight"

val MOVEMENT = listOf("", "push", "pull")
val MOVEMENT_LABELS = mapOf("" to "Not set", "push" to "Push", "pull" to "Pull")

val BODY_PARTS = listOf("arms", "shoulders", "back", "chest", "core", "legs", "glutes", "feet", "lungs")
val BODY_PART_LABELS = mapOf(
    "arms" to "Arms", "shoulders" to "Shoulders", "back" to "Back", "chest" to "Chest", "core" to "Core",
    "legs" to "Legs", "glutes" to "Glutes", "feet" to "Feet", "lungs" to "Lungs",
)
val BODY_PART_COLORS = mapOf(
    "arms" to "#C4694A", "shoulders" to "#C2A15A", "back" to "#4F8A7B", "chest" to "#B35C6B", "core" to "#7C6BA6",
    "legs" to "#4A7FA6", "glutes" to "#C98AA0", "feet" to "#8C7357", "lungs" to "#5FA3A0",
)
val PLACE_ICONS = listOf("gym", "home", "dance_studio", "park", "pool", "track", "climbing_wall", "yoga_studio", "trail", "court")
val PLACE_COLORS = listOf("#0F5E5C", "#C4694A", "#7C6BA6", "#4F8A7B", "#4A7FA6", "#C98AA0", "#C2A15A", "#A5544A")
val PLACE_ICON_LABELS = mapOf(
    "gym" to "Gym", "home" to "Home", "dance_studio" to "Dance studio", "park" to "Park", "pool" to "Pool", "track" to "Track",
    "climbing_wall" to "Climbing wall", "yoga_studio" to "Yoga studio", "trail" to "Trail", "court" to "Court or field",
)

data class ExMachine(val locationId: Long, val locationName: String, val machine: String)

data class Exercise(
    val id: Long, val name: String, val icon: String, val equipment: String, val movement: String,
    val machine: String, val gym: String, val unit: String, val step: Double, val cues: List<String>,
    val archived: Boolean, val machines: List<ExMachine>, val link: String, val bodyParts: List<String>, val exTagIds: List<Long>,
)
data class ExTag(val id: Long, val name: String)
data class Location(val id: Long, val name: String, val icon: String, val color: String)
data class WorkoutType(val id: Long, val name: String, val icon: String)
// A tag with no parent is a group (for example Health). Tags sit under a group and may have sub-tags (three levels at most).
data class Tag(val id: Long, val name: String, val kind: String, val parentId: Long?, val depth: Int)
data class TagLink(val tagId: Long, val name: String, val kind: String, val severity: Int?, val path: String)
data class SetLite(val weight: Double, val reps: Int)
data class SetRow(val id: Long, val position: Int, val weight: Double, val reps: Int, val done: Boolean)
data class WeFull(
    val id: Long, val position: Int, val status: String, val note: String, val exercise: Exercise,
    val sets: List<SetRow>, val tags: List<TagLink>, val locationId: Long?,
)
data class WorkoutFull(
    val id: Long, val title: String, val day: String, val done: Boolean, val note: String, val tags: List<TagLink>,
    val items: List<WeFull>, val locationId: Long?, val locationName: String, val typeId: Long?, val typeName: String, val icon: String,
)
data class WorkoutRow(
    val id: Long, val title: String, val day: String, val done: Boolean, val exCount: Int, val exDone: Int,
    val locationName: String, val typeName: String, val icon: String, val placeIcon: String, val placeColor: String,
)
data class Session(
    val weId: Long, val workoutId: Long, val workoutTitle: String, val day: String, val sets: List<SetLite>,
    val tags: List<TagLink>, val locationId: Long?, val locationName: String,
)
data class WeInfo(val id: Long, val workoutId: Long, val note: String, val exercise: Exercise)
data class LogFilter(
    val text: String = "", val tagIds: List<Long> = emptyList(), val from: String? = null, val to: String? = null,
    val locationId: Long? = null, val typeId: Long? = null,
)

object Limits {
    const val LINK = 300
    const val EX_TAG = 30
    const val EX_TAGS = 20
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
    const val LOCATION = 40
    const val TYPE_NAME = 30
    const val ICON = 40
    const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
}

val BP_COLOR_KEYS = listOf("bpArms", "bpShoulders", "bpBack", "bpChest", "bpCore", "bpLegs", "bpGlutes", "bpFeet", "bpLungs")
val BP_COLOR_KEY: Map<String, String> = BODY_PARTS.zip(BP_COLOR_KEYS).toMap()
val HOME_WIDGETS = listOf("today", "garden", "balance", "week", "fun")

// The saved card order as a list. Anything unknown, repeated or missing falls back to the default order,
// so Home and the Home cards settings page always agree.
fun readCardOrder(saved: String): List<String> {
    val parts = saved.split(",")
    val known = parts.filter { it in HOME_WIDGETS }
    val valid = known.size == HOME_WIDGETS.size && known.toSet().size == HOME_WIDGETS.size && parts.size == known.size
    return if (valid) known else HOME_WIDGETS
}

val SETTING_KEYS: List<String> = listOf(
    "theme", "palette", "voice", "onboarded", "tend", "unit", "step", "weightStep", "homePrs", "homePrsLook", "homePrsCount",
    "ppMachine", "ppFree", "ppCable", "ppBody", "ppCardio", "ppOther", "colorByBody",
) + BP_COLOR_KEYS + listOf(
    "recStyle", "recPeriod", "recColorBy", "widgetOrder", "wToday", "wGarden", "wBalance", "wWeek", "wFun",
    "sToday", "sGarden", "sBalance", "sWeek", "sFun", "pGarden", "pBalance", "pFun", "lockCard",
)

private val ON_OFF = listOf("on", "off")
private val PERIODS3 = listOf("week", "month", "year")

val SETTING_VALUES: Map<String, List<String>> = mapOf(
    "theme" to listOf("paper", "evening", "system"),
    "palette" to listOf("citrus", "lagoon", "moss", "sky", "orange", "terracotta", "lilac", "rose", "cherry", "gold"),
    "voice" to listOf("whimsical", "cheeky", "gentle", "callout"),
    "onboarded" to listOf("0", "1"),
    "tend" to listOf("0", "1"),
    "unit" to listOf("lb", "kg"),
    "weightStep" to listOf("0.5", "1", "2.5", "5", "10"),
    "step" to listOf("1", "2.5", "5", "10"), // no longer used by the app; kept so older backups still restore
    "homePrs" to listOf("off", "latest", "best", "all"),
    "homePrsLook" to listOf("list", "tiles"),
    "homePrsCount" to listOf("3", "5", "10", "all"),
    "ppMachine" to ON_OFF, "ppFree" to ON_OFF, "ppCable" to ON_OFF, "ppBody" to ON_OFF, "ppCardio" to ON_OFF, "ppOther" to ON_OFF,
    "colorByBody" to ON_OFF,
    "recStyle" to listOf("sky", "range", "skyline", "ghost", "list"),
    "recPeriod" to listOf("week", "month", "year", "all"),
    "recColorBy" to listOf("body", "place", "theme"),
    "wToday" to ON_OFF, "wGarden" to ON_OFF, "wBalance" to ON_OFF, "wWeek" to ON_OFF, "wFun" to ON_OFF,
    "sToday" to listOf("card", "button", "checklist"),
    "sGarden" to listOf("garden", "icons", "bars"),
    "sBalance" to listOf("seesaw", "split", "ring"),
    "sWeek" to listOf("strip", "grid", "streak"),
    "sFun" to listOf("comparison", "number", "pile"),
    "pGarden" to PERIODS3, "pBalance" to PERIODS3, "pFun" to PERIODS3,
    "lockCard" to ON_OFF,
)

private val HEX = Regex("^#[0-9A-Fa-f]{6}$")

fun validSettingValue(key: String, v: Any?): Boolean {
    if (v !is String || key !in SETTING_KEYS) return false
    if (key in BP_COLOR_KEYS) return HEX.matches(v)
    if (key == "widgetOrder") {
        val parts = v.split(",")
        return parts.size == HOME_WIDGETS.size && parts.toSet().size == parts.size && parts.all { it in HOME_WIDGETS }
    }
    return SETTING_VALUES[key]?.contains(v) == true
}

val SETTING_DEFAULTS: Map<String, String> = mapOf(
    "theme" to "system", "palette" to "lagoon", "voice" to "gentle", "onboarded" to "0", "tend" to "0", "unit" to "lb",
    "step" to "5", "weightStep" to "1", "homePrs" to "latest", "homePrsLook" to "list", "homePrsCount" to "5",
    "ppMachine" to "on", "ppFree" to "on", "ppCable" to "on", "ppBody" to "on", "ppCardio" to "off", "ppOther" to "off",
    "colorByBody" to "on",
    "bpArms" to BODY_PART_COLORS.getValue("arms"), "bpShoulders" to BODY_PART_COLORS.getValue("shoulders"),
    "bpBack" to BODY_PART_COLORS.getValue("back"), "bpChest" to BODY_PART_COLORS.getValue("chest"),
    "bpCore" to BODY_PART_COLORS.getValue("core"), "bpLegs" to BODY_PART_COLORS.getValue("legs"),
    "bpGlutes" to BODY_PART_COLORS.getValue("glutes"), "bpFeet" to BODY_PART_COLORS.getValue("feet"),
    "bpLungs" to BODY_PART_COLORS.getValue("lungs"),
    "recStyle" to "sky", "recPeriod" to "month", "recColorBy" to "body",
    "widgetOrder" to "today,garden,balance,week,fun", "wToday" to "on", "wGarden" to "on", "wBalance" to "on", "wWeek" to "on", "wFun" to "off",
    "sToday" to "button", "sGarden" to "garden", "sBalance" to "seesaw", "sWeek" to "strip", "sFun" to "comparison",
    "pGarden" to "week", "pBalance" to "month", "pFun" to "month",
    "lockCard" to "on",
)

// Which setting turns push/pull on for each kind of equipment.
val PUSH_PULL_KEY: Map<String, String> = mapOf(
    "machine" to "ppMachine", "free_weights" to "ppFree", "cable" to "ppCable",
    "body_weight" to "ppBody", "cardio" to "ppCardio", "other" to "ppOther",
)

fun pushPullOn(s: Map<String, String>, equipment: String): Boolean {
    val k = PUSH_PULL_KEY[equipment] ?: return false
    return s[k] == "on"
}

// A starting picture for a place, chosen from words in its name. Falls back to the gym picture.
fun guessPlaceIcon(name: String): String {
    val n = name.lowercase()
    val table = listOf(
        Regex("home|house|apartment|garage|basement") to "home", Regex("dance|ballet|studio") to "dance_studio",
        Regex("yoga|pilates") to "yoga_studio", Regex("pool|swim|aquatic") to "pool", Regex("climb|boulder") to "climbing_wall",
        Regex("track") to "track", Regex("trail|hike|mountain") to "trail", Regex("park|outdoor|outside|beach") to "park",
        Regex("court|field|tennis|basketball|soccer") to "court",
    )
    for ((re, icon) in table) if (re.containsMatchIn(n)) return icon
    return "gym"
}
