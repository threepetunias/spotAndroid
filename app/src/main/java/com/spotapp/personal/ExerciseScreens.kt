package com.spotapp.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// ---------- shared form pieces ----------

// One short row on a form: a label, the current choice in a few words, and an arrow. Tapping it opens a centered box.
@Composable
fun PickRow(label: String, value: String, placeholder: String = "Not set", onClick: () -> Unit, left: @Composable () -> Unit = {}) {
    val c = colors()
    val empty = value.isEmpty()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(16.dp)).background(c.card).border(1.dp, c.line, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$label, ${if (empty) placeholder else value}" }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        left()
        Column(Modifier.weight(1f)) {
            Text(label.uppercase(), color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            Text(
                if (empty) placeholder else value, color = if (empty) c.muted else c.ink, fontSize = 17.sp,
                fontWeight = if (empty) FontWeight.Medium else FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(Modifier.rotate(180f)) { SpotIcon("back", c.muted, 18.dp) }
    }
}

// A centered box with a title, scrolling content and a Done button.
@Composable
fun PickerBox(title: String, onClose: () -> Unit, hint: String? = null, doneLabel: String = "Done", content: @Composable () -> Unit) {
    val c = colors()
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.bg).border(1.dp, c.line, RoundedCornerShape(22.dp)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, Modifier.semantics { heading() }, color = c.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (hint != null) Text(hint, color = c.muted, fontSize = 15.sp, lineHeight = 21.sp)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) { content() }
            BigBtn(doneLabel, onClose, Modifier.fillMaxWidth())
        }
    }
}

// A picture tile in a grid.
@Composable
fun PictureTile(label: String, selected: Boolean, onClick: () -> Unit, multi: Boolean = false, modifier: Modifier = Modifier, picture: @Composable () -> Unit) {
    val c = colors()
    Box(
        modifier.heightIn(min = 92.dp).clip(RoundedCornerShape(16.dp)).background(c.card)
            .border(2.dp, if (selected) c.nowEdge else c.line, RoundedCornerShape(16.dp))
            .clickable(role = if (multi) Role.Checkbox else Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected; contentDescription = label },
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            picture()
            Text(label, color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (selected) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) { SpotIcon("check", c.onAccent, 13.dp) }
    }
}

@Composable
fun TileGrid(content: @Composable () -> Unit) {
    // Three per row; tiles are given equal width by the caller through a weight.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
fun <T> TileRows(items: List<T>, tile: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (rowItems in items.chunked(3)) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (it in rowItems) tile(it, Modifier.weight(1f))
            repeat(3 - rowItems.size) { Box(Modifier.weight(1f)) }
        }
    }
}

fun linkLabel(link: String): String =
    try { android.net.Uri.parse(link).host?.removePrefix("www.") ?: link } catch (e: Exception) { link }

// Shows the chosen picture and opens the full-screen picker. The answer comes back when this screen is shown again.
@Composable
fun IconPickerField(label: String, value: String, onChange: (String) -> Unit) {
    val app = LocalApp.current
    val c = colors()
    val top = LocalIsTop.current
    LaunchedEffect(top) {
        if (top) { val v = app.pickedIcon; if (v != null) { app.pickedIcon = null; onChange(v) } }
    }
    Column(Modifier.padding(top = 16.dp)) {
        Text(label, color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(14.dp)).background(c.card).border(1.5.dp, c.edge, RoundedCornerShape(14.dp))
                .clickable(role = Role.Button) { app.go(Route.PickIcon(value)) }
                .semantics { contentDescription = "$label. Opens the pictures." }.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (value.isNotEmpty()) WorkoutPicture(value, 40.dp) else Box(Modifier.size(40.dp).clip(CircleShape).background(c.chip))
            Text(if (value.isNotEmpty()) "Change picture" else "Choose a picture (optional)", Modifier.weight(1f), color = if (value.isNotEmpty()) c.ink else c.muted, fontSize = 18.sp)
            Box(Modifier.rotate(180f)) { SpotIcon("back", c.ink, 20.dp) }
        }
    }
}

// ---------- the exercise list ----------

@Composable
fun ExercisesScreen() {
    val app = LocalApp.current
    val c = colors()
    val state = load("exercises") { listExercises(true) }
    ScreenFrame(
        "Exercises",
        floating = { FloatingAdd("New exercise", { app.go(Route.ExerciseEdit(null)) }, raised = true); SettingsHomeHere() },
    ) {
        Loaded(state) { list ->
            val active = list.filter { !it.archived }
            val archived = list.filter { it.archived }
            @Composable
            fun rowFor(e: Exercise) {
                val detail = listOf(if (e.equipment.isNotEmpty()) EQUIPMENT_LABELS[e.equipment] ?: "" else "", if (e.movement.isNotEmpty()) MOVEMENT_LABELS[e.movement] ?: "" else "") .plus(e.machines.map { it.locationName }).filter { it.isNotEmpty() }.joinToString(" · ")
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(16.dp)).background(c.card)
                        .clickable(role = Role.Button, onClickLabel = "Edit ${e.name}") { app.go(Route.ExerciseEdit(e.id)) }
                        .semantics { contentDescription = e.name + (if (detail.isNotEmpty()) ", $detail" else "") }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (e.icon.isNotEmpty()) ExerciseBadge(e.icon, e.bodyParts, 44.dp)
                    Column(Modifier.weight(1f)) {
                        Text(e.name, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (detail.isNotEmpty()) Text(detail, color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                        if (e.cues.isNotEmpty()) Text(e.cues.joinToString(" · "), color = c.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            if (list.isEmpty()) Text("No exercises yet. Tap the + button to add one. Each can have a picture, equipment and places, and cues.", color = c.ink, fontSize = 18.sp, lineHeight = 26.sp, modifier = Modifier.padding(top = 24.dp))
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { for (e in active) rowFor(e) }
            if (archived.isNotEmpty()) Column(Modifier.padding(top = 26.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption("Archived")
                for (e in archived) rowFor(e)
            }
        }
    }
}

// ---------- the exercise editor ----------

@Composable
fun ExerciseEditScreen(id: Long?) {
    val app = LocalApp.current
    val c = colors()
    val context = LocalContext.current
    val isNew = id == null
    var loaded by remember(id) { mutableStateOf(isNew) }
    var name by remember(id) { mutableStateOf("") }
    var icon by remember(id) { mutableStateOf("") }
    var equipment by remember(id) { mutableStateOf("") }
    var movement by remember(id) { mutableStateOf("") }
    var bodyParts by remember(id) { mutableStateOf<List<String>>(emptyList()) }
    var placeIds by remember(id) { mutableStateOf<List<Long>>(emptyList()) }
    var tagIds by remember(id) { mutableStateOf<List<Long>>(emptyList()) }
    var cues by remember(id) { mutableStateOf<List<String>>(emptyList()) }
    var link by remember(id) { mutableStateOf("") }
    var archived by remember(id) { mutableStateOf(false) }
    var box by remember(id) { mutableStateOf<String?>(null) }
    var addingPlace by remember(id) { mutableStateOf(false) }
    var addingTag by remember(id) { mutableStateOf(false) }

    class Lists(val locations: List<Location>, val exTags: List<ExTag>, val ex: Exercise?)
    val state = load("exerciseEdit", id) { Lists(listLocations(), listExTags(), if (id != null) getExercise(id) else null) }
    val d = (state as? Load.Ready)?.value
    // Fields are filled from the saved exercise once, so a reload never overwrites what the person is typing.
    LaunchedEffect(d != null) {
        if (d != null && !loaded) {
            val e = d.ex
            if (e != null) {
                name = e.name; icon = e.icon; equipment = e.equipment; movement = e.movement; cues = e.cues; archived = e.archived
                bodyParts = e.bodyParts; tagIds = e.exTagIds; link = e.link; placeIds = e.machines.map { it.locationId }
            }
            loaded = true
        }
    }

    val showMove = pushPullOn(app.settings, equipment)
    fun save() {
        val input = Store.ExerciseInput(
            id, name, icon, equipment, if (showMove) movement else "", placeIds.map { it to "" }, cues.filter { it.trim().isNotEmpty() }, link, bodyParts, tagIds,
        )
        app.write({ saveExercise(input) }) { app.back() }
    }

    val locations = d?.locations ?: emptyList()
    val exTags = d?.exTags ?: emptyList()
    val placeNames = locations.filter { it.id in placeIds }.map { it.name }
    val tagNames = exTags.filter { it.id in tagIds }.map { it.name }
    val cueCount = cues.count { it.trim().isNotEmpty() }

    ScreenFrame(
        if (isNew) "New exercise" else "Exercise",
        bottom = {
            PinnedBar {
                BigBtn("Cancel", { app.back() }, Modifier.weight(1f), secondary = true)
                BigBtn("Save", { save() }, Modifier.weight(1f), enabled = loaded)
            }
        },
        floating = { SettingsHomeHere(raised = true) },
    ) {
        if (loaded && d != null) Column(Modifier.padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("Name", name, { name = it }, max = Limits.NAME, placeholder = "Exercise name")
            IconPickerField("Picture", icon) { icon = it }
            Box(Modifier.size(6.dp))
            PickRow("Equipment", if (equipment.isEmpty()) "" else EQUIPMENT_LABELS[equipment] ?: "", "Not set", { box = "equipment" })
            PickRow("Body parts", bodyParts.joinToString(", ") { BODY_PART_LABELS[it] ?: it }, "Not set", { box = "body" })
            if (showMove) PickRow("Push or pull", if (movement.isEmpty()) "" else MOVEMENT_LABELS[movement] ?: "", "Not set", { box = "movement" })
            if (needsPlace(equipment)) PickRow("Places", placeNames.joinToString(", "), "Choose every place you do it", { box = "places" })
            PickRow("Tags", tagNames.joinToString(", "), "None", { box = "tags" })
            PickRow("Cues", if (cueCount == 0) "" else "$cueCount ${if (cueCount == 1) "cue" else "cues"}", "None", { box = "cues" })
            PickRow("Link", if (link.isEmpty()) "" else linkLabel(link), "Add a web link", { box = "link" })
            if (link.isNotEmpty()) Row { BigBtn("Open link", { openLink(context, link, app) }, secondary = true) }
            if (id != null) Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigBtn(if (archived) "Bring back" else "Archive", {
                    val next = !archived
                    app.ask(
                        if (next) "Archive this exercise?" else "Bring it back?",
                        if (next) "It is hidden from the picker. Past workouts keep it." else "It will show in the picker again.",
                        if (next) "Archive" else "Bring back",
                    ) { app.write({ setArchived(id, next) }) { app.back() } }
                }, Modifier.weight(1f), secondary = true)
                BigBtn("Delete", {
                    app.ask(
                        "Delete this exercise?",
                        "It is removed from past workouts too, along with every set you logged for it. This cannot be undone. Archive keeps your history.",
                        "Delete", destructive = true,
                    ) { app.write({ deleteExercise(id) }) { app.back() } }
                }, Modifier.weight(1f), secondary = true)
            }
        }
    }

    when (box) {
        "equipment" -> PickerBox("Equipment", { box = null }) {
            ChoiceChips(EQUIPMENT.filter { it.isNotEmpty() }.mapIndexed { n, e -> Option((n + 1).toLong(), EQUIPMENT_LABELS[e] ?: e) },
                EQUIPMENT.indexOf(equipment).takeIf { it > 0 }?.toLong(), { v -> equipment = if (v == null) "" else EQUIPMENT[v.toInt()] }, clearable = true)
        }
        "body" -> PickerBox("Body parts", { box = null }, hint = "Choose every part this exercise works.") {
            TileRows(BODY_PARTS) { b, m ->
                PictureTile(BODY_PART_LABELS[b] ?: b, b in bodyParts, { bodyParts = if (b in bodyParts) bodyParts - b else bodyParts + b }, multi = true, modifier = m) { BodyBadge(b, 44.dp) }
            }
        }
        "movement" -> PickerBox("Push or pull", { box = null }) {
            ChoiceChips(listOf(Option(1, MOVEMENT_LABELS.getValue("push")), Option(2, MOVEMENT_LABELS.getValue("pull"))),
                when (movement) { "push" -> 1L; "pull" -> 2L; else -> null }, { v -> movement = when (v) { 1L -> "push"; 2L -> "pull"; else -> "" } }, clearable = true)
        }
        "places" -> PickerBox("Places", { box = null }, hint = "Choose every place you do it. The equipment is a little different at each one, so each place keeps its own history.") {
            TileRows(locations) { l, m ->
                PictureTile(l.name, l.id in placeIds, { placeIds = if (l.id in placeIds) placeIds - l.id else placeIds + l.id }, multi = true, modifier = m) { PlaceBadge(l.icon, l.color, 44.dp) }
            }
            Row(Modifier.padding(top = 10.dp)) { AddChip("New place") { addingPlace = true } }
        }
        "tags" -> PickerBox("Tags", { box = null }, hint = "Your own kinds of exercise, such as Rehab or Warm-up. Set them up in Settings, Exercise tags.") {
            MultiChips(exTags.map { Option(it.id, it.name) }, tagIds, { t -> tagIds = if (t in tagIds) tagIds - t else tagIds + t }, addLabel = "New tag", onAdd = { addingTag = true })
        }
        "cues" -> PickerBox("Cues", { box = null }, hint = "Short reminders that show while you log, such as \"press feet into floor\".") {
            cues.forEachIndexed { i, q ->
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { Field("Cue ${i + 1}", q, { v -> cues = cues.mapIndexed { k, x -> if (k == i) v else x } }, max = Limits.CUE) }
                    Box(Modifier.padding(bottom = 2.dp)) { CircleBtn("close", "Remove cue ${i + 1}", { cues = cues.filterIndexed { k, _ -> k != i } }) }
                }
            }
            if (cues.size < Limits.CUES) Row(Modifier.padding(top = 12.dp)) { BigBtn("Add cue", { cues = cues + "" }, secondary = true) }
        }
        "link" -> PromptDialog(
            "Link", { box = null }, onSubmit = { t ->
                try { link = cleanLink(t.trim()); box = null } catch (e: Exception) { app.fail(e) }
            },
            confirmLabel = "Save link", initial = link, maxLength = Limits.LINK, placeholder = "https://", url = true, allowEmpty = true,
        )
    }
    if (addingPlace) PromptDialog("New place", { addingPlace = false }, onSubmit = { t ->
        app.write({ saveLocation(null, t, "gym", "") }) { nid -> placeIds = placeIds + nid; addingPlace = false }
    }, maxLength = Limits.LOCATION, placeholder = "Riverside Gym")
    if (addingTag) PromptDialog("New tag", { addingTag = false }, onSubmit = { t ->
        app.write({ saveExTag(null, t) }) { nid -> tagIds = tagIds + nid; addingTag = false }
    }, maxLength = Limits.EX_TAG, placeholder = "Rehab")
}

// ---------- the picture picker ----------

@Composable
fun PickIconScreen(current: String) {
    val app = LocalApp.current
    val c = colors()
    val cur = if (Regex("^[a-z0-9_]*$").matches(current)) current else ""
    var q by remember { mutableStateOf("") }
    var sel by remember { mutableStateOf(cur) }
    var open by remember { mutableStateOf<Set<String>>(emptySet()) }
    val searching = q.trim().isNotEmpty()
    val found = if (searching) Pictures.search(q) else emptyList()
    val chosen = Pictures.byId(sel)

    @Composable
    fun cell(i: PicDef, mod: Modifier) {
        val on = i.id == sel
        Column(
            mod.clickable(role = Role.Button) { sel = i.id }.semantics { contentDescription = i.name; this.selected = on }.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.then(if (on) Modifier.border(3.dp, c.ink, CircleShape) else Modifier).padding(if (on) 2.dp else 5.dp)) { WorkoutPicture(i.id, 52.dp) }
            Text(i.name, color = c.ink, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp))
        }
    }

    fun use(id: String) { app.pickedIcon = id; app.back() }

    ScreenFrame(
        "Pictures", pad = 14.dp,
        bottom = {
            PinnedBar {
                BigBtn("No picture", { use("") }, Modifier.weight(1f), secondary = true)
                BigBtn("Use this", { use(sel) }, Modifier.weight(1f))
            }
        },
        floating = { SettingsHomeHere(raised = true) },
    ) {
        Box(Modifier.padding(horizontal = 6.dp)) { Field("Search", q, { q = it }, max = 30, placeholder = "Try lat, row, legs, swim or yoga") }
        if (chosen != null) Row(Modifier.padding(start = 6.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WorkoutPicture(chosen.id, 56.dp)
            Text(chosen.name, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        if (searching) {
            if (found.isEmpty()) Text("Nothing matches. Try a simpler word, like \"row\" or \"yoga\".", color = c.muted, fontSize = 16.sp, modifier = Modifier.padding(8.dp))
            else TileRows4(found) { i, m -> cell(i, m) }
        } else {
            for (g in Pictures.groups + "Other") {
                val list = Pictures.all.filter { it.group == g }
                if (list.isEmpty()) continue
                val on = g in open
                Column(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card)) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable(role = Role.Button) { open = if (on) open - g else open + g }
                            .semantics { contentDescription = "$g, ${list.size} pictures" }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(g, Modifier.weight(1f), color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("${list.size}", color = c.muted, fontSize = 15.sp)
                        Box(Modifier.rotate(if (on) 90f else -90f)) { SpotIcon("back", c.muted, 18.dp) }
                    }
                    if (on) Box(Modifier.padding(horizontal = 4.dp, bottom = 8.dp)) { TileRows4(list) { i, m -> cell(i, m) } }
                }
            }
        }
        Box(Modifier.size(16.dp))
    }
}

@Composable
fun <T> TileRows4(items: List<T>, tile: @Composable (T, Modifier) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        for (rowItems in items.chunked(4)) Row(Modifier.fillMaxWidth()) {
            for (it in rowItems) tile(it, Modifier.weight(1f))
            repeat(4 - rowItems.size) { Box(Modifier.weight(1f)) }
        }
    }
}
