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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect

@Composable
fun OnResume(key: Any? = Unit, block: () -> Unit) {
    LifecycleResumeEffect(key) {
        block()
        onPauseOrDispose { }
    }
}

// ---------- Start ----------

@Composable
fun StartScreen() {
    val app = LocalApp.current
    val c = colors()
    var rows by remember { mutableStateOf<List<WorkoutRow>?>(null) }
    OnResume { app.io({ app.repo.listWorkouts() }, { rows = it }) }

    val today = todayStr()
    val tomorrow = addDays(today, 1)
    val list = rows ?: emptyList()
    val sections = listOf(
        "Today" to list.filter { it.day == today },
        "Tomorrow" to list.filter { it.day == tomorrow },
        "Coming up" to list.filter { it.day > tomorrow }.reversed(),
        "Past" to list.filter { it.day < today }.take(30),
    )

    ScreenFrame(
        title = "Spot", back = false, help = true, cog = true,
        bottom = { PinnedBar { BigBtn("New workout", { app.go("workout/new") }, Modifier.weight(1f)) } },
    ) {
        if (rows != null && rows!!.isEmpty()) {
            Column(
                Modifier.padding(top = 28.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.card).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Ready when you are", color = c.ink, fontFamily = Serif, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Start a workout and pick exercises. Spot fills in where you left off, so there is nothing to copy.",
                    color = c.muted, fontSize = 17.sp, lineHeight = 24.sp,
                )
            }
        }
        for ((title, items) in sections) {
            if (items.isEmpty()) continue
            Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption(title)
                for (w in items) {
                    val sub = (if (title == "Today" || title == "Tomorrow") "" else "${prettyDay(w.day)} · ") +
                        "${w.exDone} of ${w.exCount} ${if (w.exCount == 1) "exercise" else "exercises"}"
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(18.dp)).background(c.card)
                            .clickable(role = Role.Button, onClickLabel = "Open ${w.title}") { app.go("workout/${w.id}") }
                            .semantics { contentDescription = "${w.title}, ${prettyDay(w.day)}, ${w.exDone} of ${w.exCount} exercises done" }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(w.title, color = c.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(sub, color = c.muted, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        if (w.done) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                SpotIcon("check", c.ok, 18.dp)
                                Text("Finished", color = c.ok, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        TileRow(
            listOf(
                { m -> Tile("dumbbell", "Exercises", { app.go("exercises") }, m) },
                { m -> Tile("tag", "Tags", { app.go("tags") }, m) },
                { m -> Tile("history", "Back up", { app.go("settings/backup") }, m) },
            ),
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ---------- Welcome ----------

@Composable
fun WelcomeScreen() {
    val app = LocalApp.current
    val s = app.settings
    ScreenFrame(
        title = "Spot", back = false,
        bottom = { PinnedBar { BigBtn("Get started", { app.setSetting("onboarded", "1"); app.back() }, Modifier.weight(1f)) } },
    ) {
        Spacer(Modifier.height(24.dp))
        Para("Log weights, sets and reps. Spot remembers where you left off, so you never copy last week's notes again.")
        Heading("Color")
        ChipRows(listOf(
            chipItem("Lagoon", s["palette"] == "lagoon", { app.setSetting("palette", "lagoon") }),
            chipItem("Citrus", s["palette"] == "citrus", { app.setSetting("palette", "citrus") }),
        ))
        Heading("Light or dark")
        ChipRows(listOf(
            chipItem("Paper", s["theme"] == "paper", { app.setSetting("theme", "paper") }),
            chipItem("Evening", s["theme"] == "evening", { app.setSetting("theme", "evening") }),
            chipItem("Match phone", s["theme"] == "system", { app.setSetting("theme", "system") }),
        ))
        Heading("Good to know", 18)
        Para(
            "Everything stays on this phone. There are no accounts. Spot is not a medical device and does not diagnose, treat, cure, or prevent any medical condition. Check with a healthcare professional before changing how you exercise.",
            muted = true,
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ---------- Help ----------

private val HELP_TOPICS = listOf(
    "Starting a workout" to "Tap New workout, give it a name, pick a day and choose exercises. You can copy the exercises from a past workout. Spot fills in the sets from the last time you did each exercise.",
    "Logging sets" to "Each set is a row. Use the minus and plus buttons, or tap the number to type it. Tap Log set when you finish one. Tap Add set for another. Tap any row to change it.",
    "Where I left off" to "Tap Load next to Last time, or the History tile. Choose the last time, the last full-strength session, the last one with a tag, your best, or any past session.",
    "Cues" to "Cues are short reminders such as \"press feet into floor\". They show on the exercise screen. Edit them from the Machine tile or the Exercises page.",
    "Tags" to "Tag a whole workout or a single exercise, for example a health issue and how strong it was, from 1 to 5. Tags help you look back later. They never leave your phone.",
    "Back up" to "Settings, Back up saves a file you keep. Restore replaces what is on this phone with the file.",
    "Not a medical device" to "Spot is not a medical device and does not diagnose, treat, cure, or prevent any medical condition. Talk to a doctor or other healthcare professional about pain, injury or health concerns.",
)

@Composable
fun HelpScreen() {
    val app = LocalApp.current
    ScreenFrame(title = "Help", cog = true) {
        for ((t, b) in HELP_TOPICS) {
            Heading(t, 20)
            Para(b)
        }
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth()) { BigBtn("Replay the welcome", { app.go("welcome") }, Modifier.weight(1f), secondary = true) }
        Spacer(Modifier.height(32.dp))
    }
}

// ---------- Exercise picker (New workout and Add exercises) ----------

@Composable
fun ExercisePicker(exercises: List<Exercise>, selected: List<Long>, onToggle: (Long) -> Unit, onNew: () -> Unit) {
    val c = colors()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (exercises.isEmpty()) Text("No exercises yet. Add the first one below.", color = c.muted, fontSize = 17.sp, lineHeight = 24.sp)
        for (e in exercises) {
            val idx = selected.indexOf(e.id)
            val on = idx >= 0
            val detail = listOf(e.machine, e.gym).filter { it.isNotEmpty() }.joinToString(" · ")
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(16.dp)).background(c.card)
                    .border(2.dp, if (on) c.nowEdge else c.card, RoundedCornerShape(16.dp))
                    .clickable(role = Role.Checkbox) { onToggle(e.id) }
                    .semantics { contentDescription = e.name + (if (detail.isNotEmpty()) ", $detail" else "") + (if (on) ", chosen as number ${idx + 1}" else "") }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(if (on) c.nowFill else c.chip), contentAlignment = Alignment.Center) {
                    if (on) Text("${idx + 1}", color = c.onNowFill, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    else SpotIcon("plus", c.ink, 16.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(e.name, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (detail.isNotEmpty()) Text(detail, color = c.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Row(Modifier.padding(top = 6.dp).fillMaxWidth()) { BigBtn("New exercise", onNew, Modifier.weight(1f), secondary = true) }
    }
}

// ---------- New workout ----------

@Composable
fun NewWorkoutScreen() {
    val app = LocalApp.current
    val c = colors()
    var title by remember { mutableStateOf("Workout") }
    var offset by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf(listOf<Long>()) }
    var exercises by remember { mutableStateOf(listOf<Exercise>()) }
    var past by remember { mutableStateOf(listOf<WorkoutRow>()) }
    var busy by remember { mutableStateOf(false) }

    OnResume {
        app.io({ app.repo.listExercises() }, { exercises = it })
        app.io({ app.repo.listWorkouts() }, { r -> past = r.filter { it.exCount > 0 && it.day <= todayStr() }.take(3) })
    }

    val day = addDays(todayStr(), offset.toLong())
    ScreenFrame(
        title = "New workout",
        bottom = {
            PinnedBar {
                BigBtn("Cancel", { app.back() }, Modifier.weight(1f), secondary = true)
                BigBtn(
                    if (offset == 0) "Start" else "Save",
                    {
                        if (!busy) {
                            busy = true
                            val t = title
                            val ids = selected
                            app.io({ app.repo.createWorkout(t, day, ids) }, { id ->
                                app.nav.navigate("workout/$id") { popUpTo("workout/new") { inclusive = true } }
                            }, { busy = false })
                        }
                    },
                    Modifier.weight(1f), enabled = !busy,
                )
            }
        },
    ) {
        Field("Name", title, { title = it }, max = Limits.TITLE)
        Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Day · ${prettyDay(day)}")
            ChipRows(
                listOf("Today" to 0, "Tomorrow" to 1, "In 2 days" to 2, "In a week" to 7).map { (label, n) ->
                    chipItem(label, offset == n, { offset = n })
                },
            )
        }
        if (past.isNotEmpty()) {
            Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption("Copy exercises from")
                for (w in past) {
                    Chip("${w.title} · ${prettyDay(w.day)}", false, {
                        app.io({ app.repo.getWorkout(w.id) }, { full ->
                            if (full != null) {
                                val ok = exercises.map { it.id }.toSet()
                                selected = full.items.map { it.exercise.id }.filter { it in ok }
                                title = full.title
                            }
                        })
                    }, Modifier.fillMaxWidth())
                }
            }
        }
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Exercises · ${selected.size} chosen")
            ExercisePicker(exercises, selected, { id -> selected = if (id in selected) selected - id else selected + id }, { app.go("exercise/new") })
        }
        Text("You can add or remove exercises later from the workout screen.", color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 20.dp, bottom = 20.dp))
    }
}

// ---------- Add exercises to a workout ----------

@Composable
fun AddExercisesScreen(workoutId: Long?) {
    val app = LocalApp.current
    var exercises by remember { mutableStateOf(listOf<Exercise>()) }
    var selected by remember { mutableStateOf(listOf<Long>()) }
    OnResume { app.io({ app.repo.listExercises() }, { exercises = it }) }
    ScreenFrame(
        title = "Add exercises",
        bottom = {
            PinnedBar {
                BigBtn("Cancel", { app.back() }, Modifier.weight(1f), secondary = true)
                BigBtn("Add", {
                    val id = workoutId
                    val ids = selected
                    if (id == null) app.back() else app.io({ app.repo.addExercisesToWorkout(id, ids) }, { app.back() })
                }, Modifier.weight(1f), enabled = selected.isNotEmpty())
            }
        },
    ) {
        Spacer(Modifier.height(16.dp))
        ExercisePicker(exercises, selected, { id -> selected = if (id in selected) selected - id else selected + id }, { app.go("exercise/new") })
        Spacer(Modifier.height(24.dp))
    }
}
