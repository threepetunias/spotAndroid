package com.spotapp.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PERIODS = listOf(Period.WEEK, Period.MONTH, Period.YEAR, Period.ALL)
private val ZERO: Map<String, Double> = BODY_PARTS.associateWith { 0.0 }

// One row per exercise: its heaviest set, with the place it was done as a small colored picture.
@Composable
fun RecordsList(rows: List<RecordRow>) {
    val app = LocalApp.current
    val c = colors()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (p in rows) {
            val detail = listOf("${p.reps} ${if (p.reps == 1) "rep" else "reps"}", shortDay(p.day), p.locationName).filter { it.isNotEmpty() }.joinToString(" · ")
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(18.dp)).background(c.card)
                    .clickable(role = Role.Button, onClickLabel = "Open ${p.name}") { app.go(Route.ExerciseEdit(p.exerciseId)) }
                    .semantics { contentDescription = "${p.name}, ${num(p.weight)} ${p.unit}, $detail. Opens the exercise." }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExerciseBadge(p.icon, p.bodyParts, 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(p.name, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (p.placeColor.isNotEmpty() || p.placeIcon.isNotEmpty()) PlaceBadge(p.placeIcon, p.placeColor, 16.dp)
                        Text(detail, color = c.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(num(p.weight), color = c.ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(p.unit, color = c.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
        }
    }
}

private class PrsData(val rows: List<RecordRow>, val all: List<RecordRow>, val now: Map<String, Double>, val before: Map<String, Double>)

@Composable
fun PrsScreen() {
    val app = LocalApp.current
    val c = colors()
    val style = app.settings["recStyle"] ?: "sky"
    val colorBy = app.settings["recColorBy"] ?: "body"
    var period by remember { mutableStateOf(Period.of(app.settings["recPeriod"] ?: "month")) }
    var sel by remember { mutableStateOf<Long?>(null) }
    val state = load("prs", period) {
        val inPeriod = listRecords(RecordFilter(period = period))
        val everything = listRecords(RecordFilter())
        val a = periodRange(period, 0)
        val b = periodRange(period, -1)
        PrsData(inPeriod, everything, volumeByPart(a.from, a.to), if (period == Period.ALL) ZERO else volumeByPart(b.from, b.to))
    }
    val d = (state as? Load.Ready)?.value
    val rows = d?.rows ?: emptyList()
    val chosen = rows.firstOrNull { it.exerciseId == sel } ?: rows.firstOrNull()
    val list = style == "list"
    val ink = if (list) c.ink else if (style == "range" && !c.isDark) Color(0xFF16393B) else Color(0xFFF4F1EA)
    val dark = style != "range" && !list || c.isDark
    val density = LocalDensity.current.density
    val statusPx = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()
    val topPx = statusPx + 118f * density
    val bottomPx = 150f * density

    @Composable
    fun controls() {
        val track = if (list) c.chip else if (style == "range" && !c.isDark) Color(0x5516393B) else Color(0x66000000)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(track).padding(3.dp)) {
            for (p in PERIODS) {
                val on = p == period
                Box(
                    Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(15.dp))
                        .background(if (on) (if (list) c.accent else Color(0xFFF4F1EA)) else Color.Transparent)
                        .clickable(role = Role.Button) { period = p }.semantics { selected = on },
                    contentAlignment = Alignment.Center,
                ) { Text(p.label, color = if (on) (if (list) c.onAccent else Color(0xFF0F5E5C)) else if (list) c.ink else Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }
            }
        }
    }

    @Composable
    fun topRow(color: Color) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("PRs", Modifier.weight(1f).semantics { heading() }, color = color, fontFamily = Serif, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            CircleBtn("chart", "Insights", { app.go(Route.Insights) })
            CircleBtn("help", "Help", { app.go(Route.Help("prs")) })
            CircleBtn("cog", "Settings", { app.goSettings(Route.Settings) })
        }
    }

    val openList = { app.go(Route.Records) }

    if (list) {
        Column(Modifier.fillMaxSize().background(c.bg)) {
            Column(Modifier.weight(1f).fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                topRow(c.ink)
                controls()
                if (d != null) {
                    if (rows.isEmpty()) Text("Your heaviest lifts show up here once you finish a set.", color = c.muted, fontSize = 17.sp, lineHeight = 24.sp) else RecordsList(rows)
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(26.dp)).background(c.chip).clickable(role = Role.Button, onClick = openList),
                        contentAlignment = Alignment.Center,
                    ) { Text("Search and filter all records", color = c.accentText, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold) }
                }
            }
            TabBar(Route.Prs)
        }
        return
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val m = Modifier.fillMaxSize()
            val pick: (RecordRow) -> Unit = { sel = it.exerciseId }
            if (d != null) when (style) {
                "sky" -> NightSky(rows, colorBy, topPx, bottomPx, chosen?.exerciseId, pick, m)
                "range" -> MountainRange(rows, colorBy, topPx, bottomPx, chosen?.exerciseId, pick, m)
                "skyline" -> Skyline(rows, colorBy, topPx, bottomPx, chosen?.exerciseId, pick, m)
                "ghost" -> GhostRace(d.now, d.before, topPx, bottomPx, if (period == Period.ALL) "period" else period.key, m)
            }
            Column(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                Box(Modifier.padding(bottom = 10.dp)) { topRow(if (style == "ghost") c.ink else ink) }
                controls()
            }
            if (style != "ghost" && chosen != null) Column(
                Modifier.align(Alignment.BottomCenter).padding(start = 18.dp, end = 18.dp, bottom = 76.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(if (dark) Color(0xAA000000) else Color(0xDD16393B))
                    .clickable(role = Role.Button, onClickLabel = "Open the exercise") { app.go(Route.ExerciseEdit(chosen.exerciseId)) }
                    .semantics { contentDescription = "${chosen.name}, ${num(chosen.weight)} ${chosen.unit}. Opens the exercise." }
                    .padding(vertical = 10.dp, horizontal = 14.dp),
            ) {
                Text("${chosen.name} · ${num(chosen.weight)} ${chosen.unit}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "${chosen.reps} ${if (chosen.reps == 1) "rep" else "reps"} · ${shortDay(chosen.day)}" + (if (chosen.locationName.isNotEmpty()) " · ${chosen.locationName}" else ""),
                    color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
            val all = d?.all?.size ?: 0
            Row(
                Modifier.align(Alignment.BottomCenter).padding(start = 18.dp, end = 18.dp, bottom = 14.dp).fillMaxWidth().heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(26.dp)).background(Color(0xFFF4F1EA)).clickable(role = Role.Button, onClick = openList)
                    .semantics { contentDescription = "See all $all records as a list" },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Text("See all $all ${if (all == 1) "record" else "records"} as a list", color = Color(0xFF0F5E5C), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Box(Modifier.rotate(180f)) { SpotIcon("back", Color(0xFF0F5E5C), 16.dp) }
            }
        }
        TabBar(Route.Prs)
    }
}

// Every record, with search and filters by body part, place, equipment, push or pull, and the person's own tags.
@Composable
fun RecordsScreen() {
    val c = colors()
    var q by remember { mutableStateOf("") }
    var parts by remember { mutableStateOf<List<String>>(emptyList()) }
    var places by remember { mutableStateOf<List<Long>>(emptyList()) }
    var equip by remember { mutableStateOf<List<String>>(emptyList()) }
    var move by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf<List<Long>>(emptyList()) }
    var open by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(q) { kotlinx.coroutines.delay(200); typed = q }

    class Data(val rows: List<RecordRow>, val locs: List<Location>, val exTags: List<ExTag>)
    val state = load("records", typed, parts, places, equip, move, tags) {
        Data(listRecords(RecordFilter(typed, parts, places, equip, move, tags)), listLocations(), listExTags())
    }
    val d = (state as? Load.Ready)?.value
    val count = parts.size + places.size + equip.size + (if (move.isNotEmpty()) 1 else 0) + tags.size
    fun <T> toggle(list: List<T>, v: T): List<T> = if (v in list) list - v else list + v

    ScreenFrame("All records", floating = { SettingsHomeHere() }) {
        Field("Search exercises", q, { q = it }, max = 60, placeholder = "Lat pulldown")
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(if (count > 0) c.accent else c.chip)
                    .clickable(role = Role.Button, onClickLabel = "Filters, $count chosen") { open = true }.padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Filters" + if (count > 0) " · $count" else "", color = if (count > 0) c.onAccent else c.ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
            if (count > 0) LinkText("Clear filters") { parts = emptyList(); places = emptyList(); equip = emptyList(); move = ""; tags = emptyList() }
        }
        Loaded(state) { data ->
            Column(Modifier.padding(top = 12.dp, bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (data.rows.isEmpty()) Text(if (count > 0 || q.isNotEmpty()) "Nothing matches. Try fewer filters." else "Your heaviest lifts show up here once you finish a set.", color = c.muted, fontSize = 17.sp, lineHeight = 24.sp)
                else RecordsList(data.rows)
                if (data.rows.isNotEmpty()) Text("${data.rows.size} ${if (data.rows.size == 1) "record" else "records"}", color = c.muted, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
    if (open) PickerBox("Filters", { open = false }, doneLabel = if (d != null) "Show ${d.rows.size}" else "Done") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Body part")
            ChipRows { for (b in BODY_PARTS) Chip(BODY_PART_LABELS[b] ?: b, b in parts, { parts = toggle(parts, b) }) }
            if (d != null && d.locs.isNotEmpty()) {
                Caption("Place")
                ChipRows { for (l in d.locs) Chip(l.name, l.id in places, { places = toggle(places, l.id) }) }
            }
            Caption("Equipment")
            ChipRows { for (e in EQUIPMENT.filter { it.isNotEmpty() }) Chip(EQUIPMENT_LABELS[e] ?: e, e in equip, { equip = toggle(equip, e) }) }
            Caption("Push or pull")
            ChoiceChips(listOf(Option(1, "Push"), Option(2, "Pull")), when (move) { "push" -> 1L; "pull" -> 2L; else -> null }, { v -> move = when (v) { 1L -> "push"; 2L -> "pull"; else -> "" } }, clearable = true)
            if (d != null && d.exTags.isNotEmpty()) {
                Caption("My tags")
                ChipRows { for (t in d.exTags) Chip(t.name, t.id in tags, { tags = toggle(tags, t.id) }) }
            }
        }
    }
}
