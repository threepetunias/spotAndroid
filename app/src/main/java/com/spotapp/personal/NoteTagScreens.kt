package com.spotapp.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- Load past ----------

@Composable
fun LoadPastScreen(weId: Long) {
    val app = LocalApp.current
    val c = colors()
    var tagId by remember(weId) { mutableStateOf<Long?>(null) }
    class Data(val name: String, val unit: String, val sessions: List<Session>)
    val state = load("loadPast", weId) {
        val info = getWeInfo(weId)
        if (info == null) null else Data(info.exercise.name, info.exercise.unit, pastSessions(info.exercise.id, weId))
    }

    ScreenFrame("Load past") {
        Loaded(state) { d ->
            if (d == null) { Para("This exercise could not be found."); return@Loaded }
            val list = d.sessions
            val tagsSeen = ArrayList<Pair<Long, String>>()
            for (s in list) for (t in s.tags) if (tagsSeen.none { it.first == t.tagId }) tagsSeen.add(t.tagId to t.name)
            val picks = quickPicks(list, tagId)

            @Composable
            fun card(label: String, s: Session) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.card)
                        .clickable(role = Role.Button, onClickLabel = "Load this session") {
                            app.ask("Load this session?", "${summarize(s.sets, d.unit)} from ${prettyDay(s.day)} will replace the sets you have not logged yet.", "Load") {
                                app.write({ loadSession(weId, s.weId) }) { app.back() }
                            }
                        }
                        .semantics { contentDescription = "$label. ${summarize(s.sets, d.unit)}, ${prettyDay(s.day)}. Tap to load." }.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(label, color = c.accentText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(summarize(s.sets, d.unit), color = c.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(prettyDay(s.day) + " · " + s.workoutTitle + (if (s.tags.isNotEmpty()) " · " + s.tags.joinToString(", ") { tagLabel(it) } else ""), color = c.muted, fontSize = 15.sp)
                }
            }

            Text(d.name, color = c.muted, fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp))
            if (list.isEmpty()) Text("Nothing logged for this exercise yet. After your first workout, your earlier sessions show up here.", color = c.ink, fontSize = 18.sp, lineHeight = 26.sp, modifier = Modifier.padding(top = 20.dp))
            if (list.isNotEmpty()) Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { for (p in picks) card(p.label, p.session) }
            if (tagsSeen.isNotEmpty()) Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption("Last time with a tag")
                ChipRows { for ((id, n) in tagsSeen) Chip(n, tagId == id, { tagId = if (tagId == id) null else id }) }
            }
            if (list.isNotEmpty()) Column(Modifier.padding(top = 26.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Caption("Any session")
                for (s in list) card(prettyDay(s.day), s)
            }
        }
    }
}

// ---------- Note ----------

@Composable
fun NoteScreen(weId: Long) {
    val app = LocalApp.current
    val c = colors()
    var text by remember(weId) { mutableStateOf("") }
    var loaded by remember(weId) { mutableStateOf(false) }
    class Data(val name: String, val note: String)
    val state = load("note", weId) { getWeInfo(weId)?.let { Data(it.exercise.name, it.note) } }
    val d = (state as? Load.Ready)?.value
    // Blank until loaded, so an old note never flashes the wrong text. Filled once so a reload never overwrites typing.
    LaunchedEffect(d != null) { if (d != null && !loaded) { text = d.note; loaded = true } }

    ScreenFrame(
        "Notes",
        bottom = {
            PinnedBar {
                BigBtn("Cancel", { app.back() }, Modifier.weight(1f), secondary = true)
                BigBtn("Save", { val t = text; app.write({ setWeNote(weId, t) }) { app.back() } }, Modifier.weight(1f), enabled = loaded)
            }
        },
    ) {
        Text(d?.name ?: "", color = c.muted, fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp))
        if (loaded) Field("Note for this workout", text, { text = it }, max = Limits.NOTE, placeholder = "How did it feel?", minLines = 5, singleLine = false, hint = "${text.length} of ${Limits.NOTE}")
    }
}

// ---------- Tag picker ----------

private class TagPick(val tagId: Long, val severity: Int?)

@Composable
fun TagPickerScreen(workoutId: Long?, weId: Long?) {
    val app = LocalApp.current
    val c = colors()
    val target: TagTarget? = if (weId != null) TagTarget.OfItem(weId) else if (workoutId != null) TagTarget.OfWorkout(workoutId) else null
    var picks by remember(weId, workoutId) { mutableStateOf<List<TagPick>>(emptyList()) }
    var didLoad by remember(weId, workoutId) { mutableStateOf(false) }
    var addUnder by remember(weId, workoutId) { mutableStateOf<Tag?>(null) }
    class Data(val tags: List<Tag>, val heading: String, val links: List<TagLink>)
    val state = load("tagPicker", weId, workoutId) {
        val heading = if (weId != null) getWeInfo(weId)?.exercise?.name ?: "" else if (workoutId != null) getWorkoutTitle(workoutId) ?: "" else ""
        Data(listTags(), heading, if (target != null) getLinks(target) else emptyList())
    }
    val d = (state as? Load.Ready)?.value
    // The saved picks are copied in once; later reloads (new tags) never undo what was just ticked.
    LaunchedEffect(d != null) { if (d != null && !didLoad) { picks = d.links.map { TagPick(it.tagId, it.severity) }; didLoad = true } }

    fun toggle(t: Tag) { picks = if (picks.any { it.tagId == t.id }) picks.filter { it.tagId != t.id } else picks + TagPick(t.id, null) }
    fun setSeverity(tagId: Long, n: Int) { picks = picks.map { if (it.tagId == tagId) TagPick(it.tagId, if (it.severity == n) null else n) else it } }

    ScreenFrame(
        "Tags",
        bottom = {
            PinnedBar {
                BigBtn("Cancel", { app.back() }, Modifier.weight(1f), secondary = true)
                BigBtn("Save", {
                    if (target == null) app.back() else {
                        val links = picks.map { it.tagId to it.severity }
                        app.write({ setLinks(target, links) }) { app.back() }
                    }
                }, Modifier.weight(1f), enabled = didLoad)
            }
        },
    ) {
        Loaded(state) { data ->
            Text(data.heading, color = c.muted, fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp))
            if (data.tags.isEmpty()) Text("No tags yet. Add them in Settings, under Tags.", color = c.ink, fontSize = 17.sp, modifier = Modifier.padding(top = 16.dp))
            for (t in data.tags) {
                if (t.depth == 1) {
                    Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Caption(t.name)
                        Row { AddChip("New under ${t.name}") { addUnder = t } }
                    }
                } else {
                    val pick = picks.firstOrNull { it.tagId == t.id }
                    Column(
                        Modifier.padding(top = 8.dp, start = ((t.depth - 2) * 18).dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Checkbox) { toggle(t) }
                                .semantics { contentDescription = t.name + if (pick != null) ", chosen" else "" }.padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier.size(28.dp).clip(CircleShape).background(if (pick != null) c.nowFill else c.chip).border(2.dp, if (pick != null) c.nowEdge else c.edge, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { if (pick != null) SpotIcon("check", c.onNowFill, 16.dp) }
                            Text(t.name, Modifier.weight(1f), color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        if (pick != null && t.kind == "health") Column(Modifier.padding(horizontal = 6.dp).padding(bottom = 6.dp)) {
                            Text("How strong? ${if (pick.severity != null) "${pick.severity}/5" else "Not set"}", color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (n in 1..5) {
                                    val on = pick.severity != null && n <= pick.severity
                                    Box(
                                        Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(if (on) c.accent else c.chip)
                                            .clickable(role = Role.Button) { setSeverity(t.id, n) }
                                            .semantics { contentDescription = "Severity $n of 5"; selected = pick.severity == n },
                                        contentAlignment = Alignment.Center,
                                    ) { Text("$n", color = if (on) c.onAccent else c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }
            Text("Tags stay on this phone. They are for your own records and are not medical advice.", color = c.muted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 20.dp, bottom = 24.dp))
        }
    }
    val under = addUnder
    if (under != null) PromptDialog("New under ${under.name}", { addUnder = null }, onSubmit = { name ->
        app.write({ saveTag(null, name, null, under.id) }) { id -> picks = picks + TagPick(id, null); addUnder = null }
    }, maxLength = Limits.TAG, placeholder = "Knee pain")
}

// ---------- Tags (Settings) ----------

private sealed class TagDialog {
    object Group : TagDialog()
    class Child(val parent: Tag) : TagDialog()
    class Rename(val tag: Tag) : TagDialog()
}

@Composable
fun TagsScreen() {
    val app = LocalApp.current
    val c = colors()
    var dialog by remember { mutableStateOf<TagDialog?>(null) }
    val state = load("tags") { listTags() }

    ScreenFrame("Tags", floating = { SettingsHomeHere() }) {
        Loaded(state) { tags ->
            Text(
                "Organize tags in groups, for example Health, then Knee pain, then Left knee. Tags in a Health group get a 1 to 5 strength when you use them. Tags stay on this phone.",
                color = c.muted, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.padding(top = 8.dp),
            )
            Column(Modifier.padding(top = 18.dp)) {
                tags.forEachIndexed { i, t ->
                    if (i > 0 && t.depth == 1) Divider()
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = ((t.depth - 1) * 20).dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, color = c.ink, fontSize = if (t.depth == 1) 19.sp else 17.sp, fontWeight = if (t.depth == 1) FontWeight.ExtraBold else FontWeight.SemiBold)
                            if (t.depth == 1) Caption(if (t.kind == "health") "Health group" else "Notes group")
                        }
                        if (t.depth < 3) Box(Modifier.size(44.dp).clickable(role = Role.Button, onClickLabel = "Add under ${t.name}") { dialog = TagDialog.Child(t) }, contentAlignment = Alignment.Center) { SpotIcon("plus", c.ink, 20.dp) }
                        LinkTextSmall("Rename") { dialog = TagDialog.Rename(t) }
                        LinkTextSmall("Delete") {
                            val below = tags.count { it.parentId == t.id }
                            app.ask(
                                "Delete \"${t.name}\"?",
                                (if (below > 0) "Everything under it is deleted too. " else "") + "It is taken off every workout and exercise that uses it. This cannot be undone.",
                                "Delete", destructive = true,
                            ) { app.write({ deleteTag(t.id) }) }
                        }
                    }
                    if (t.depth == 1) Box(
                        Modifier.heightIn(min = 40.dp).clickable(role = Role.Button) {
                            app.write({ saveTag(t.id, t.name, if (t.kind == "health") "general" else "health", null) })
                        },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            if (t.kind == "health") "Make this a notes group (no strength)" else "Make this a health group (adds strength)",
                            color = c.muted, fontSize = 14.sp, textDecoration = TextDecoration.Underline,
                        )
                    }
                }
                if (tags.isEmpty()) Text("No tags yet.", color = c.ink, fontSize = 17.sp, modifier = Modifier.padding(top = 10.dp))
            }
            Row(Modifier.padding(top = 22.dp, bottom = 90.dp)) { BigBtn("New group", { dialog = TagDialog.Group }) }
        }
    }
    val dg = dialog
    if (dg != null) PromptDialog(
        title = when (dg) { is TagDialog.Group -> "New group"; is TagDialog.Child -> "New under ${dg.parent.name}"; is TagDialog.Rename -> "Rename" },
        onCancel = { dialog = null },
        initial = if (dg is TagDialog.Rename) dg.tag.name else "",
        maxLength = Limits.TAG,
        placeholder = if (dg is TagDialog.Group) "Training notes" else "Knee pain",
        confirmLabel = if (dg is TagDialog.Rename) "Save" else "Add",
        onSubmit = { text ->
            when (dg) {
                is TagDialog.Group -> app.write({ saveTag(null, text, "general", null) }) { dialog = null }
                is TagDialog.Child -> app.write({ saveTag(null, text, null, dg.parent.id) }) { dialog = null }
                is TagDialog.Rename -> app.write({ saveTag(dg.tag.id, text, null, null) }) { dialog = null }
            }
        },
    )
}

@Composable
fun LinkTextSmall(text: String, onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
        Text(text, color = colors().ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
    }
}
