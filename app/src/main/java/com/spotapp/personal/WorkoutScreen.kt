package com.spotapp.personal

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class LogResult(val record: Boolean, val newSetId: Long?)
private class FinishResult(val sets: Int, val exercises: Int)

@Composable
fun WorkoutScreen(wid: Long) {
    val app = LocalApp.current
    val c = colors()
    val context = LocalContext.current
    val state = load("workout", wid) { getWorkout(wid) }

    var idx by remember(wid) { mutableStateOf(0) }
    var mode by remember(wid) { mutableStateOf("list") }
    var celebrate by remember(wid) { mutableStateOf<String?>(null) }
    var sel by remember(wid) { mutableStateOf<Long?>(null) }
    var menu by remember(wid) { mutableStateOf<String?>(null) }
    var hist by remember(wid) { mutableStateOf("last") }
    var dragging by remember(wid) { mutableStateOf(false) }

    val w = (state as? Load.Ready)?.value
    val items = w?.items ?: emptyList()
    val safeIdx = if (items.isNotEmpty()) minOf(idx, items.size - 1) else 0
    val item = items.getOrNull(safeIdx)
    val sets = item?.sets ?: emptyList()
    val curSet = if (item != null) (sets.firstOrNull { it.id == sel } ?: sets.firstOrNull { !it.done } ?: sets.lastOrNull()) else null
    val curNo = if (curSet != null) sets.indexOfFirst { it.id == curSet.id } + 1 else 0
    val editingDone = curSet != null && curSet.done && sel == curSet.id
    val allDone = sets.isNotEmpty() && sets.all { it.done }

    // Where I left off: the most recent earlier session of this exercise.
    val pastState = load("past", item?.exercise?.id, item?.id) {
        if (item == null) emptyList<Session>() else pastSessions(item.exercise.id, item.id)
    }
    val past = (pastState as? Load.Ready)?.value ?: emptyList()

    fun openItem(i: Int) {
        if (i < 0 || i >= items.size) return
        idx = i; sel = null; menu = null; mode = "exercise"
    }

    fun later(i: Int) {
        val ids = items.map { it.id }.toMutableList()
        if (i < 0 || i >= ids.size) return
        val one = ids.removeAt(i)
        ids.add(one)
        app.write({ reorderWorkoutExercises(wid, ids) })
    }

    fun finishWorkout() {
        app.write({
            val cur = getWorkout(wid) ?: throw InputError("This workout could not be found.")
            for (it in cur.items) if (it.status == "todo") setWeStatus(it.id, if (it.sets.any { s -> s.done }) "done" else "skipped")
            setWorkoutDone(wid, true)
            val fresh = getWorkout(wid)
            FinishResult(fresh?.items?.sumOf { i -> i.sets.count { s -> s.done } } ?: 0, fresh?.items?.count { i -> i.status == "done" } ?: 0)
        }) { r ->
            app.say("Workout finished", finishLine(app.settings["voice"] ?: "gentle", r.sets, r.exercises)) { app.home() }
        }
    }

    val doneCount = items.count { it.status == "done" }
    val todoLeft = items.count { it.status == "todo" }
    fun confirmFinish() {
        if (todoLeft == 0) { finishWorkout(); return }
        app.ask(
            "Finish the workout?",
            "$todoLeft ${if (todoLeft == 1) "exercise has" else "exercises have"} not been done. They are kept as skipped if nothing was logged.",
            "Finish",
        ) { finishWorkout() }
    }

    fun onPrimary() {
        if (item == null) return
        if (curSet == null) { app.write({ addSet(item.id) }) { sel = it }; return }
        if (!curSet.done) {
            val setId = curSet.id; val weight = curSet.weight; val exId = item.exercise.id; val weId = item.id
            val lastPlanned = sets.all { it.id == setId || it.done }
            app.write({
                val record = isNewRecord(exId, weight, setId)
                setSetDone(setId, true)
                // That was the last planned set: start the next one right away (Add set is still there too).
                var next: Long? = null
                if (lastPlanned) next = try { addSet(weId) } catch (e: InputError) { null }
                LogResult(record, next)
            }) { r ->
                sel = r.newSetId
                if (r.record) celebrate = "${item.exercise.name}: ${num(weight)} ${item.exercise.unit}"
            }
            return
        }
        if (editingDone) { sel = null; return }
        // every set is logged: back to the list, where the next one can be chosen
        app.write({ setWeStatus(item.id, "done") }) { mode = "list" }
    }

    fun bumpWeight(sign: Int) {
        if (curSet == null) return
        val step = app.settings["weightStep"]?.toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0
        val next = Math.min(Limits.MAX_WEIGHT, Math.max(0.0, Math.round((curSet.weight + sign * step) * 100) / 100.0))
        val id = curSet.id
        app.write({ updateSetWeight(id, next) })
    }

    fun bumpReps(sign: Int) {
        if (curSet == null) return
        val next = Math.min(Limits.MAX_REPS.toInt(), Math.max(0, curSet.reps + sign))
        val id = curSet.id
        app.write({ updateSetReps(id, next) })
    }

    fun finishExercise() {
        if (item == null) return
        val id = item.id
        app.write({ dropUnloggedSets(id); setWeStatus(id, "done") }) { sel = null; mode = "list" }
    }

    if (state is Load.Failed || (state is Load.Ready && w == null)) {
        ScreenFrame("Workout") { Para("This workout could not be found.") }
        return
    }
    if (w == null) { ScreenFrame("Workout") {}; return }

    // The workout actions sheet. Finish is offered only when the workout has an exercise.
    @Composable
    fun WorkoutMenu(withFinish: Boolean) {
        SpotSheet("Workout", { menu = null }) {
            SheetRow("history", "Change day, place or type") { menu = null; app.go(Route.WorkoutEdit(w.id)) }
            SheetRow("plus", "Add exercises") { menu = null; app.go(Route.AddExercises(w.id)) }
            SheetRow("tag", "Tag this workout") { menu = null; app.go(Route.TagPicker(w.id, null)) }
            if (withFinish) SheetRow("check", "Finish workout") { menu = null; confirmFinish() }
            SheetRow("trash", "Delete workout") {
                menu = null
                app.ask("Delete this workout?", "Everything logged in it will be deleted. This cannot be undone.", "Delete", destructive = true) {
                    app.write({ deleteWorkout(wid) }) { app.home() }
                }
            }
        }
    }

    if (item == null) {
        ScreenFrame(w.title, right = { CircleBtn("more", "Workout actions", { menu = "list" }) }) {
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Para("No exercises in this workout yet.")
                BigBtn("Add exercises", { app.go(Route.AddExercises(w.id)) }, Modifier.fillMaxWidth())
            }
        }
        if (menu == "list") WorkoutMenu(false)
        return
    }

    val dayPill: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Row(
                Modifier.padding(top = 6.dp).heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(c.chip)
                    .clickable(role = Role.Button) { app.go(Route.WorkoutEdit(w.id)) }
                    .semantics { contentDescription = "Workout day ${prettyDay(w.day)}. Tap to change the day, place or type." }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SpotIcon("history", c.ink, 18.dp)
                Text(prettyDay(w.day) + (if (w.locationName.isNotEmpty()) " · ${w.locationName}" else ""), color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Change", color = c.accentText, fontSize = 15.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
            }
        }
    }

    if (mode == "list") {
        ScreenFrame(
            w.title, scrollEnabled = !dragging,
            right = { CircleBtn("more", "Workout actions", { menu = "list" }) },
            bottom = {
                PinnedBar {
                    Box(
                        Modifier.weight(1f).heightIn(min = 60.dp).clip(RoundedCornerShape(30.dp)).background(if (todoLeft == 0) c.accent else c.chip)
                            .clickable(role = Role.Button, onClickLabel = "Finish workout") { confirmFinish() },
                        contentAlignment = Alignment.Center,
                    ) { Text("Finish workout", color = if (todoLeft == 0) c.onAccent else c.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
                }
            },
        ) {
            dayPill()
            ExerciseListView(
                w, doneCount, ::openItem, ::later, { ids -> app.write({ reorderWorkoutExercises(wid, ids) }) },
                { app.go(Route.AddExercises(w.id)) }, { dragging = it },
            )
        }
        if (menu == "list") WorkoutMenu(true)
        return
    }

    val ex = item.exercise
    val unit = ex.unit
    val placeId = item.locationId
    val placeName = ex.machines.firstOrNull { it.locationId == placeId }?.locationName ?: w.locationName
    val showPlaces = needsPlace(ex.equipment) && ex.machines.isNotEmpty()
    val allTags = w.tags + item.tags
    val primaryLabel = if (curSet == null) "Add a set" else if (!curSet.done) "Log set $curNo" else if (editingDone) "Done editing" else "Done with this exercise"

    ScreenFrame(
        w.title, help = "logging", cog = true,
        bottom = {
            PinnedBar {
                CircleBtn("list", "Back to the list of exercises", { sel = null; mode = "list" }, size = 56.dp, iconSize = 24.dp)
                Box(
                    Modifier.weight(1f).heightIn(min = 60.dp).clip(RoundedCornerShape(30.dp)).background(c.accent)
                        .clickable(role = Role.Button, onClickLabel = primaryLabel) { onPrimary() },
                    contentAlignment = Alignment.Center,
                ) { Text(primaryLabel, color = c.onAccent, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
            }
        },
        pad = 24.dp,
    ) {
        dayPill()
        Column(Modifier.padding(top = 6.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (ex.icon.isNotEmpty()) ExerciseBadge(ex.icon, ex.bodyParts, 44.dp) else Box(Modifier.width(44.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(ex.name, Modifier.semantics { heading() }, color = c.ink, fontFamily = Serif, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (placeName.isNotEmpty()) Text(placeName, color = c.muted, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
                }
                CircleBtn("more", "Exercise actions", { menu = "exercise" })
            }
            if (allTags.isNotEmpty()) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (t in allTags) Box(Modifier.clip(RoundedCornerShape(14.dp)).background(c.chip).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(tagLabel(t), color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            SetList(sets, curSet?.id, unit, { sel = it }) { app.write({ addSet(item.id) }) { sel = it } }
            if (curSet != null) {
                SetEditor(
                    curSet, unit, app.settings["weightStep"]?.toDoubleOrNull() ?: 1.0, ::bumpWeight, ::bumpReps,
                    { n -> val id = curSet.id; app.write({ updateSetWeight(id, n) }) },
                    { n -> val id = curSet.id; app.write({ updateSetReps(id, n.toInt()) }) },
                )
            }
            if (sets.any { it.done } && !allDone) {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).background(c.chip)
                        .clickable(role = Role.Button, onClickLabel = "Done with this exercise") { finishExercise() },
                    contentAlignment = Alignment.Center,
                ) { Text("Done with this exercise", color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            }
            if (showPlaces) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Caption("Done at today")
                    MultiChips(ex.machines.map { Option(it.locationId, it.locationName) }, if (placeId == null) emptyList() else listOf(placeId), { lid ->
                        app.write({ setWeLocation(item.id, lid) })
                    })
                }
            }
            PastPanel(past, placeId, hist, { hist = it }, unit, { weId -> app.write({ loadSession(item.id, weId) }) }, { app.go(Route.LoadPast(item.id)) })
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.cue)
                    .clickable(role = Role.Button, onClickLabel = "Edit cues") { app.go(Route.ExerciseEdit(ex.id)) }
                    .semantics { contentDescription = "Cues. Tap to edit." }.padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Caption("Cues")
                Text(
                    if (ex.cues.isNotEmpty()) ex.cues.joinToString(" · ") else "Add a reminder, such as \"press feet into floor\"",
                    color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
            TileRow {
                Tile("machine", "Exercise", { app.go(Route.ExerciseEdit(ex.id)) }, Modifier.weight(1f))
                Tile("notes", "Notes", { app.go(Route.Note(item.id)) }, Modifier.weight(1f))
                Tile("tag", "Tags", { app.go(Route.TagPicker(null, item.id)) }, Modifier.weight(1f))
                Tile("history", "History", { app.go(Route.LoadPast(item.id)) }, Modifier.weight(1f))
                if (ex.link.isNotEmpty()) Tile("link", "Link", { openLink(context, ex.link, app) }, Modifier.weight(1f))
            }
            if (item.note.isNotEmpty()) Text("Note: ${item.note}", color = c.muted, fontSize = 15.sp, lineHeight = 21.sp)
        }
    }

    if (menu == "exercise") SpotSheet(ex.name, { menu = null }) {
        SheetRow("machine", "Edit exercise and cues") { menu = null; app.go(Route.ExerciseEdit(ex.id)) }
        SheetRow("tag", "Tag this exercise") { menu = null; app.go(Route.TagPicker(null, item.id)) }
        if (curSet != null && sets.size > 1) SheetRow("minus", "Remove set $curNo") {
            menu = null
            val id = curSet.id
            app.write({ deleteSet(id) }) { sel = null }
        }
        if (item.status == "todo" && safeIdx < items.size - 1) SheetRow("history", "Do this one later (machine busy)") { menu = null; later(safeIdx); mode = "list" }
        if (item.status != "skipped") SheetRow("close", "Skip this exercise today") {
            menu = null
            app.write({ setWeStatus(item.id, "skipped") }) { mode = "list" }
        }
        SheetRow("trash", "Remove from this workout") {
            menu = null
            app.ask("Remove from this workout?", "Sets logged for this exercise in this workout will be removed. The exercise itself stays.", "Remove", destructive = true) {
                app.write({ removeWe(item.id) }) { sel = null; mode = "list" }
            }
        }
    }
    val cel = celebrate
    if (cel != null) Celebration(cel) { celebrate = null }
}
