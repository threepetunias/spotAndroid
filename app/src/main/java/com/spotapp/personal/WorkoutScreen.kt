package com.spotapp.personal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun round2(n: Double) = Math.round(n * 100.0) / 100.0

private data class Derived(val cur: SetRow?, val curNo: Int, val editingDone: Boolean, val allDone: Boolean)

private fun derive(item: WeFull, sel: Long?): Derived {
    val sets = item.sets
    val cur = sets.firstOrNull { it.id == sel } ?: sets.firstOrNull { !it.done } ?: sets.lastOrNull()
    val no = if (cur != null) sets.indexOfFirst { it.id == cur.id } + 1 else 0
    return Derived(cur, no, cur != null && cur.done && sel == cur.id, sets.isNotEmpty() && sets.all { it.done })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutScreen(wid: Long?) {
    val app = LocalApp.current
    val c = colors()
    var w by remember { mutableStateOf<WorkoutFull?>(null) }
    var missing by remember { mutableStateOf(false) }
    var idx by remember { mutableStateOf(0) }
    var sel by remember { mutableStateOf<Long?>(null) }
    var menu by remember { mutableStateOf("") }
    var last by remember { mutableStateOf<Session?>(null) }
    var didInit by remember { mutableStateOf(false) }

    fun reload(after: () -> Unit = {}) {
        if (wid == null) { missing = true; return }
        app.io({ app.repo.getWorkout(wid) }, { x ->
            if (x == null) {
                missing = true
            } else {
                w = x
                if (!didInit) {
                    didInit = true
                    val f = x.items.indexOfFirst { it.status == "todo" }
                    idx = if (f >= 0) f else 0
                }
                after()
            }
        })
    }

    val items = w?.items ?: emptyList()
    val safeIdx = if (items.isNotEmpty()) minOf(idx, items.size - 1) else 0
    val item = items.getOrNull(safeIdx)

    OnResume { reload() }
    OnResume(item?.id) {
        val it = item
        if (it == null) last = null
        else app.io({ app.repo.pastSessions(it.exercise.id, it.id) }, { s -> last = s.firstOrNull() })
    }

    fun act(block: () -> Unit) = app.io(block, { reload() })

    fun goTo(i: Int) {
        if (i == safeIdx || i < 0 || i >= items.size) return
        idx = i
        sel = null
        menu = ""
    }

    fun finishWorkout() {
        val id = wid ?: return
        val cur = item
        val voice = app.settings["voice"] ?: "gentle"
        app.io({
            if (cur != null && cur.status == "todo") app.repo.setWeStatus(cur.id, if (cur.sets.any { it.done }) "done" else "skipped")
            app.repo.setWorkoutDone(id, true)
            val fresh = app.repo.getWorkout(id)
            val s = fresh?.items?.sumOf { i -> i.sets.count { it.done } } ?: 0
            val e = fresh?.items?.count { it.status == "done" } ?: 0
            Pair(s, e)
        }, { r ->
            app.confirm = Confirm("Workout finished", finishLine(voice, r.first, r.second), "OK", null) { app.nav.popBackStack("start", false) }
        })
    }

    if (missing) {
        ScreenFrame(title = "Workout") { Text("This workout could not be found.", color = c.ink, fontSize = 18.sp, modifier = Modifier.padding(top = 24.dp)) }
        return
    }
    val wk = w
    if (wk == null) { ScreenFrame(title = "Workout") {}; return }
    if (item == null) {
        ScreenFrame(title = wk.title, help = true, cog = true) {
            Text("No exercises in this workout yet.", color = c.ink, fontSize = 18.sp, modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))
            Row(Modifier.fillMaxWidth()) { BigBtn("Add exercises", { app.go("add/${wk.id}") }, Modifier.weight(1f)) }
        }
        return
    }

    val d = derive(item, sel)
    val isLast = safeIdx >= items.size - 1
    val primaryLabel = when {
        d.cur == null -> "Add a set"
        !d.cur.done -> "Log set ${d.curNo}"
        d.editingDone -> "Done editing"
        isLast -> "Finish workout"
        else -> "Next exercise"
    }

    fun onPrimary() {
        val cur = d.cur
        if (cur == null) {
            app.io({ app.repo.addSet(item.id) }, { id -> sel = id; reload() })
            return
        }
        if (!cur.done) { act { app.repo.updateSet(cur.id, done = true); }; sel = null; return }
        if (d.editingDone) { sel = null; return }
        if (isLast) { finishWorkout(); return }
        app.io({ app.repo.setWeStatus(item.id, "done") }, { reload { goTo(safeIdx + 1) } })
    }

    fun dotWord(i: Int): String = if (i == safeIdx) "current" else when (items[i].status) { "done" -> "done"; "skipped" -> "skipped"; else -> "not started" }

    @Composable
    fun Dot(i: Int) {
        val st = items[i].status
        when {
            i == safeIdx -> Box(Modifier.size(16.dp).clip(CircleShape).background(c.nowFill).border(2.dp, c.ink, CircleShape))
            st == "done" -> Box(Modifier.size(12.dp).clip(CircleShape).background(c.ok))
            // skipped is shown with a thinner ring, never by color alone: the label also says "skipped"
            st == "skipped" -> Box(Modifier.size(12.dp).border(1.dp, c.edge, CircleShape))
            else -> Box(Modifier.size(12.dp).border(2.dp, c.edge, CircleShape))
        }
    }

    ScreenFrame(
        title = wk.title, help = true, cog = true, scroll = false,
        bottom = {
            PinnedBar {
                CircleBtn("list", "All exercises", { menu = "list" }, size = 56.dp, iconSize = 24.dp)
                BigBtn(primaryLabel, { onPrimary() }, Modifier.weight(1f))
            }
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp)) {
                for (i in items.indices) {
                    Box(
                        Modifier.size(44.dp)
                            .clickable(role = Role.Button, onClickLabel = "Go to ${items[i].exercise.name}") { goTo(i) }
                            .semantics { contentDescription = "Exercise ${i + 1}, ${items[i].exercise.name}, ${dotWord(i)}" },
                        contentAlignment = Alignment.Center,
                    ) { Dot(i) }
                }
            }
        }

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 6.dp)) {
            AnimatedContent(
                targetState = safeIdx,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(260)) { dir * 120 } + fadeIn(tween(260))) togetherWith fadeOut(tween(120))
                },
                label = "exercise",
            ) { i ->
                val it = items.getOrNull(i) ?: item
                val dd = derive(it, sel)
                val ex = it.exercise
                val unit = ex.unit
                val cur = dd.cur
                val sub = listOf(ex.machine, ex.gym).filter { s -> s.isNotEmpty() }.joinToString(" · ")
                val allTags = wk.tags + it.tags
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.size(44.dp))
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(ex.name, color = c.ink, fontFamily = Serif, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
                            if (sub.isNotEmpty()) Text(sub, color = c.muted, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                        }
                        CircleBtn("more", "Exercise actions", { menu = "exercise" })
                    }
                    if (allTags.isNotEmpty()) {
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (t in allTags) {
                                Box(Modifier.clip(RoundedCornerShape(14.dp)).background(c.chip).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text(tagLabel(t), color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        it.sets.forEachIndexed { n, s ->
                            val isCur = cur != null && cur.id == s.id
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(c.card)
                                    .border(2.dp, if (isCur) c.nowEdge else c.card, RoundedCornerShape(16.dp))
                                    .clickable(role = Role.Button) { sel = s.id }
                                    .semantics { contentDescription = "Set ${n + 1}, ${num(s.weight)} $unit for ${s.reps} reps, ${if (s.done) "done" else if (isCur) "editing now" else "not yet done"}. Tap to edit." }
                                    .padding(start = 8.dp, end = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(Modifier.size(32.dp).clip(CircleShape).background(if (isCur) c.nowFill else c.chip), contentAlignment = Alignment.Center) {
                                    Text("${n + 1}", color = if (isCur) c.onNowFill else c.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("${num(s.weight)} $unit × ${s.reps}", Modifier.weight(1f), color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                if (s.done) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        SpotIcon("check", c.ok, 20.dp)
                                        Text("Done", color = c.ok, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(if (isCur) "Now" else "Next", color = if (isCur) c.accentText else c.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp))
                                .border(2.dp, c.edge, RoundedCornerShape(16.dp))
                                .clickable(role = Role.Button, onClickLabel = "Add a set") {
                                    app.io({ app.repo.addSet(it.id) }, { id -> sel = id; reload() })
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        ) {
                            SpotIcon("plus", c.ink, 20.dp)
                            Text("Add set", color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (cur != null) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                CircleBtn("minus", "Weight minus ${num(ex.step)}", {
                                    act { app.repo.updateSet(cur.id, weight = minOf(Limits.MAX_WEIGHT, maxOf(0.0, round2(cur.weight - ex.step)))) }
                                }, size = 52.dp)
                                Row(Modifier.widthIn(min = 150.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                                    NumberField(cur.weight, "Weight", 64, Limits.MAX_WEIGHT, false) { v -> act { app.repo.updateSet(cur.id, weight = v) } }
                                    Text(unit, color = c.muted, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 10.dp))
                                }
                                CircleBtn("plus", "Weight plus ${num(ex.step)}", {
                                    act { app.repo.updateSet(cur.id, weight = minOf(Limits.MAX_WEIGHT, maxOf(0.0, round2(cur.weight + ex.step)))) }
                                }, size = 52.dp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                CircleBtn("minus", "Reps minus 1", { act { app.repo.updateSet(cur.id, reps = maxOf(0, cur.reps - 1)) } }, size = 48.dp)
                                Row(Modifier.widthIn(min = 150.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                                    NumberField(cur.reps.toDouble(), "Reps", 44, Limits.MAX_REPS.toDouble(), true) { v -> act { app.repo.updateSet(cur.id, reps = v.toInt()) } }
                                    Text("reps", color = c.muted, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
                                }
                                CircleBtn("plus", "Reps plus 1", { act { app.repo.updateSet(cur.id, reps = minOf(Limits.MAX_REPS.toInt(), cur.reps + 1)) } }, size = 48.dp)
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        val l = last
                        Text(
                            if (l != null) "Last time · ${summarize(l.sets)} · ${prettyDay(l.day)}" else "No earlier session yet",
                            color = c.muted, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false),
                        )
                        if (l != null) LinkText("Load…") { app.go("load/${it.id}") }
                    }

                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.cue)
                            .clickable(role = Role.Button, onClickLabel = "Edit cues") { app.go("exercise/${ex.id}") }
                            .semantics { contentDescription = "Cues. Tap to edit. " + ex.cues.joinToString(", ") }
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                    ) {
                        Text("CUES", color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        Text(
                            if (ex.cues.isNotEmpty()) ex.cues.joinToString(" · ") else "Add a reminder, such as \"press feet into floor\"",
                            color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, modifier = Modifier.padding(top = 2.dp),
                        )
                    }

                    TileRow(
                        listOf(
                            { m -> Tile("machine", "Machine", { app.go("exercise/${ex.id}") }, m) },
                            { m -> Tile("notes", "Notes", { app.go("note/${it.id}") }, m) },
                            { m -> Tile("tag", "Tags", { app.go("tagpick/we/${it.id}") }, m) },
                            { m -> Tile("history", "History", { app.go("load/${it.id}") }, m) },
                        ),
                    )
                    if (it.note.isNotEmpty()) Text("Note: ${it.note}", color = c.muted, fontSize = 15.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    if (menu == "exercise") {
        SpotSheet(item.exercise.name, { menu = "" }) {
            SheetRow("machine", "Edit exercise and cues") { menu = ""; app.go("exercise/${item.exercise.id}") }
            SheetRow("tag", "Tag this exercise") { menu = ""; app.go("tagpick/we/${item.id}") }
            val cur = d.cur
            if (cur != null && item.sets.size > 1) {
                SheetRow("minus", "Remove set ${d.curNo}") { menu = ""; sel = null; act { app.repo.deleteSet(cur.id) } }
            }
            if (item.status != "skipped") {
                SheetRow("close", "Skip this exercise today") {
                    menu = ""
                    app.io({ app.repo.setWeStatus(item.id, "skipped") }, { reload { goTo(safeIdx + 1) } })
                }
            }
            SheetRow("trash", "Remove from this workout") {
                menu = ""
                app.confirm = Confirm("Remove from this workout?", "Sets logged for this exercise in this workout will be removed. The exercise itself stays.", "Remove") {
                    sel = null
                    act { app.repo.removeWe(item.id) }
                }
            }
        }
    }
    if (menu == "list") {
        SpotSheet("All exercises", { menu = "" }) {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                for (i in items.indices) {
                    val it = items[i]
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable(role = Role.Button) { goTo(i) }
                            .semantics { contentDescription = "${it.exercise.name}, ${dotWord(i)}" },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { Dot(i) }
                        Text(it.exercise.name, Modifier.weight(1f), color = c.ink, fontSize = 18.sp, fontWeight = if (i == safeIdx) FontWeight.ExtraBold else FontWeight.SemiBold)
                        Text("${it.sets.count { s -> s.done }} of ${it.sets.size} sets", color = c.muted, fontSize = 14.sp)
                    }
                }
            }
            SheetRow("plus", "Add exercises") { menu = ""; app.go("add/${wk.id}") }
            SheetRow("tag", "Tag this workout") { menu = ""; app.go("tagpick/workout/${wk.id}") }
            SheetRow("check", "Finish workout") { menu = ""; finishWorkout() }
            SheetRow("trash", "Delete workout") {
                menu = ""
                app.confirm = Confirm("Delete this workout?", "Everything logged in it will be deleted. This cannot be undone.", "Delete") {
                    app.io({ app.repo.deleteWorkout(wk.id) }, { app.nav.popBackStack("start", false) })
                }
            }
        }
    }
}
