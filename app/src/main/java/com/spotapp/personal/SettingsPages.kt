package com.spotapp.personal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Settings pages: look, voice, workouts, places, workout types, body parts, exercise tags, push and pull.
// Every page has the Settings home button. Every setting here also has a Help line and a Settings search entry.

@Composable
internal fun SPage(
    title: String, bottom: (@Composable () -> Unit)? = null, add: Pair<String, () -> Unit>? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val app = LocalApp.current
    app.inSettings = true
    ScreenFrame(
        title, bottom = bottom,
        floating = {
            if (add != null) FloatingAdd(add.first, add.second, raised = true)
            SettingsHome(raised = bottom != null)
        },
        content = content,
    )
}

@Composable
internal fun Intro(text: String) {
    Text(text, color = colors().muted, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.padding(top = 16.dp))
}

@Composable
internal fun OnOff(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Caption(label)
        ChipRows {
            Chip("On", on, { onChange(true) })
            Chip("Off", !on, { onChange(false) })
        }
    }
}

// A row of round color choices. The chosen one has a check.
@Composable
fun Swatches(options: List<String>, value: String, label: String, onPick: (String) -> Unit) {
    val c = colors()
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (hex in options) {
            val on = hex.equals(value, ignoreCase = true)
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(hexColor(hex))
                    .border(if (on) 3.dp else 1.dp, if (on) c.ink else c.line, CircleShape)
                    .clickable(role = Role.RadioButton) { onPick(hex) }
                    .semantics { selected = on; contentDescription = "$label $hex" },
                contentAlignment = Alignment.Center,
            ) { if (on) SpotIcon("check", Color.White, 22.dp, hole = hexColor(hex)) }
        }
    }
}

// ---------- Theme and color ----------

@Composable
fun LookPage() {
    val app = LocalApp.current
    val c = colors()
    val s = app.settings
    SPage("Theme and color") {
        Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Theme")
            ChipRows {
                Chip("Paper", s["theme"] == "paper", { app.setSetting("theme", "paper") })
                Chip("Evening", s["theme"] == "evening", { app.setSetting("theme", "evening") })
                Chip("Match phone", s["theme"] == "system", { app.setSetting("theme", "system") })
            }
        }
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Color")
            ChipRows { for (p in PALETTES) Chip(p.name, s["palette"] == p.key, { app.setSetting("palette", p.key) }) }
        }
        Column(Modifier.padding(top = 28.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.card).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Caption("Preview")
            Text("60 lb × 8", color = c.ink, fontFamily = Serif, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(c.nowFill), contentAlignment = Alignment.Center) {
                    Text("3", color = c.onNowFill, fontWeight = FontWeight.Bold)
                }
                Text("Now", color = c.accentText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WorkoutPicture("barbell", 44.dp)
                WorkoutPicture("yoga", 44.dp)
                WorkoutPicture("swimming", 44.dp)
            }
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.cue).padding(12.dp)) {
                Text("Chest up · Pull to collarbone", color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------- Voice ----------

@Composable
fun VoicePage() {
    val app = LocalApp.current
    val c = colors()
    SPage("Voice") {
        Intro("Only the words change. Counts and behavior stay the same.")
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (v in VOICES) {
                val on = app.settings["voice"] == v.key
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.card)
                        .border(BorderStroke(2.dp, if (on) c.nowEdge else c.card), RoundedCornerShape(18.dp))
                        .clickable(role = Role.RadioButton) { app.setSetting("voice", v.key) }
                        .semantics { selected = on; contentDescription = "${v.name}. ${v.blurb}. Sample: ${sampleLine(v.key)}" }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(v.name, Modifier.weight(1f), color = c.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        if (on) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            SpotIcon("check", c.accentText, 18.dp)
                            Text("Chosen", color = c.accentText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(v.blurb, color = c.muted, fontSize = 15.sp)
                    Text("“${sampleLine(v.key)}”", color = c.ink, fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

// ---------- Units and buttons ----------

@Composable
fun WorkoutsPage() {
    val app = LocalApp.current
    val c = colors()
    val unit = app.settings["unit"] ?: "lb"
    SPage("Units and buttons") {
        Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Units")
            ChipRows {
                Chip("lb", unit == "lb", { app.setSetting("unit", "lb") })
                Chip("kg", unit == "kg", { app.setSetting("unit", "kg") })
            }
            Text("Used for every exercise. Numbers you already logged stay as they are; they are not converted.", color = c.muted, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Weight buttons")
            ChipRows { for (v in listOf("0.5", "1", "2.5", "5", "10")) Chip("$v $unit", app.settings["weightStep"] == v, { app.setSetting("weightStep", v) }) }
            Text("How far the plus and minus buttons move the weight on a set. You can still type any number.", color = c.muted, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

// ---------- Places ----------

private val SUGGESTED_PLACES = listOf("Gym", "Home", "Outdoors")

@Composable
fun LocationsPage() {
    val app = LocalApp.current
    val c = colors()
    val st = load { listLocations() }
    var editing by remember { mutableStateOf<Location?>(null) }
    var isNew by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("gym") }
    var color by remember { mutableStateOf(PLACE_COLORS[0]) }
    val rows = (st as? Load.Ready)?.value ?: emptyList()

    fun open(x: Location?) {
        isNew = x == null
        editing = x ?: Location(0, "", "gym", "")
        name = x?.name ?: ""
        icon = x?.icon ?: "gym"
        color = x?.color?.ifEmpty { PLACE_COLORS[0] } ?: PLACE_COLORS[rows.size % PLACE_COLORS.size]
    }
    fun close() { editing = null; name = "" }

    SPage("Places", add = "Add a place" to { open(null) }) {
        Intro("Where you work out. Give each place a picture and a color. They show on workouts, in the Log and on records. You can also add a place right from a workout with the New place chip.")
        Loaded(st) { list ->
            val missing = SUGGESTED_PLACES.filter { s -> list.none { it.name.equals(s, ignoreCase = true) } }
            if (missing.isNotEmpty()) Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption("Quick add")
                ChipRows {
                    for (m in missing) Chip(m, false, { app.write({ saveLocation(null, m, guessPlaceIcon(m), PLACE_COLORS[list.size % PLACE_COLORS.size]) }) })
                }
            }
            Column(Modifier.padding(top = 18.dp)) {
                if (list.isEmpty()) Text("No places yet.", color = c.ink, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp))
                list.forEachIndexed { i, r ->
                    if (i > 0) Divider()
                    ListRow(r.name, onClick = { open(r) }, right = { PlaceBadge(r.icon, r.color, 40.dp) })
                }
            }
        }
    }
    val e = editing
    if (e != null) SpotSheet(if (isNew) "New place" else "Place", onClose = { close() }) {
        Field("Name", name, { name = it }, max = Limits.LOCATION, placeholder = "Riverside Gym")
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Picture")
            TileRows(PLACE_ICONS) { p, m -> PictureTile(PLACE_ICON_LABELS[p] ?: p, icon == p, { icon = p }, modifier = m) { PlaceBadge(p, color, 40.dp) } }
            Caption("Color")
            Swatches(PLACE_COLORS, color, "Color") { color = it }
        }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigBtn("Cancel", { close() }, Modifier.weight(1f), secondary = true)
            BigBtn("Save", {
                val id = if (isNew) null else e.id
                app.write({ saveLocation(id, name, icon, color) }, then = { close() })
            }, Modifier.weight(1f), enabled = name.trim().isNotEmpty())
        }
        if (!isNew) Row(Modifier.padding(top = 12.dp)) {
            BigBtn("Delete", {
                app.ask("Delete this place?", "Workouts keep their name. They just lose the place. Machines set up here are removed.", "Delete", true) {
                    app.write({ deleteLocation(e.id) }, then = { close() })
                }
            }, Modifier.weight(1f), secondary = true)
        }
    }
}

// ---------- Workout types ----------

private class Suggested(val name: String, val icon: String)
private val SUGGESTED_TYPES = listOf(
    Suggested("Strength", "barbell"), Suggested("Cardio", "run"), Suggested("Yoga", "yoga"),
    Suggested("Pilates", "pilates"), Suggested("Walking", "walk"), Suggested("Swimming", "swimming"),
)

@Composable
fun WorkoutTypesPage() {
    val app = LocalApp.current
    val c = colors()
    val st = load { listWorkoutTypes() }
    SPage("Workout types", add = "Add a workout type" to { app.go(Route.Page("workout-type", null)) }) {
        Intro("Kinds of workout, such as strength or yoga. Pick one when you start a workout. The picture shows next to the workout in your list.")
        Loaded(st) { list ->
            val missing = SUGGESTED_TYPES.filter { s -> list.none { it.name.equals(s.name, ignoreCase = true) } }
            if (missing.isNotEmpty()) Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption("Quick add")
                ChipRows { for (m in missing) Chip(m.name, false, { app.write({ saveWorkoutType(null, m.name, m.icon) }) }) }
            }
            Column(Modifier.padding(top = 18.dp)) {
                if (list.isEmpty()) Text("No types yet.", color = c.ink, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp))
                list.forEachIndexed { i, r ->
                    if (i > 0) Divider()
                    ListRow(r.name, onClick = { app.go(Route.Page("workout-type", r.id)) }, right = { if (r.icon.isNotEmpty()) WorkoutPicture(r.icon, 40.dp) })
                }
            }
        }
    }
}

@Composable
fun WorkoutTypePage(arg: Long?) {
    val app = LocalApp.current
    val st = load(arg) { if (arg == null) null else listWorkoutTypes().firstOrNull { it.id == arg } }
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(arg == null) }
    LaunchedEffect(st) {
        if (!loaded && st is Load.Ready) {
            val t = st.value
            if (t != null) { name = t.name; icon = t.icon }
            loaded = true
        }
    }
    SPage(
        if (arg == null) "New type" else "Workout type",
        bottom = {
            PinnedBar {
                BigBtn("Cancel", { app.back() }, Modifier.weight(1f), secondary = true)
                BigBtn("Save", { app.write({ saveWorkoutType(arg, name, icon) }, then = { app.back() }) }, Modifier.weight(1f), enabled = loaded && name.trim().isNotEmpty())
            }
        },
    ) {
        if (loaded) {
            Field("Name", name, { name = it }, Modifier.padding(top = 16.dp), max = Limits.TYPE_NAME, placeholder = "Strength")
            IconPickerField("Picture", icon) { icon = it }
            if (arg != null) Row(Modifier.padding(top = 28.dp)) {
                BigBtn("Delete", {
                    app.ask("Delete this type?", "Workouts keep their name and history. They just lose the type.", "Delete", true) {
                        app.write({ deleteWorkoutType(arg) }, then = { app.back() })
                    }
                }, Modifier.weight(1f), secondary = true)
            }
        }
    }
}

// ---------- Body parts ----------

@Composable
fun BodyPartsPage() {
    val app = LocalApp.current
    val c = colors()
    val s = app.settings
    var open by remember { mutableStateOf<String?>(null) }
    val on = s["colorByBody"] == "on"
    fun keyOf(part: String) = BP_COLOR_KEY.getValue(part)
    fun colorOf(part: String) = s[keyOf(part)] ?: BODY_PART_COLORS.getValue(part)
    SPage("Body parts") {
        Intro("Each body part has a color. With \"Color pictures by body part\" on, an exercise picture takes the color of the first body part you chose for it.")
        OnOff("Color pictures by body part", on) { app.setSetting("colorByBody", if (it) "on" else "off") }
        Column(Modifier.padding(top = 20.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card).padding(horizontal = 14.dp)) {
            BODY_PARTS.forEachIndexed { i, b ->
                if (i > 0) Divider()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button) { open = b }
                        .semantics { contentDescription = "${BODY_PART_LABELS[b]} color. Tap to change." },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BodyBadge(b, 36.dp)
                    Text(BODY_PART_LABELS[b] ?: b, Modifier.weight(1f), color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.size(28.dp).clip(CircleShape).background(hexColor(colorOf(b))))
                }
            }
        }
        Row(Modifier.padding(top = 20.dp)) {
            BigBtn("Reset all colors", {
                app.write({ for (p in BODY_PARTS) setSetting(BP_COLOR_KEY.getValue(p), BODY_PART_COLORS.getValue(p)) }, then = { app.reloadSettings() })
            }, Modifier.weight(1f), secondary = true)
        }
    }
    val o = open
    if (o != null) {
        val cur = colorOf(o)
        val base = BODY_PART_COLORS.values.toList()
        val options = if (base.any { it.equals(cur, ignoreCase = true) }) base else base + cur.uppercase()
        PickerBox(BODY_PART_LABELS[o] ?: o, onClose = { open = null }) {
            Swatches(options, cur, BODY_PART_LABELS[o] ?: o) { hex -> app.setSetting(keyOf(o), hex) }
        }
    }
}

// ---------- Exercise tags ----------

@Composable
fun ExerciseTagsPage() {
    val app = LocalApp.current
    val c = colors()
    val st = load { listExTags() }
    var dialog by remember { mutableStateOf<ExTag?>(null) }
    var isNew by remember { mutableStateOf(false) }
    SPage("Exercise tags", add = "Add a tag" to { isNew = true; dialog = ExTag(0, "") }) {
        Intro("Make your own labels for exercises. Tap a tag to rename it.")
        Loaded(st) { list ->
            Column(Modifier.padding(top = 14.dp)) {
                if (list.isEmpty()) Text("No tags yet.", color = c.ink, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp))
                list.forEachIndexed { i, t ->
                    if (i > 0) Divider()
                    ListRow(t.name, onClick = { isNew = false; dialog = t }, right = {
                        Box(
                            Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) {
                                app.ask("Delete this tag?", "Exercises keep everything else. They just lose this tag.", "Delete", true) { app.write({ deleteExTag(t.id) }) }
                            }.semantics { contentDescription = "Delete ${t.name}" },
                            contentAlignment = Alignment.Center,
                        ) { Text("Delete", color = c.accentText, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp)) }
                    })
                }
            }
        }
    }
    val d = dialog
    if (d != null) PromptDialog(
        if (isNew) "New tag" else "Rename tag", onCancel = { dialog = null },
        onSubmit = { text -> app.write({ saveExTag(if (isNew) null else d.id, text) }, then = { dialog = null }) },
        confirmLabel = "Save", initial = d.name, maxLength = Limits.EX_TAG, placeholder = "Warm-up",
    )
}


// ---------- Push and pull ----------

private val KINDS = listOf("machine", "free_weights", "cable", "body_weight", "cardio", "other")

@Composable
fun PushPullPage() {
    val app = LocalApp.current
    SPage("Push and pull") {
        Intro("Choose which kinds of equipment ask whether an exercise is a push or a pull. Exercises you already marked keep their choice.")
        for (k in KINDS) {
            val key = PUSH_PULL_KEY[k] ?: continue
            OnOff(EQUIPMENT_LABELS[k] ?: k, app.settings[key] == "on") { app.setSetting(key, if (it) "on" else "off") }
        }
    }
}
