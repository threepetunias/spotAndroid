package com.spotapp.personal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as rotateDraw
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle as DayStyle
import java.util.Locale

// The five Home cards. Each can be drawn three ways; the person picks in Settings, Home and PRs, Home cards.
// They are given their numbers, so the same drawings also make the previews in Settings.

class WidgetStyle(val key: String, val label: String, val sub: String)
class WidgetInfo(val title: String, val sub: String, val onKey: String, val styleKey: String, val periodKey: String?, val styles: List<WidgetStyle>)

val WIDGET_INFO: Map<String, WidgetInfo> = mapOf(
    "today" to WidgetInfo("Today's workout", "Start or continue the workout planned for today", "wToday", "sToday", null, listOf(
        WidgetStyle("card", "Card", "One small row"), WidgetStyle("button", "Big button", "A large Continue button"),
        WidgetStyle("checklist", "Checklist", "The first exercises, ticked as you go"))),
    "garden" to WidgetInfo("Muscle garden", "How much each body part was trained", "wGarden", "sGarden", "pGarden", listOf(
        WidgetStyle("garden", "Illustrated garden", "Flowers grow as you train"), WidgetStyle("icons", "Icon row", "Nine small icons in a row"),
        WidgetStyle("bars", "Bars", "A bar for each body part"))),
    "balance" to WidgetInfo("Push and pull", "Whether you train pushing and pulling evenly", "wBalance", "sBalance", "pBalance", listOf(
        WidgetStyle("seesaw", "Seesaw", "Tilts toward the heavier side"), WidgetStyle("split", "Split bar", "One bar in two colors"),
        WidgetStyle("ring", "Ring and tip", "A ring with a small suggestion"))),
    "week" to WidgetInfo("This week", "Which days you trained, and where", "wWeek", "sWeek", null, listOf(
        WidgetStyle("strip", "Week strip", "Seven days with a place picture"), WidgetStyle("grid", "Month grid", "The last four weeks as small blocks"),
        WidgetStyle("streak", "Streak", "Weeks in a row with 3 or more workouts"))),
    "fun" to WidgetInfo("Fun total", "The weight you lifted, as something you can picture", "wFun", "sFun", "pFun", listOf(
        WidgetStyle("comparison", "Comparison", "\"About 3 elephants\""), WidgetStyle("number", "Big number", "The total, with progress to the next one"),
        WidgetStyle("pile", "Icon pile", "A row of weights"))),
)

val PERIOD_WORD = mapOf("week" to "this week", "month" to "this month", "year" to "this year", "all" to "all time")

class TodayItem(val name: String, val done: Boolean, val icon: String, val parts: List<String>)
class TodayData(
    val id: Long, val title: String, val place: String, val placeIcon: String, val placeColor: String,
    val done: Int, val total: Int, val items: List<TodayItem>,
)

private fun hx(s: String) = hexColor(s)

@Composable
private fun CardBox(pad: Int = 14, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors().card).padding(pad.dp)) { content() }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), color = colors().muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
}

@Composable
private fun Titled(head: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Label(head); content() }
}

// ---------- Today ----------

@Composable
fun TodayWidget(style: String, data: TodayData?, onOpen: () -> Unit, onNew: () -> Unit) {
    val c = colors()
    if (data == null) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card)
                .clickable(onClickLabel = "Start a workout", onClick = onNew).semantics { contentDescription = "No workout planned today. Start one." }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.chip), contentAlignment = Alignment.Center) { SpotIcon("plus", c.ink, 22.dp) }
            Column(Modifier.weight(1f)) {
                Text("No workout today", color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Tap to start one", color = c.muted, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        return
    }
    val sub = listOf(data.place, "${data.done} of ${data.total} done").filter { it.isNotEmpty() }.joinToString(" · ")
    if (style == "button") {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.accent).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("TODAY", color = c.onAccent.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
            Text(data.title, color = c.onAccent, fontFamily = Serif, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = c.onAccent.copy(alpha = 0.85f), fontSize = 15.sp)
            Box(
                Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).background(c.bg)
                    .clickable(onClickLabel = "Continue ${data.title}", onClick = onOpen),
                contentAlignment = Alignment.Center,
            ) { Text(if (data.done > 0) "Continue workout" else "Start workout", color = c.accentText, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold) }
        }
        return
    }
    if (style == "checklist") {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card)
                .clickable(onClickLabel = "Open ${data.title}", onClick = onOpen).semantics { contentDescription = "${data.title}, $sub. Opens it." }.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(data.title, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${data.done} of ${data.total}", color = c.muted, fontSize = 15.sp)
            }
            for (it in data.items.take(3)) Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExerciseBadge(it.icon, it.parts, 30.dp)
                Text(
                    it.name, Modifier.weight(1f), color = c.ink.copy(alpha = if (it.done) 0.55f else 1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (it.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                )
                Text(if (it.done) "Done" else "Next", color = if (it.done) c.ok else c.muted, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (data.total > 3) Text("and ${data.total - 3} more", color = c.muted, fontSize = 14.sp)
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card)
            .clickable(onClickLabel = "Open ${data.title}", onClick = onOpen).semantics { contentDescription = "${data.title}, $sub. Opens it." }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlaceBadge(data.placeIcon, data.placeColor, 44.dp)
        Column(Modifier.weight(1f)) {
            Text(data.title, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Today · $sub", color = c.muted, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        Chevron()
    }
}

@Composable
fun Chevron() {
    Box(Modifier.size(18.dp).rotate(180f)) { SpotIcon("back", colors().muted, 18.dp) }
}

// ---------- Garden ----------

private fun DrawScope.plant(x: Float, base: Float, bloom: Int, color: Color, leaf: Color, leaf2: Color) {
    if (bloom == 0) {
        val p = Path().apply { moveTo(x, base); relativeQuadraticBezierTo(-1f, -12f, 4f, -20f) }
        drawPath(p, Color(0xFF9AA58F), style = Stroke(3f, cap = StrokeCap.Round))
        rotateDraw(-25f, Offset(x + 8, base - 22)) { drawOval(Color(0xFFB8C2AD), Offset(x + 1, base - 26), Size(14f, 8f)) }
        return
    }
    val h = 24f + bloom * 22f
    val r = 9f + bloom * 2f
    val cy = base - h
    drawLine(leaf, Offset(x, base), Offset(x, cy), 4f, StrokeCap.Round)
    rotateDraw(-30f, Offset(x - 9, base - h * 0.45f)) { drawOval(leaf, Offset(x - 18, base - h * 0.45f - 4), Size(18f, 8f)) }
    rotateDraw(30f, Offset(x + 9, base - h * 0.65f)) { drawOval(leaf2, Offset(x, base - h * 0.65f - 4), Size(18f, 8f)) }
    for (i in 0..5) {
        val a = i * Math.PI / 3
        val px = (x + Math.cos(a) * r * 0.8).toFloat()
        val py = (cy + Math.sin(a) * r * 0.8).toFloat()
        val rx = r * 0.62f
        val ry = r * 0.42f
        rotateDraw((a * 180 / Math.PI).toFloat(), Offset(px, py)) { drawOval(color, Offset(px - rx, py - ry), Size(rx * 2, ry * 2)) }
    }
    drawCircle(Color(0xFFF4E3B5), r * 0.38f, Offset(x, cy))
}

@Composable
private fun GardenArt(sets: Map<String, Int>) {
    val c = colors()
    val cols = BODY_PARTS.map { bodyColor(it) }
    val w = 380f; val hgt = 250f; val base = hgt - 70f
    Canvas(Modifier.fillMaxWidth().aspectRatio(w / hgt)) {
        val k = size.width / w
        withTransform({ scale(k, k, Offset.Zero) }) {
            val top = if (c.isDark) Color(0xFF173436) else Color(0xFFCFE3DF)
            val bot = if (c.isDark) Color(0xFF1F3D3A) else Color(0xFFF3E9D2)
            drawRect(Brush.verticalGradient(listOf(top, bot), 0f, hgt), size = Size(w, hgt))
            drawCircle(if (c.isDark) Color(0xFFD9C98F) else Color(0xFFF6E6B8), 24f, Offset(w - 60, 56f))
            val hill = Path().apply {
                moveTo(0f, base - 30); quadraticBezierTo(90f, base - 70, 190f, base - 35); quadraticBezierTo(290f, base, w, base - 45)
                lineTo(w, hgt); lineTo(0f, hgt); close()
            }
            drawPath(hill, if (c.isDark) Color(0xFF27504A) else Color(0xFFB9D1C3))
            drawRect(Color(0xFF8C7357), Offset(0f, base + 4), Size(w, hgt - base))
            drawRect(Color(0xFF7A6247), Offset(0f, base + 4), Size(w, 7f))
            BODY_PARTS.forEachIndexed { i, b ->
                val x = 34f + i * (w - 68f) / 8f
                plant(x, base, bloomLevel(sets[b] ?: 0), cols[i], Color(0xFF4F8A7B), Color(0xFF3B6B5F))
            }
            BODY_PARTS.forEachIndexed { i, b ->
                val x = 34f + i * (w - 68f) / 8f
                drawCircle(if ((sets[b] ?: 0) > 0) cols[i] else Color(0xFFCFC6B3), 12f, Offset(x, base + 28))
            }
        }
    }
}

@Composable
fun GardenWidget(style: String, sets: Map<String, Int>, period: String) {
    val c = colors()
    val bloomCount = BODY_PARTS.count { (sets[it] ?: 0) > 0 }
    val sentence = if (bloomCount == 0) "Nothing logged yet. Your first workout plants the first flower." else "$bloomCount of 9 in bloom. The others are resting."
    val head = "Muscle garden · ${PERIOD_WORD[period] ?: period}"
    if (style == "icons") {
        Titled(head) {
            CardBox(10) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (b in BODY_PARTS) Column(
                        Modifier.weight(1f).semantics { contentDescription = "${BODY_PART_LABELS[b]}, ${sets[b] ?: 0} sets" },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BodyBadge(b, 30.dp, off = (sets[b] ?: 0) == 0)
                        Text((BODY_PART_LABELS[b] ?: b).take(5), color = if ((sets[b] ?: 0) > 0) bodyColor(b) else c.muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                        Row(Modifier.height(12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            repeat(minOf(3, bloomLevel(sets[b] ?: 0))) { Box(Modifier.size(5.dp).clip(CircleShape).background(bodyColor(b))) }
                        }
                    }
                }
                Text(sentence, color = c.muted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
        }
        return
    }
    if (style == "bars") {
        val max = maxOf(1, BODY_PARTS.maxOf { sets[it] ?: 0 })
        val order = BODY_PARTS.sortedByDescending { sets[it] ?: 0 }.take(6)
        Titled(head) {
            CardBox(12) {
                for (b in order) Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(BODY_PART_LABELS[b] ?: b, Modifier.width(76.dp), color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(c.chip)) {
                        Box(Modifier.fillMaxWidth(((sets[b] ?: 0).toFloat() / max).coerceIn(0f, 1f)).height(10.dp).clip(RoundedCornerShape(5.dp)).background(bodyColor(b)))
                    }
                    Text("${sets[b] ?: 0}", Modifier.width(28.dp), color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                }
            }
        }
        return
    }
    Titled(head) {
        Box(Modifier.clip(RoundedCornerShape(20.dp)).semantics { contentDescription = "Muscle garden. $sentence" }) { GardenArt(sets) }
    }
}

// ---------- Push and pull ----------

@Composable
fun BalanceWidget(style: String, push: Int, pull: Int, period: String) {
    val c = colors()
    val pushColor = bodyColor("arms"); val pullColor = bodyColor("legs")
    val t = balanceTip(push, pull)
    val head = "Push and pull · ${PERIOD_WORD[period] ?: period}"
    val acc = "Push ${t.pushPct} percent, pull ${100 - t.pushPct} percent. ${t.title}."
    if (style == "split") {
        Titled(head) {
            CardBox {
                Row(Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(9.dp)).background(c.chip).semantics { contentDescription = acc }) {
                    if (t.pushPct > 0) Box(Modifier.weight(t.pushPct.toFloat()).height(18.dp).background(pushColor))
                    if (t.pushPct < 100) Box(Modifier.weight((100 - t.pushPct).toFloat()).height(18.dp).background(pullColor))
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Push ${t.pushPct}%", color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Pull ${100 - t.pushPct}%", color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }
    if (style == "ring") {
        Titled(head) {
            CardBox {
                Row(Modifier.semantics { contentDescription = "$acc ${t.tip}" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Canvas(Modifier.size(74.dp)) {
                        val sw = 6f * size.width / 36f
                        val r = 14f * size.width / 36f
                        val tl = Offset(size.width / 2 - r, size.height / 2 - r)
                        drawArc(pullColor, 0f, 360f, false, tl, Size(r * 2, r * 2), style = Stroke(sw))
                        drawArc(pushColor, -90f, 360f * t.pushPct / 100f, false, tl, Size(r * 2, r * 2), style = Stroke(sw))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(t.title, color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(t.tip, color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
        return
    }
    val tilt = if (push + pull == 0) 0f else if (t.pushPct > 55) -8f else if (t.pushPct < 45) 8f else 0f
    Titled(head) {
        CardBox(6) {
            Column(Modifier.semantics { contentDescription = "$acc ${t.tip}" }) {
                Canvas(Modifier.fillMaxWidth().aspectRatio(340f / 110f)) {
                    val k = size.width / 340f
                    withTransform({ scale(k, k, Offset.Zero) }) {
                        rotateDraw(tilt, Offset(170f, 70f)) {
                            drawRoundRect(Color(0xFF8C7357), Offset(30f, 64f), Size(280f, 9f), CornerRadius(4.5f, 4.5f))
                            drawCircle(pushColor, 18f, Offset(62f, 48f))
                            drawCircle(pullColor, 18f, Offset(278f, 48f))
                        }
                        val tri = Path().apply { moveTo(170f, 74f); lineTo(148f, 104f); lineTo(192f, 104f); close() }
                        drawPath(tri, c.accent)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Push ${t.pushPct}%", color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(t.title, color = c.muted, fontSize = 14.sp)
                    Text("Pull ${100 - t.pushPct}%", color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------- Fun total ----------

private fun withCommas(d: Double): String = String.format(Locale.getDefault(), "%,d", Math.round(d))

@Composable
fun FunWidget(style: String, fn: FunCompare, unit: String, period: String) {
    val c = colors()
    val tint = bodyColor("shoulders")
    val total = withCommas(fn.total)
    val head = "Fun total · ${PERIOD_WORD[period] ?: period}"
    val word = if (fn.count == 0.0) "A good start" else "About ${num(fn.count)} ${fn.label}"
    @Composable
    fun bar() {
        Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(c.chip)) {
            Box(Modifier.fillMaxWidth(fn.progress.toFloat().coerceIn(0f, 1f)).height(10.dp).clip(RoundedCornerShape(5.dp)).background(tint))
        }
    }
    if (style == "number") {
        Titled(head) {
            CardBox {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("$total $unit", color = c.ink, fontFamily = Serif, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(PERIOD_WORD[period] ?: period, color = c.muted, fontSize = 14.sp)
                }
                bar()
                Text("Next: ${withCommas(fn.nextAt)} $unit (${fn.nextLabel})", color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
        return
    }
    if (style == "pile") {
        val n = maxOf(1, minOf(5, Math.round(if (fn.count == 0.0) 1.0 else fn.count).toInt()))
        Titled(head) {
            CardBox {
                Column(Modifier.fillMaxWidth().semantics { contentDescription = "$word. $total $unit." }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(n) { Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(tint), contentAlignment = Alignment.Center) { SpotIcon("dumbbell", Color.White, 28.dp) } }
                    }
                    Text(word, color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text("$total $unit lifted", color = c.muted, fontSize = 14.sp)
                }
            }
        }
        return
    }
    Titled(head) {
        CardBox {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) { SpotIcon("dumbbell", Color.White, 28.dp) }
                Column(Modifier.weight(1f)) {
                    Text(word, color = c.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("$total $unit lifted", color = c.muted, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

// ---------- Week ----------

@Composable
fun WeekWidget(style: String, days: List<TrainedDay>, streak: Int, today: String) {
    val c = colors()
    val byDay = HashMap<String, TrainedDay>()
    for (d in days) if (!byDay.containsKey(d.day)) byDay[d.day] = d
    if (style == "streak") {
        Titled("Streak") {
            CardBox {
                Row(Modifier.semantics { contentDescription = "$streak week streak. Weeks with 3 or more workouts." }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("$streak", color = c.accentText, fontFamily = Serif, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    Column(Modifier.weight(1f)) {
                        Text(if (streak == 1) "week in a row" else "weeks in a row", color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("Weeks with 3 or more workouts", color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            for (i in 0 until 8) Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (i < streak) c.accent else c.chip))
                        }
                    }
                }
            }
        }
        return
    }
    if (style == "grid") {
        val cells = (0 until 28).map { addDays(today, (it - 27).toLong()) }
        val count = cells.count { byDay.containsKey(it) }
        Titled("Last four weeks") {
            CardBox(10) {
                Column(Modifier.fillMaxWidth().semantics { contentDescription = "$count workouts in the last four weeks" }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until 4) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (col in 0 until 7) {
                            val d = cells[row * 7 + col]
                            val t = byDay[d]
                            val fill = if (t != null) (if (t.color.isEmpty()) c.accent else hx(t.color)) else c.chip
                            Box(Modifier.weight(1f).height(22.dp).clip(RoundedCornerShape(6.dp)).background(fill).then(if (d == today) Modifier.border(2.dp, c.ink, RoundedCornerShape(6.dp)) else Modifier))
                        }
                    }
                }
                Text("$count ${if (count == 1) "workout" else "workouts"} in the last 4 weeks", color = c.muted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
        }
        return
    }
    val start = periodRange(Period.WEEK, 0, today).from
    val names = listOf("M", "T", "W", "T", "F", "S", "S")
    Titled("This week") {
        CardBox(10) {
            Row(Modifier.fillMaxWidth()) {
                names.forEachIndexed { i, n ->
                    val d = addDays(start, i.toLong())
                    val t = byDay[d]
                    val word = LocalDate.parse(d).dayOfWeek.getDisplayName(DayStyle.FULL, Locale.getDefault())
                    Column(
                        Modifier.weight(1f).semantics { contentDescription = word + (if (t != null) ", trained" + (if (t.place.isNotEmpty()) " at ${t.place}" else "") else ", no workout") },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(n, color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Box(Modifier.then(if (d == today) Modifier.border(2.dp, c.ink, CircleShape) else Modifier).padding(2.dp)) {
                            if (t != null) PlaceBadge(t.icon, t.color, 34.dp)
                            else Box(Modifier.size(34.dp).border(2.dp, c.edge, CircleShape))
                        }
                    }
                }
            }
        }
    }
}

// ---------- samples for the previews in Settings ----------

object Sample {
    val sets: Map<String, Int> = mapOf("arms" to 6, "shoulders" to 0, "back" to 11, "chest" to 2, "core" to 3, "legs" to 7, "glutes" to 0, "feet" to 0, "lungs" to 0)
    val today = TodayData(0, "Leg and back", "Gym", "gym", "", 2, 5, listOf(
        TodayItem("Squat", true, "squat", listOf("legs")), TodayItem("Leg press", true, "leg_press", listOf("legs")),
        TodayItem("Lat pulldown", false, "lat_pulldown", listOf("back")),
    ))
    const val push = 58
    const val pull = 42
    val fn = FunCompare(19300.0, "elephants", 3.2, "elephants", 24000.0, 0.2)
}
