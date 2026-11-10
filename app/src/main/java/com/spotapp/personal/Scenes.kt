package com.spotapp.personal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp

// The PR Records Style scenes. Each one is drawn from the person's real records: heavier lifts make brighter stars, taller peaks and taller buildings.

class StyleInfo(val key: String, val label: String, val sub: String)

val RECORDS_STYLES = listOf(
    StyleInfo("sky", "Night sky", "A star for each exercise, joined into constellations"),
    StyleInfo("range", "Mountain range", "A peak for each exercise, taller as your record rises"),
    StyleInfo("skyline", "Skyline", "A building for each exercise that gains floors"),
    StyleInfo("ghost", "Ghost race", "You against the last period, one lane per body part"),
    StyleInfo("list", "Plain list", "Just the list, newest first"),
)

private fun rnd(seed: Double): Double {
    val x = Math.sin(seed * 12.9898 + 78.233) * 43758.5453
    return x - Math.floor(x)
}

class Hit(val x0: Float, val y0: Float, val x1: Float, val y1: Float, val rec: RecordRow)

private fun DrawScope.label(tm: TextMeasurer, text: String, x: Float, y: Float, size: Float, color: Color, weight: FontWeight = FontWeight.Bold, center: Boolean = false, spacing: Float = 0f) {
    val r = tm.measure(text, TextStyle(color = color, fontSize = size.sp, fontWeight = weight, letterSpacing = spacing.sp))
    val ox = if (center) x - r.size.width / 2f else x
    drawText(r, topLeft = Offset(ox, y - r.size.height * 0.75f))
}

private val sortHeavy = Comparator<RecordRow> { a, b -> b.weight.compareTo(a.weight) }

@Composable
private fun recordColor(colorBy: String): (RecordRow) -> Color {
    val c = colors()
    val cols = BODY_PARTS.associateWith { bodyColor(it) }
    return { r ->
        when (colorBy) {
            "body" -> r.bodyParts.firstOrNull()?.let { cols[it] } ?: c.accent
            "place" -> if (r.placeColor.isNotEmpty()) hexColor(r.placeColor) else c.accent
            else -> c.accent
        }
    }
}

@Composable
fun SceneCanvas(desc: String, onSelect: (RecordRow) -> Unit, modifier: Modifier, draw: DrawScope.(TextMeasurer, MutableList<Hit>) -> Unit) {
    val tm = rememberTextMeasurer()
    val hits = remember { ArrayList<Hit>() }
    Canvas(
        modifier.semantics { contentDescription = desc }.pointerInput(Unit) {
            detectTapGestures { p ->
                val h = hits.lastOrNull { p.x >= it.x0 && p.x <= it.x1 && p.y >= it.y0 && p.y <= it.y1 }
                if (h != null) onSelect(h.rec)
            }
        },
    ) {
        hits.clear()
        this.draw(tm, hits)
    }
}

// ---------- night sky ----------

@Composable
fun NightSky(records: List<RecordRow>, colorBy: String, top: Float, bottom: Float, selectedId: Long?, onSelect: (RecordRow) -> Unit, modifier: Modifier) {
    val c = colors()
    val color = recordColor(colorBy)
    SceneCanvas("Night sky with ${records.size} records. Open the list for details.", onSelect, modifier) { tm, hits ->
        val width = size.width; val height = size.height
        val max = maxOf(1.0, records.maxOfOrNull { it.weight } ?: 1.0)
        val newest = records.fold<RecordRow, RecordRow?>(null) { m, r -> if (m == null || r.day > m.day) r else m }
        class G(val label: String, val col: Color, val recs: MutableList<RecordRow>)
        val groups = LinkedHashMap<String, G>()
        for (r in records) {
            val key = if (colorBy == "place") r.locationName.ifEmpty { "No place" } else if (colorBy == "body") (r.bodyParts.firstOrNull() ?: "other") else "all"
            val lab = if (colorBy == "body") (r.bodyParts.firstOrNull()?.let { BODY_PART_LABELS[it] } ?: "Other") else if (colorBy == "place") r.locationName.ifEmpty { "No place" } else "Records"
            groups.getOrPut(key) { G(lab, color(r), ArrayList()) }.recs.add(r)
        }
        val list = groups.values.toList()
        val cols = if (list.size > 4) 3 else 2
        val rows = maxOf(1, Math.ceil(list.size.toDouble() / cols).toInt())
        val areaH = height - top - bottom
        val cw = width / cols; val ch = areaH / rows
        drawRect(Brush.verticalGradient(listOf(Color(0xFF06141A), Color(0xFF123741), Color(0xFF1F4A4F)), 0f, height), size = Size(width, height))
        for (i in 0 until 70) drawCircle(Color.White.copy(alpha = (0.35 + rnd(i + 3.0) * 0.6).toFloat()), (0.6 + rnd(i + 7.0) * 1.1).toFloat(), Offset((rnd(i + 1.0) * width).toFloat(), (rnd(i + 91.0) * height).toFloat()))
        list.forEachIndexed { gi, g ->
            val cx0 = (gi % cols) * cw; val cy0 = top + (gi / cols) * ch
            class P(val r: RecordRow, val x: Float, val y: Float)
            val pts = g.recs.sortedWith(sortHeavy).map { r -> P(r, (cx0 + 24 + rnd(r.exerciseId * 3.0 + 1) * (cw - 48)).toFloat(), (cy0 + 22 + rnd(r.exerciseId * 5.0 + 2) * (ch - 56)).toFloat()) }.sortedBy { it.x }
            if (pts.size > 1) {
                val path = Path().apply { moveTo(pts[0].x, pts[0].y); for (p in pts.drop(1)) lineTo(p.x, p.y) }
                drawPath(path, g.col.copy(alpha = 0.8f), style = Stroke(1.5f * density))
            }
            for (p in pts) {
                val rad = (3 + (p.r.weight / max) * 4).toFloat() * density
                val isNew = newest != null && p.r.exerciseId == newest.exerciseId
                val sel = selectedId == p.r.exerciseId
                val glow = rad + (if (isNew || sel) 10f else 6f) * density
                drawCircle(if (isNew || sel) Color(0xFFF3E4B0).copy(alpha = 0.4f) else g.col.copy(alpha = 0.22f), glow, Offset(p.x, p.y))
                drawCircle(if (isNew) Color(0xFFF6ECC4) else Color.White, rad, Offset(p.x, p.y))
                val hr = 22f * density
                hits.add(Hit(p.x - hr, p.y - hr, p.x + hr, p.y + hr, p.r))
            }
            label(tm, g.label.uppercase(), cx0 + 20f, cy0 + ch - 14f, 12f, g.col, FontWeight.ExtraBold, spacing = 2f)
        }
        if (records.isEmpty()) label(tm, "Your first record lights the first star", width / 2, height / 2, 16f, c.muted, FontWeight.Normal, true)
    }
}

// ---------- mountain range ----------

@Composable
fun MountainRange(records: List<RecordRow>, colorBy: String, top: Float, bottom: Float, selectedId: Long?, onSelect: (RecordRow) -> Unit, modifier: Modifier) {
    val c = colors()
    val color = recordColor(colorBy)
    SceneCanvas("Mountain range with ${records.size} records. Open the list for details.", onSelect, modifier) { tm, hits ->
        val width = size.width; val height = size.height
        val shown = records.sortedWith(sortHeavy).take(9)
        val max = maxOf(1.0, shown.maxOfOrNull { it.weight } ?: 1.0)
        val ground = height - bottom + 30f * density
        val areaH = ground - top - 30f * density
        val ordered = ArrayList<RecordRow>()
        shown.forEachIndexed { i, r -> if (i % 2 == 0) ordered.add(r) else ordered.add(0, r) }
        val n = maxOf(1, ordered.size)
        val slot = width / n
        drawRect(
            Brush.verticalGradient(
                listOf(if (c.isDark) Color(0xFF1B2F37) else Color(0xFFCFD9E0), if (c.isDark) Color(0xFF3A3B3F) else Color(0xFFF1D9C4), if (c.isDark) Color(0xFF4A4338) else Color(0xFFF3E6CF)),
                0f, height,
            ),
            size = Size(width, height),
        )
        drawCircle(if (c.isDark) Color(0xFFD9C98F) else Color(0xFFFBF0D6), 40f * density, Offset(width * 0.78f, top + 40f * density))
        ordered.forEachIndexed { i, r ->
            val x = slot * (i + 0.5f)
            val h = (0.3 * areaH + 0.7 * areaH * (r.weight / max)).toFloat()
            val w = maxOf(slot * 1.5f, 90f * density)
            val sel = selectedId == r.exerciseId
            val tri = Path().apply { moveTo(x - w / 2, ground); lineTo(x, ground - h); lineTo(x + w / 2, ground); close() }
            drawPath(tri, color(r).copy(alpha = if (sel) 1f else 0.92f))
            val cap = Path().apply {
                moveTo(x - w * 0.11f, ground - h * 0.78f); lineTo(x, ground - h); lineTo(x + w * 0.11f, ground - h * 0.78f)
                lineTo(x + w * 0.04f, ground - h * 0.72f); lineTo(x, ground - h * 0.8f); lineTo(x - w * 0.05f, ground - h * 0.72f); close()
            }
            drawPath(cap, Color.White.copy(alpha = 0.9f))
            val pole = 22f * density
            drawLine(Color(0xFF5A4636), Offset(x, ground - h), Offset(x, ground - h - pole), 2f * density)
            val flag = Path().apply { moveTo(x, ground - h - pole); relativeLineTo(14f * density, 4f * density); relativeLineTo(-14f * density, 4f * density); close() }
            drawPath(flag, c.accent)
            hits.add(Hit(x - w / 2, ground - h - pole, x + w / 2, ground, r))
        }
        drawRect(if (c.isDark) Color(0xFF1D2B27) else Color(0xFF2F4B43), Offset(0f, ground), Size(width, height - ground))
        ordered.take(6).forEachIndexed { i, r ->
            label(tm, if (r.name.length > 12) r.name.take(11) + "…" else r.name, slot * (i + 0.5f), ground + 22f * density, 11f, Color(0xFFF4F1EA), center = true)
        }
        if (records.isEmpty()) label(tm, "Your first record plants the first flag", width / 2, height / 2, 16f, c.muted, FontWeight.Normal, true)
    }
}

// ---------- skyline ----------

@Composable
fun Skyline(records: List<RecordRow>, colorBy: String, top: Float, bottom: Float, selectedId: Long?, onSelect: (RecordRow) -> Unit, modifier: Modifier) {
    val c = colors()
    val color = recordColor(colorBy)
    SceneCanvas("Skyline with ${records.size} records. Open the list for details.", onSelect, modifier) { tm, hits ->
        val width = size.width; val height = size.height
        val d = density
        val shown = records.sortedWith(sortHeavy).take(10)
        val max = maxOf(1.0, shown.maxOfOrNull { it.weight } ?: 1.0)
        val ground = height - bottom + 40f * d
        val areaH = ground - top - 40f * d
        val n = maxOf(1, shown.size)
        val bw = minOf(70f * d, (width - 24f * d) / n - 6f * d)
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0D2630), Color(0xFF2F4B57)), 0f, height), size = Size(width, height))
        for (i in 0 until 40) drawCircle(Color.White.copy(alpha = 0.6f), ((0.7 + rnd(i.toDouble())) * d).toFloat(), Offset((rnd(i + 11.0) * width).toFloat(), (top + rnd(i + 51.0) * (areaH * 0.5)).toFloat()))
        drawCircle(Color(0xFFF3E9CC), 20f * d, Offset(width * 0.82f, top + 36f * d))
        shown.forEachIndexed { i, r ->
            val x = 12f * d + i * ((width - 24f * d) / n) + ((width - 24f * d) / n - bw) / 2
            val h = (0.25 * areaH + 0.75 * areaH * (r.weight / max)).toFloat()
            val floors = maxOf(2, Math.round(h / (18f * d)).toInt())
            val sel = selectedId == r.exerciseId
            drawRect(color(r).copy(alpha = if (sel) 1f else 0.92f), Offset(x, ground - h), Size(bw, h))
            for (f in 0 until floors - 1) {
                val y = ground - h + 8f * d + f * 18f * d
                drawRect(Color(0xFFF6ECC4).copy(alpha = if (rnd(r.exerciseId * 7.0 + f) > 0.25) 0.9f else 0.25f), Offset(x + bw * 0.2f, y), Size(bw * 0.18f, 8f * d))
                drawRect(Color(0xFFF6ECC4).copy(alpha = if (rnd(r.exerciseId * 13.0 + f) > 0.25) 0.9f else 0.25f), Offset(x + bw * 0.6f, y), Size(bw * 0.18f, 8f * d))
            }
            label(tm, if (r.name.length > 8) r.name.take(7) + "…" else r.name, x + bw / 2, ground + 16f * d, 10f, Color(0xFFE8F2EE), center = true)
            hits.add(Hit(x, ground - h, x + bw, ground, r))
        }
        drawRect(Color(0xFFE8F2EE).copy(alpha = 0.4f), Offset(0f, ground), Size(width, 2f * d))
        if (records.isEmpty()) label(tm, "Your first record raises the first building", width / 2, height / 2, 16f, c.muted, FontWeight.Normal, true)
    }
}

// ---------- ghost race ----------

@Composable
fun GhostRace(now: Map<String, Double>, before: Map<String, Double>, top: Float, bottom: Float, periodWord: String, modifier: Modifier) {
    val c = colors()
    val cols = BODY_PARTS.associateWith { bodyColor(it) }
    val lanes = BODY_PARTS.filter { (now[it] ?: 0.0) > 0 || (before[it] ?: 0.0) > 0 }
        .sortedByDescending { maxOf(now[it] ?: 0.0, before[it] ?: 0.0) }.take(5)
    SceneCanvas("Ghost race: this $periodWord against the one before, for ${lanes.size} body parts.", {}, modifier) { tm, _ ->
        val width = size.width; val height = size.height; val d = density
        val area = height - top - bottom
        val gap = if (lanes.isNotEmpty()) area / lanes.size else area
        val left = 70f * d; val right = width - 28f * d
        drawRect(c.bg, size = Size(width, height))
        lanes.forEachIndexed { i, b ->
            val y = top + gap * (i + 0.5f)
            val nv = now[b] ?: 0.0; val bv = before[b] ?: 0.0
            val m = maxOf(nv, bv, 1.0)
            val xNow = (left + (right - left) * nv / m).toFloat()
            val xBefore = (left + (right - left) * bv / m).toFloat()
            val col = cols.getValue(b)
            label(tm, BODY_PART_LABELS[b] ?: b, 14f * d, y + 4f * d, 13f, c.ink, FontWeight.ExtraBold)
            drawLine(c.edge, Offset(left, y), Offset(right, y), 2f * d, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * d, 6f * d)))
            drawCircle(col, 14f * d, Offset(xBefore, y), style = Stroke(2.5f * d, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * d, 4f * d))))
            drawCircle(col, 12f * d, Offset(xNow, y))
        }
        if (lanes.isEmpty()) label(tm, "Log a few workouts to start the race", width / 2, height / 2, 16f, c.muted, FontWeight.Normal, true)
        label(tm, "Solid dot: this $periodWord. Dashed ring: the one before.", width / 2, height - bottom + 28f * d, 13f, c.muted, FontWeight.Normal, true)
    }
}
