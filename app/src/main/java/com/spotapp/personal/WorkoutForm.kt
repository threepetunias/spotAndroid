package com.spotapp.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// A month calendar. Any past, present or future day can be picked. Weeks start on Sunday, like the iPhone app.
@Composable
fun CalendarPicker(value: String, onChange: (String) -> Unit) {
    val c = colors()
    val start = LocalDate.parse(value)
    var year by remember { mutableStateOf(start.year) }
    var month by remember { mutableStateOf(start.monthValue - 1) }
    val today = todayStr()

    val first = LocalDate.of(year, month + 1, 1)
    val lead = first.dayOfWeek.value % 7 // Sunday = 0
    val count = first.lengthOfMonth()
    val cells = ArrayList<String?>()
    repeat(lead) { cells.add(null) }
    for (d in 1..count) cells.add(first.withDayOfMonth(d).toString())
    while (cells.size % 7 != 0) cells.add(null)

    fun shift(n: Int) {
        val d = first.plusMonths(n.toLong())
        year = d.year; month = d.monthValue - 1
    }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircleBtn("back", "Previous month", { shift(-1) })
            Text(monthName(year, month), Modifier.weight(1f).semantics { heading() }, color = c.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Box(Modifier.rotate(180f)) { CircleBtn("back", "Next month", { shift(1) }) }
        }
        Row(Modifier.padding(top = 8.dp)) {
            val order = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
            for (d in order) Text(
                d.getDisplayName(TextStyle.NARROW, Locale.ENGLISH), Modifier.weight(1f).semantics { contentDescription = d.getDisplayName(TextStyle.FULL, Locale.getDefault()) },
                color = c.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            )
        }
        for (r in cells.chunked(7)) Row(Modifier.fillMaxWidth()) {
            for (day in r) {
                if (day == null) { Box(Modifier.weight(1f).heightIn(min = 46.dp)); continue }
                val sel = day == value
                val isToday = day == today
                val n = day.substring(8).toInt()
                Box(
                    Modifier.weight(1f).heightIn(min = 46.dp)
                        .clickable(role = Role.Button) { onChange(day) }
                        .semantics {
                            this.selected = sel
                            contentDescription = LocalDate.parse(day).let { it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) + ", " + prettyDay(day) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(if (sel) c.accent else androidx.compose.ui.graphics.Color.Transparent)
                            .then(if (isToday && !sel) Modifier.border(2.dp, c.accentText, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { Text("$n", color = if (sel) c.onAccent else c.ink, fontSize = 17.sp, fontWeight = if (sel || isToday) FontWeight.Bold else FontWeight.Medium) }
                }
            }
        }
        Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button) {
            val d = LocalDate.parse(today); year = d.year; month = d.monthValue - 1; onChange(today)
        }, contentAlignment = Alignment.Center) { Text("Today", color = c.accentText, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

// The day, place and type of a workout. Used when making a workout and when changing one.
@Composable
fun WorkoutForm(
    day: String, onDay: (String) -> Unit,
    locationId: Long?, onLocation: (Long?) -> Unit, locations: List<Location>,
    typeId: Long?, onType: (Long?) -> Unit, types: List<WorkoutType>,
) {
    val app = LocalApp.current
    var adding by remember { mutableStateOf<String?>(null) }
    Column {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Day · ${prettyDay(day)}")
            CalendarPicker(day, onDay)
        }
        Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Where")
            ChoiceChips(locations.map { Option(it.id, it.name) }, locationId, onLocation, clearable = true, addLabel = "New place", onAdd = { adding = "place" })
        }
        Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Type of workout")
            TypeTiles(types.map { Option(it.id, it.name, it.icon) }, typeId, onType, addLabel = "New", onAdd = { adding = "type" })
        }
    }
    val kind = adding
    if (kind != null) PromptDialog(
        title = if (kind == "type") "New workout type" else "New place",
        maxLength = if (kind == "type") Limits.TYPE_NAME else Limits.LOCATION,
        placeholder = if (kind == "type") "Strength" else "Riverside Gym",
        onCancel = { adding = null },
        onSubmit = { text ->
            if (kind == "place") app.write({ saveLocation(null, text, guessPlaceIcon(text), "") }, onFail = {}) { onLocation(it); adding = null }
            else app.write({ saveWorkoutType(null, text, "") }) { onType(it); adding = null }
        },
    )
}

@Composable
fun ExercisePicker(exercises: List<Exercise>, selected: List<Long>, onToggle: (Long) -> Unit, onNew: () -> Unit) {
    val c = colors()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (exercises.isEmpty()) Text("No exercises yet. Add the first one below.", color = c.muted, fontSize = 17.sp, lineHeight = 24.sp)
        for (e in exercises) {
            val idx = selected.indexOf(e.id)
            val on = idx >= 0
            val detail = (listOf(if (e.movement.isNotEmpty()) MOVEMENT_LABELS[e.movement] ?: "" else "") + e.machines.map { it.locationName }).filter { it.isNotEmpty() }.joinToString(" · ")
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(16.dp)).background(c.card)
                    .border(2.dp, if (on) c.nowEdge else c.card, RoundedCornerShape(16.dp))
                    .clickable(role = Role.Checkbox, onClickLabel = if (on) "Remove ${e.name}" else "Choose ${e.name}") { onToggle(e.id) }
                    .semantics { contentDescription = e.name + (if (detail.isNotEmpty()) ", $detail" else "") + (if (on) ", chosen, number ${idx + 1}" else "") }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(if (on) c.nowFill else c.chip), contentAlignment = Alignment.Center) {
                    if (on) Text("${idx + 1}", color = c.onNowFill, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) else SpotIcon("plus", c.ink, 16.dp)
                }
                if (e.icon.isNotEmpty()) ExerciseBadge(e.icon, e.bodyParts, 36.dp)
                Column(Modifier.weight(1f)) {
                    Text(e.name, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    if (detail.isNotEmpty()) Text(detail, color = c.muted, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        Box(Modifier.padding(top = 4.dp)) { DashedAdd("New exercise", "New exercise", onNew, height = 54) }
    }
}

@Composable
fun WorkoutFormScreen(editId: Long?) {
    val app = LocalApp.current
    val c = colors()
    var day by remember(editId) { mutableStateOf(todayStr()) }
    var locationId by remember(editId) { mutableStateOf<Long?>(null) }
    var typeId by remember(editId) { mutableStateOf<Long?>(null) }
    var selected by remember(editId) { mutableStateOf<List<Long>>(emptyList()) }
    var busy by remember(editId) { mutableStateOf(false) }
    var loaded by remember(editId) { mutableStateOf(editId == null) }

    class FormData(val locations: List<Location>, val types: List<WorkoutType>, val exercises: List<Exercise>, val past: List<WorkoutRow>, val current: WorkoutFull?)
    val state = load("workoutForm", editId) {
        FormData(
            listLocations(), listWorkoutTypes(), if (editId == null) listExercises() else emptyList(),
            if (editId == null) listWorkouts().filter { it.exCount > 0 && it.day <= todayStr() }.take(3) else emptyList(),
            if (editId != null) getWorkout(editId) else null,
        )
    }
    val d = (state as? Load.Ready)?.value
    // The details of an existing workout are copied in once, so later reloads never overwrite what the person is typing.
    LaunchedEffect(d != null) {
        if (d != null && editId != null && !loaded) {
            val cur = d.current
            if (cur != null) { day = cur.day; locationId = cur.locationId; typeId = cur.typeId }
            loaded = true
        }
    }

    fun copyFrom(w: WorkoutRow) {
        val exs = d?.exercises ?: return
        val locs = d.locations; val ts = d.types
        app.read({ getWorkout(w.id) }) { full ->
            if (full != null) {
                selected = full.items.map { it.exercise.id }.filter { id -> exs.any { it.id == id } }
                if (full.locationId != null && locs.any { it.id == full.locationId }) locationId = full.locationId
                if (full.typeId != null && ts.any { it.id == full.typeId }) typeId = full.typeId
            }
        }
    }

    fun save() {
        if (busy) return
        busy = true
        if (editId == null) {
            val ids = selected
            app.write({ createWorkout(day, locationId, typeId, ids) }, onFail = { busy = false }) { id -> app.replace(Route.Workout(id)) }
        } else {
            app.write({ updateWorkoutMeta(editId, day, locationId, typeId) }, onFail = { busy = false }) { app.back() }
        }
    }

    ScreenFrame(
        if (editId == null) "New workout" else "Workout details",
        bottom = {
            PinnedBar {
                BigBtn("Cancel", { app.back() }, Modifier.weight(1f), secondary = true)
                BigBtn(if (editId != null || day != todayStr()) "Save" else "Start", { save() }, Modifier.weight(1f), enabled = !busy && loaded)
            }
        },
    ) {
        Loaded(state) { data ->
            if (loaded) Column(Modifier.padding(top = 12.dp, bottom = 24.dp)) {
                WorkoutForm(day, { day = it }, locationId, { locationId = it }, data.locations, typeId, { typeId = it }, data.types)
                if (editId == null) {
                    if (data.past.isNotEmpty()) Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Caption("Copy exercises from")
                        for (w in data.past) Row { Chip("${w.title} · ${prettyDay(w.day)}", false, { copyFrom(w) }) }
                    }
                    Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Caption("Exercises · ${selected.size} chosen")
                        ExercisePicker(data.exercises, selected, { id -> selected = if (id in selected) selected - id else selected + id }, { app.go(Route.ExerciseEdit(null)) })
                    }
                    Text("The workout is named for you from the day and place. You can add or remove exercises later from the workout screen.", color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 20.dp))
                }
            }
        }
    }
}

@Composable
fun AddExercisesScreen(workoutId: Long) {
    val app = LocalApp.current
    var selected by remember { mutableStateOf<List<Long>>(emptyList()) }
    val state = load("addEx") { listExercises() }
    ScreenFrame(
        "Add exercises",
        bottom = {
            PinnedBar {
                BigBtn("Cancel", { app.back() }, Modifier.weight(1f), secondary = true)
                BigBtn("Add", {
                    val ids = selected
                    app.write({ addExercisesToWorkout(workoutId, ids) }) { app.back() }
                }, Modifier.weight(1f), enabled = selected.isNotEmpty())
            }
        },
    ) {
        Loaded(state) { list ->
            Column(Modifier.padding(top = 16.dp, bottom = 24.dp)) {
                ExercisePicker(list, selected, { id -> selected = if (id in selected) selected - id else selected + id }, { app.go(Route.ExerciseEdit(null)) })
            }
        }
    }
}
