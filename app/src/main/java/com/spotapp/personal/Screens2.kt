package com.spotapp.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- Load past ----------

@Composable
fun LoadPastScreen(weId: Long?) {
    val app = LocalApp.current
    val c = colors()
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("lb") }
    var sessions by remember { mutableStateOf<List<Session>?>(null) }
    var tagId by remember { mutableStateOf<Long?>(null) }

    OnResume(weId) {
        if (weId != null) {
            app.io({
                val info = app.repo.getWeInfo(weId)
                if (info == null) null else Triple(info.exercise.name, info.exercise.unit, app.repo.pastSessions(info.exercise.id, weId))
            }, { r ->
                if (r != null) { name = r.first; unit = r.second; sessions = r.third }
            })
        }
    }

    val list = sessions ?: emptyList()
    val tagsSeen = ArrayList<Pair<Long, String>>()
    for (s in list) for (t in s.tags) if (tagsSeen.none { it.first == t.tagId }) tagsSeen.add(t.tagId to t.name)
    val picks = quickPicks(list, tagId)

    fun apply(s: Session) {
        val id = weId ?: return
        app.confirm = Confirm(
            "Load this session?",
            "${summarize(s.sets, unit)} from ${prettyDay(s.day)} will replace the sets you have not logged yet.",
            "Load",
        ) { app.io({ app.repo.loadSession(id, s.weId) }, { app.back() }) }
    }

    @Composable
    fun SessionCard(label: String, s: Session) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.card)
                .clickable(role = Role.Button) { apply(s) }
                .semantics { contentDescription = "$label. ${summarize(s.sets, unit)}, ${prettyDay(s.day)}. Tap to load." }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, color = c.accentText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(summarize(s.sets, unit), color = c.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "${prettyDay(s.day)} · ${s.workoutTitle}" + (if (s.tags.isNotEmpty()) " · " + s.tags.joinToString(", ") { tagLabel(it) } else ""),
                color = c.muted, fontSize = 15.sp,
            )
        }
    }

    ScreenFrame(title = "Load past") {
        Text(name, color = c.muted, fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp))
        if (sessions != null && list.isEmpty()) {
            Text(
                "Nothing logged for this exercise yet. After your first workout, your earlier sessions show up here.",
                color = c.ink, fontSize = 18.sp, lineHeight = 26.sp, modifier = Modifier.padding(top = 20.dp),
            )
        }
        if (list.isNotEmpty()) {
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (p in picks) SessionCard(p.label, p.session)
            }
        }
        if (tagsSeen.isNotEmpty()) {
            Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption("Last time with a tag")
                ChipRows(tagsSeen.map { (id, n) -> chipItem(n, tagId == id, { tagId = if (tagId == id) null else id }) })
            }
        }
        if (list.isNotEmpty()) {
            Column(Modifier.padding(top = 26.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Caption("Any session")
                for (s in list) SessionCard(prettyDay(s.day), s)
            }
        }
    }
}

// ---------- Notes ----------

@Composable
fun NoteScreen(weId: Long?) {
    val app = LocalApp.current
    var name by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    OnResume(weId) {
        if (weId != null && !loaded) {
            app.io({ app.repo.getWeInfo(weId) }, { i ->
                if (i != null) { name = i.exercise.name; text = i.note }
                loaded = true
            })
        }
    }
    ScreenFrame(
        title = "Notes",
        bottom = {
            PinnedBar {
                BigBtn("Cancel", { app.back() }, Modifier.weight(1f), secondary = true)
                BigBtn("Save", {
                    val id = weId
                    val t = text
                    if (id == null) app.back() else app.io({ app.repo.setWeNote(id, t) }, { app.back() })
                }, Modifier.weight(1f), enabled = loaded)
            }
        },
    ) {
        Text(name, color = colors().muted, fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp))
        // Blank until loaded, so an old note never flashes the wrong text.
        if (loaded) {
            Field(
                "Note for this workout", text, { text = it }, max = Limits.NOTE, placeholder = "How did it feel?",
                minLines = 5, singleLine = false, hint = "${text.length} of ${Limits.NOTE}",
            )
        }
    }
}

// ---------- Tag picker (for a workout or one exercise) ----------

@Composable
fun TagPickerScreen(kind: String?, targetId: Long?) {
    val app = LocalApp.current
    val c = colors()
    val isWorkout = kind == "workout"
    var tags by remember { mutableStateOf(listOf<Tag>()) }
    var picks by remember { mutableStateOf(listOf<Pair<Long, Int?>>()) }
    var heading by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newKind by remember { mutableStateOf("health") }
    var didLoad by remember { mutableStateOf(false) }

    OnResume(targetId) {
        app.io({
            val t = app.repo.listTags()
            val h = if (targetId == null) "" else if (isWorkout) (app.repo.getWorkoutTitle(targetId) ?: "") else (app.repo.getWeInfo(targetId)?.exercise?.name ?: "")
            val saved = if (didLoad || targetId == null) null else (if (isWorkout) app.repo.getLinksForWorkout(targetId) else app.repo.getLinksForWe(targetId))
            Triple(t, h, saved)
        }, { r ->
            tags = r.first; heading = r.second
            val saved = r.third
            if (saved != null) { picks = saved.map { it.tagId to it.severity }; didLoad = true }
        })
    }

    fun toggle(t: Tag) {
        picks = if (picks.any { it.first == t.id }) picks.filter { it.first != t.id } else picks + (t.id to null)
    }
    fun setSeverity(id: Long, n: Int) {
        picks = picks.map { if (it.first == id) it.first to (if (it.second == n) null else n) else it }
    }

    @Composable
    fun TagCard(t: Tag) {
        val pick = picks.firstOrNull { it.first == t.id }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Checkbox) { toggle(t) }
                    .semantics { contentDescription = t.name + if (pick != null) ", chosen" else "" }.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(if (pick != null) c.nowFill else c.chip)
                        .border(2.dp, if (pick != null) c.nowEdge else c.edge, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (pick != null) SpotIcon("check", c.onNowFill, 16.dp) }
                Text(t.name, Modifier.weight(1f), color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            if (pick != null && t.kind == "health") {
                Column(Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("How strong? ${if (pick.second != null) "${pick.second}/5" else "Not set"}", color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (n in 1..5) {
                            val on = pick.second != null && n <= pick.second!!
                            Box(
                                Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(if (on) c.accent else c.chip)
                                    .clickable(role = Role.Button) { setSeverity(t.id, n) }
                                    .semantics { contentDescription = "Severity $n of 5"; selected = pick.second == n },
                                contentAlignment = Alignment.Center,
                            ) { Text("$n", color = if (on) c.onAccent else c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }

    val health = tags.filter { it.kind == "health" }
    val general = tags.filter { it.kind == "general" }
    ScreenFrame(
        title = "Tags",
        bottom = {
            PinnedBar {
                BigBtn("Cancel", { app.back() }, Modifier.weight(1f), secondary = true)
                BigBtn("Save", {
                    val id = targetId
                    val p = picks
                    if (id == null) app.back() else app.io({ app.repo.setLinks(isWorkout, id, p) }, { app.back() })
                }, Modifier.weight(1f))
            }
        },
    ) {
        Text(heading, color = c.muted, fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp))
        if (health.isNotEmpty()) {
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Caption("Health"); for (t in health) TagCard(t) }
        }
        if (general.isNotEmpty()) {
            Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Caption("General"); for (t in general) TagCard(t) }
        }
        Column(Modifier.padding(top = 24.dp)) {
            Caption("New tag")
            Field("Name", newName, { newName = it }, max = Limits.TAG, placeholder = "For example: shoulder, tired, short on time")
            Spacer(Modifier.height(12.dp))
            ChipRows(listOf(
                chipItem("Health", newKind == "health", { newKind = "health" }),
                chipItem("General", newKind == "general", { newKind = "general" }),
            ))
            Row(Modifier.padding(top = 12.dp).fillMaxWidth()) {
                BigBtn("Add tag", {
                    val n = newName
                    val k = newKind
                    app.io({ val id = app.repo.saveTag(null, n, k); Pair(id, app.repo.listTags()) }, { r ->
                        tags = r.second
                        picks = picks + (r.first to null)
                        newName = ""
                    })
                }, Modifier.weight(1f), secondary = true, enabled = newName.isNotBlank())
            }
        }
        Text(
            "Tags stay on this phone. They are for your own records and are not medical advice.",
            color = c.muted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 20.dp, bottom = 24.dp),
        )
    }
}

// ---------- Manage tags ----------

@Composable
fun TagsScreen() {
    val app = LocalApp.current
    val c = colors()
    var tags by remember { mutableStateOf(listOf<Tag>()) }
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("health") }
    fun load() { app.io({ app.repo.listTags() }, { tags = it }) }
    OnResume { load() }

    ScreenFrame(title = "Tags") {
        Text(
            "Tag workouts and exercises, for example a health issue and how strong it was. Tags stay on this phone.",
            color = c.muted, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.padding(top = 16.dp),
        )
        Column(Modifier.padding(top = 18.dp)) {
            Caption("All tags")
            if (tags.isEmpty()) Text("No tags yet.", color = c.ink, fontSize = 17.sp, modifier = Modifier.padding(top = 10.dp))
            tags.forEachIndexed { i, t ->
                if (i > 0) Divider()
                Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(t.name, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(if (t.kind == "health") "Health, with a 1 to 5 strength" else "General", color = c.muted, fontSize = 14.sp)
                    }
                    LinkText("Switch type") {
                        app.io({ app.repo.saveTag(t.id, t.name, if (t.kind == "health") "general" else "health") }, { load() })
                    }
                    LinkText("Delete") {
                        app.confirm = Confirm("Delete \"${t.name}\"?", "It will be taken off every workout and exercise that uses it. This cannot be undone.", "Delete") {
                            app.io({ app.repo.deleteTag(t.id) }, { load() })
                        }
                    }
                }
            }
        }
        Column(Modifier.padding(top = 24.dp)) {
            Caption("New tag")
            Field("Name", name, { name = it }, max = Limits.TAG)
            Spacer(Modifier.height(12.dp))
            ChipRows(listOf(
                chipItem("Health", kind == "health", { kind = "health" }),
                chipItem("General", kind == "general", { kind = "general" }),
            ))
            Row(Modifier.padding(top = 12.dp).fillMaxWidth()) {
                BigBtn("Add tag", {
                    val n = name
                    val k = kind
                    app.io({ app.repo.saveTag(null, n, k) }, { name = ""; load() })
                }, Modifier.weight(1f), enabled = name.isNotBlank())
            }
        }
        Row(Modifier.padding(top = 20.dp, bottom = 32.dp).fillMaxWidth()) { BigBtn("Done", { app.back() }, Modifier.weight(1f), secondary = true) }
    }
}

// ---------- Exercise list ----------

@Composable
fun ExerciseListScreen() {
    val app = LocalApp.current
    val c = colors()
    var rows by remember { mutableStateOf<List<Exercise>?>(null) }
    OnResume { app.io({ app.repo.listExercises(true) }, { rows = it }) }
    val list = rows ?: emptyList()

    @Composable
    fun ExRow(e: Exercise) {
        val detail = listOf(e.machine, e.gym).filter { it.isNotEmpty() }.joinToString(" · ")
        Column(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(16.dp)).background(c.card)
                .clickable(role = Role.Button) { app.go("exercise/${e.id}") }
                .semantics { contentDescription = e.name + if (detail.isNotEmpty()) ", $detail" else "" }
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(e.name, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (detail.isNotEmpty()) Text(detail, color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
            if (e.cues.isNotEmpty()) Text(e.cues.joinToString(" · "), color = c.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
    }

    ScreenFrame(
        title = "Exercises",
        bottom = { PinnedBar { BigBtn("New exercise", { app.go("exercise/new") }, Modifier.weight(1f)) } },
    ) {
        if (rows != null && list.isEmpty()) {
            Text(
                "No exercises yet. Add a machine or make up a name. Each one can have a gym, a step size and cues.",
                color = c.ink, fontSize = 18.sp, lineHeight = 26.sp, modifier = Modifier.padding(top = 24.dp),
            )
        }
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { for (e in list.filter { !it.archived }) ExRow(e) }
        val arch = list.filter { it.archived }
        if (arch.isNotEmpty()) {
            Column(Modifier.padding(top = 26.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Caption("Archived"); for (e in arch) ExRow(e) }
        }
    }
}

// ---------- Exercise edit ----------

@Composable
fun ExerciseEditScreen(idParam: String?) {
    val app = LocalApp.current
    val c = colors()
    val isNew = idParam == "new"
    val id: Long? = if (isNew) null else paramId(idParam)
    var loaded by remember { mutableStateOf(isNew) }
    var name by remember { mutableStateOf("") }
    var machine by remember { mutableStateOf("") }
    var gym by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(app.settings["unit"] ?: "lb") }
    var step by remember { mutableStateOf(5.0) }
    var cues by remember { mutableStateOf(listOf<String>()) }
    var archived by remember { mutableStateOf(false) }
    var gyms by remember { mutableStateOf(listOf<String>()) }

    OnResume(id) {
        app.io({ Pair(app.repo.listGyms(), if (id != null) app.repo.getExercise(id) else null) }, { r ->
            gyms = r.first
            val e = r.second
            if (e != null && !loaded) {
                name = e.name; machine = e.machine; gym = e.gym; unit = e.unit; step = e.step; cues = e.cues; archived = e.archived
            }
            loaded = true
        })
    }

    ScreenFrame(
        title = if (isNew) "New exercise" else "Exercise",
        bottom = {
            PinnedBar {
                BigBtn("Cancel", { app.back() }, Modifier.weight(1f), secondary = true)
                BigBtn("Save", {
                    val n = name; val m = machine; val g = gym; val u = unit; val s = step; val cs = cues
                    app.io({ app.repo.saveExercise(id, n, m, g, u, s, cs) }, { app.back() })
                }, Modifier.weight(1f), enabled = loaded)
            }
        },
    ) {
        if (!loaded) return@ScreenFrame
        Field("Name", name, { name = it }, max = Limits.NAME, placeholder = "Lat pulldown")
        Column(Modifier.padding(top = 24.dp)) {
            Caption("Cues")
            Text("Short reminders that show while you log, such as \"press feet into floor\".", color = c.muted, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
            cues.forEachIndexed { i, q ->
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Cue ${i + 1}", q, { v -> cues = cues.mapIndexed { k, x -> if (k == i) v else x } }, Modifier.weight(1f), max = Limits.CUE)
                    Box(Modifier.padding(bottom = 3.dp)) { CircleBtn("close", "Remove cue ${i + 1}", { cues = cues.filterIndexed { k, _ -> k != i } }) }
                }
            }
            if (cues.size < Limits.CUES) {
                Row(Modifier.padding(top = 12.dp).fillMaxWidth()) { BigBtn("Add cue", { cues = cues + "" }, Modifier.weight(1f), secondary = true) }
            }
        }
        Field("Machine", machine, { machine = it }, max = Limits.MACHINE, placeholder = "Machine #4 (optional)")
        Field("Gym", gym, { gym = it }, max = Limits.GYM, placeholder = "Riverside Gym (optional)")
        if (gyms.isNotEmpty()) {
            Column(Modifier.padding(top = 10.dp)) {
                ChipRows(gyms.take(6).map { g -> chipItem(g, gym == g, { gym = g }) })
            }
        }
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Units")
            ChipRows(listOf(
                chipItem("lb", unit == "lb", { unit = "lb" }),
                chipItem("kg", unit == "kg", { unit = "kg" }),
            ))
        }
        Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Weight step")
            ChipRows(listOf(1.0, 2.5, 5.0).map { s -> chipItem("${num(s)} $unit", step == s, { step = s }) })
        }
        if (id != null) {
            Row(Modifier.padding(top = 28.dp, bottom = 24.dp).fillMaxWidth()) {
                BigBtn(if (archived) "Bring back" else "Archive", {
                    val next = !archived
                    app.confirm = Confirm(
                        if (next) "Archive this exercise?" else "Bring it back?",
                        if (next) "It is hidden from the picker. Past workouts keep it." else "It will show in the picker again.",
                        if (next) "Archive" else "Bring back",
                    ) { app.io({ app.repo.setArchived(id, next) }, { app.back() }) }
                }, Modifier.weight(1f), secondary = true)
            }
        } else {
            Spacer(Modifier.height(24.dp))
        }
    }
}
