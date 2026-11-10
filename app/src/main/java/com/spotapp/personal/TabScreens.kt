package com.spotapp.personal

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ---------- Log ----------

private class LogData(val rows: List<WorkoutRow>, val tags: List<Tag>, val locs: List<Location>, val types: List<WorkoutType>)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogScreen() {
    val app = LocalApp.current
    val c = colors()
    var text by remember { mutableStateOf("") }
    var typed by remember { mutableStateOf("") }
    var tagId by remember { mutableStateOf<Long?>(null) }
    var placeId by remember { mutableStateOf<Long?>(null) }
    var typeId by remember { mutableStateOf<Long?>(null) }
    var from by remember { mutableStateOf<String?>(null) }
    var to by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf(false) }
    var openMonths by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    LaunchedEffect(text) { delay(250); typed = text }

    val state = load("log", typed, tagId, placeId, typeId, from, to) {
        val tags = listTags()
        val pickedTag = tagId
        val ids = if (pickedTag == null) emptyList() else tagWithChildren(tags, pickedTag)
        LogData(searchWorkouts(LogFilter(typed, ids, from, to, placeId, typeId)), tags, listLocations(), listWorkoutTypes())
    }
    val d = (state as? Load.Ready)?.value
    val active = (if (tagId != null) 1 else 0) + (if (placeId != null) 1 else 0) + (if (typeId != null) 1 else 0) + (if (from != null) 1 else 0) + (if (to != null) 1 else 0)
    val searching = typed.trim().isNotEmpty() || active > 0

    ScreenFrame("Log", back = false, help = "log", cog = true, insights = true, bottom = { TabBar(Route.Log) }) {
        Field("Search", text, { text = it }, max = 60, placeholder = "Title, exercise, note or place")
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Chip(if (open) "Hide filters" else if (active > 0) "Filters ($active)" else "Filters", active > 0, { open = !open })
            if (active > 0) Chip("Clear", false, { tagId = null; placeId = null; typeId = null; from = null; to = null; picking = null })
        }
        if (open && d != null) Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (d.tags.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption("Tag or sub-tag")
                ChipRows { for (t in d.tags) Chip((if (t.depth > 1) "· ".repeat(t.depth - 1) else "") + t.name, tagId == t.id, { tagId = if (tagId == t.id) null else t.id }) }
                Text("Choosing a group also finds everything under it.", color = c.muted, fontSize = 14.sp)
            }
            if (d.locs.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption("Place")
                ChoiceChips(d.locs.map { Option(it.id, it.name) }, placeId, { placeId = it }, clearable = true)
            }
            if (d.types.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption("Type")
                ChoiceChips(d.types.map { Option(it.id, it.name) }, typeId, { typeId = it }, clearable = true)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption("Dates")
                ChipRows {
                    val fromDay = from
                    val toDay = to
                    Chip(if (fromDay != null) "From ${prettyDay(fromDay)}" else "From", fromDay != null || picking == "from", { picking = if (picking == "from") null else "from" })
                    Chip(if (toDay != null) "To ${prettyDay(toDay)}" else "To", toDay != null || picking == "to", { picking = if (picking == "to") null else "to" })
                }
                val pk = picking
                if (pk != null) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalendarPicker((if (pk == "from") from else to) ?: todayStr()) { day -> if (pk == "from") from = day else to = day; picking = null }
                    Row { BigBtn(if (pk == "from") "No start date" else "No end date", { if (pk == "from") from = null else to = null; picking = null }, secondary = true) }
                }
            }
        }
        Loaded(state) { data ->
            if (data.rows.isEmpty()) Text(if (searching) "No workouts match." else "Finished and planned workouts will be listed here.", color = c.muted, fontSize = 17.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 24.dp))
            val groups = groupByMonth(data.rows) { it.day }
            groups.forEachIndexed { gi, g ->
                val shown = isMonthOpen(searching, openMonths, g.month, gi)
                Column(Modifier.padding(top = 18.dp)) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .clickable(enabled = !searching, role = androidx.compose.ui.semantics.Role.Button) { openMonths = openMonths + (g.month to !isMonthOpen(false, openMonths, g.month, gi)) }
                            .semantics { contentDescription = "${g.month}, ${g.items.size} ${if (g.items.size == 1) "workout" else "workouts"}. ${if (shown) "Collapse" else "Expand"}." },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.rotate(if (shown) -90f else 180f)) { SpotIcon("back", if (searching) c.ink.copy(alpha = 0.4f) else c.ink, 18.dp) }
                        Text(g.month, Modifier.weight(1f), color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${g.items.size} ${if (g.items.size == 1) "workout" else "workouts"}", color = c.muted, fontSize = 15.sp)
                    }
                    if (shown) Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (w in g.items) {
                            val del = {
                                app.ask("Delete this workout?", "\"${w.title}\" and everything logged in it will be deleted. This cannot be undone.", "Delete", destructive = true) {
                                    app.write({ deleteWorkout(w.id) })
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(18.dp)).background(c.card)
                                    .combinedClickable(onClick = { app.go(Route.Workout(w.id)) }, onLongClick = { del() }, onClickLabel = "Open ${w.title}", onLongClickLabel = "Delete")
                                    .semantics {
                                        contentDescription = "${w.title}, ${prettyDay(w.day)}, ${w.exDone} of ${w.exCount} exercises done"
                                        customActions = listOf(CustomAccessibilityAction("Delete workout") { del(); true })
                                    }.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (w.icon.isNotEmpty()) WorkoutPicture(w.icon, 44.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(w.title, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (w.locationName.isNotEmpty()) PlaceBadge(w.placeIcon, w.placeColor, 16.dp)
                                        Text(listOf(prettyDay(w.day), w.typeName, w.locationName).filter { it.isNotEmpty() }.joinToString(" · "), color = c.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                if (w.done) SpotIcon("check", c.ok, 20.dp)
                            }
                        }
                    }
                }
            }
            if (data.rows.size >= LOG_LIMIT) Text("Showing the newest $LOG_LIMIT workouts. Use the filters to find older ones.", color = c.muted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 20.dp))
            if (data.rows.isNotEmpty()) Text("Press and hold a workout to delete it.", color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 20.dp, bottom = 24.dp))
        }
    }
}

// ---------- Insights ----------

private val INSIGHT_PAGES = listOf("Overview", "Days trained", "Push and pull", "Body parts")

private class InsightData(
    val weeks: List<WeekPoint>, val monthDays: Int, val lastMonthDays: Int, val parts: Map<String, Int>,
    val dots: List<DayDot>, val push: Int, val pull: Int,
)

@Composable
private fun InsightCard(title: String, caption: String, content: @Composable () -> Unit) {
    val c = colors()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Caption(title)
        content()
        Text(caption, color = c.muted, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

// Trends over weeks. Numbers appear only when there is something real to show.
@Composable
fun InsightsScreen() {
    val app = LocalApp.current
    val c = colors()
    var page by remember { mutableStateOf(0) }
    val unit = if (app.settings["unit"] == "kg") "kg" else "lb"
    val state = load("insights") {
        val today = todayStr()
        val w = weekSeries(8, today)
        val m0 = periodRange(Period.MONTH, 0, today)
        val m1 = periodRange(Period.MONTH, -1, today)
        val d0 = trainedDays(m0.from, m0.to)
        val d1 = trainedDays(m1.from, m1.to)
        val parts = setsByPart(m0.from, m0.to)
        val monday = periodRange(Period.WEEK, -4, today).from
        val span = trainedDays(monday, addDays(monday, 34))
        val byDay = span.associateBy { it.day }
        val grid = (0 until 35).map { i ->
            val day = addDays(monday, i.toLong())
            val t = byDay[day]
            DayDot(day, t?.color ?: "", t != null && day <= today)
        }
        val pp = pushPull(w.firstOrNull()?.from ?: monday, w.lastOrNull()?.to ?: today)
        InsightData(w, d0.map { it.day }.toSet().size, d1.map { it.day }.toSet().size, parts, grid, pp.first, pp.second)
    }
    val next = (page + 1) % INSIGHT_PAGES.size
    ScreenFrame(INSIGHT_PAGES[page], help = "insights", cog = true, bottom = { TabBar(null) }, pad = 16.dp) {
        Loaded(state) { d ->
            val nothing = d.weeks.all { it.days == 0 } && d.monthDays == 0
            Column(Modifier.padding(top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (nothing) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card).padding(18.dp)) {
                    Text("Insights fill in as you log sets", color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Finish a few workouts and trends over the weeks will show up here.", color = c.muted, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.padding(top = 6.dp))
                } else when (page) {
                    0 -> {
                        val vols = d.weeks.map { it.volume }
                        val diff = d.monthDays - d.lastMonthDays
                        val compare = if (d.lastMonthDays == 0) "First month with workouts" else if (diff == 0) "Same as last month" else if (diff > 0) "$diff more than last month" else "${d.lastMonthDays} last month"
                        InsightCard("Workouts this month", "Days with at least one logged set.") {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("${d.monthDays}", color = c.accentText, fontFamily = Serif, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                                Column(Modifier.weight(1f)) {
                                    Text(if (d.monthDays == 1) "day trained" else "days trained", color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                    Text(compare, color = c.muted, fontSize = 14.sp)
                                }
                            }
                        }
                        if (vols.count { it > 0 } >= 2) InsightCard("Weight lifted, last 8 weeks", "Total weight lifted each week, in $unit.") {
                            LineChart(vols, d.weeks.first().from.substring(5).replace('-', '/'), "This week")
                        }
                        if (d.weeks.any { w -> w.sets.values.sum() > 0 }) InsightCard("Sets by body part", "Each column is one week, newest on the right. Colors match Body parts.") {
                            StackedColumns(d.weeks.takeLast(6).map { it.sets }, "6 weeks")
                        }
                    }
                    1 -> {
                        val n = d.dots.count { it.trained }
                        InsightCard("Last 5 weeks", "A filled dot is a day with at least one logged set, in the color of its place. Empty dots are rest days.") { DayDots(d.dots) }
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card).padding(14.dp)) {
                            Text("$n ${if (n == 1) "day" else "days"}", color = c.accentText, fontFamily = Serif, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Text("trained in the last 5 weeks. Every one counts; gaps are just rests.", color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    2 -> {
                        val total = d.push + d.pull
                        val tip = balanceTip(d.push, d.pull)
                        val center = if (!enoughForPercent(total)) "${d.push} of $total" else "${tip.pushPct}%"
                        InsightCard(
                            "Last 8 weeks",
                            if (total == 0) "Mark exercises as push or pull to see this." else if (enoughForPercent(total)) "Share of push sets out of all push and pull sets." else "Push sets out of all push and pull sets so far.",
                        ) {
                            if (total != 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Ring(d.push.toDouble() / total, c.accent, center, "push")
                                Column(Modifier.weight(1f)) {
                                    Text(tip.title, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                    Text(tip.tip, color = c.muted, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                    else -> {
                        val rows = BODY_PARTS.map { it to (d.parts[it] ?: 0) }.filter { it.second > 0 }.sortedByDescending { it.second }
                        val max = maxOf(1, rows.maxOfOrNull { it.second } ?: 1)
                        InsightCard("Sets this month", "Sets logged for each body part this month.") {
                            if (rows.isEmpty()) Text("Choose body parts for your exercises to see them here.", color = c.muted, fontSize = 16.sp)
                            for ((p, n) in rows) Row(Modifier.heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                BodyBadge(p, 32.dp)
                                Text(BODY_PART_LABELS[p] ?: p, Modifier.padding(0.dp).then(Modifier.heightIn()), color = c.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Box(Modifier.weight(1f).heightIn(min = 12.dp).clip(RoundedCornerShape(6.dp)).background(c.chip)) {
                                    Box(Modifier.fillMaxWidth(maxOf(0.06f, n.toFloat() / max)).heightIn(min = 12.dp).clip(RoundedCornerShape(6.dp)).background(bodyColor(p)))
                                }
                                Text("$n", color = c.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.accent)
                        .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "Next page") { page = next }
                        .semantics { contentDescription = "Next: ${INSIGHT_PAGES[next]}, ${next + 1} of ${INSIGHT_PAGES.size}" }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Next: ${INSIGHT_PAGES[next]}", color = c.onAccent.copy(alpha = 0.85f), fontSize = 13.sp)
                        Text("${next + 1} of ${INSIGHT_PAGES.size}", color = c.onAccent, fontFamily = Serif, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.rotate(180f)) { SpotIcon("back", c.onAccent, 22.dp) }
                }
            }
        }
    }
}

// ---------- Timer ----------

private val PRESETS = listOf(30, 60, 90, 120)
private fun clock(s: Int): String = "${s / 60}:${if (s % 60 < 10) "0" else ""}${s % 60}"
private fun secLabel(s: Int): String = if (s < 120) "$s s" else "${s / 60} min"

private fun buzz(context: Context) {
    try {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 250, 500, 250, 500), -1))
    } catch (e: Exception) { /* no vibration on this phone */ }
}

private fun stopBuzz(context: Context) {
    try {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        v?.cancel()
    } catch (e: Exception) { /* nothing to stop on this phone */ }
}

// Rest timer. It counts down from the end time (not by ticking), so it stays right if the screen is slow.
// It vibrates and flashes when it finishes while the app is open. It does not send notifications.
@Composable
fun TimerScreen() {
    val c = colors()
    val context = LocalContext.current
    var total by remember { mutableStateOf(60) }
    var endAt by remember { mutableStateOf<Long?>(null) }
    var left by remember { mutableStateOf(60) }
    var finished by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf("") }
    val flash = remember { Animatable(0f) }

    LaunchedEffect(endAt) {
        val end = endAt ?: return@LaunchedEffect
        while (true) {
            val r = Math.ceil((end - System.currentTimeMillis()) / 1000.0).toInt()
            if (r <= 0) break
            left = r
            delay(200)
        }
        endAt = null; left = 0; finished = true
        buzz(context)
        repeat(6) { flash.animateTo(1f, tween(350)); flash.animateTo(0f, tween(350)) }
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { stopBuzz(context) } }

    fun start(secs: Int) { stopBuzz(context); finished = false; total = secs; left = secs; endAt = System.currentTimeMillis() + secs * 1000L }
    fun reset() { stopBuzz(context); finished = false; endAt = null; left = total }
    val running = endAt != null
    val customSecs = if (Regex("^\\d{1,3}$").matches(custom.trim())) custom.trim().toInt() else null
    val customOk = customSecs != null && customSecs in 5..600
    val bg = lerp(c.card, c.accent, flash.value)

    ScreenFrame("Rest timer", back = false, help = "timer", cog = true, insights = true, bottom = { TabBar(Route.Timer) }) {
        Column(
            Modifier.padding(top = 20.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(bg).padding(vertical = 36.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }, horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (finished) "Go!" else clock(left), Modifier.semantics { contentDescription = if (finished) "Time is up" else "$left seconds left" },
                color = if (finished) c.onAccent else c.ink, fontSize = 72.sp, fontWeight = FontWeight.ExtraBold,
            )
            Text(if (finished) "Rest is over" else if (running) "Resting" else "${secLabel(total)} ready", color = if (finished) c.onAccent else c.muted, fontSize = 17.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Start a rest")
            ChipRows { for (p in PRESETS) Chip(secLabel(p), total == p && (running || finished), { start(p) }) }
        }
        Row(Modifier.padding(top = 22.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                Field("Custom seconds", custom, { custom = it.filter { ch -> ch.isDigit() }.take(3) }, max = 3, placeholder = "45", keyboard = androidx.compose.ui.text.input.KeyboardType.Number)
            }
            BigBtn("Start", { customSecs?.let { secs -> if (customOk) start(secs) } }, enabled = customOk)
        }
        Text("Custom runs from 5 to 600 seconds.", color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        if (running || finished) Row(Modifier.padding(top = 22.dp)) { BigBtn(if (finished) "Done" else "Stop", { reset() }, secondary = true) }
        Text("It buzzes and flashes when time is up, as long as Spot is open. Spot does not send reminders when it is closed.", color = c.muted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 22.dp, bottom = 24.dp))
    }
}
