package com.spotapp.personal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Charts for Insights. Each is drawn from numbers it is given. Colors: one meaning each, never red.

@Composable
fun Ring(fraction: Double, color: Color, center: String, sub: String? = null, size: Dp = 104.dp) {
    val c = colors()
    val f = fraction.coerceIn(0.0, 1.0).toFloat()
    Box(Modifier.size(size).semantics { contentDescription = center + (if (sub != null) " $sub" else "") }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size).clearAndSetSemantics { }) {
            val sw = 11f * this.size.width / 100f
            val r = 40f * this.size.width / 100f
            val tl = Offset(this.size.width / 2 - r, this.size.height / 2 - r)
            drawArc(c.chip, 0f, 360f, false, tl, Size(r * 2, r * 2), style = Stroke(sw))
            drawArc(color, -90f, 360f * f, false, tl, Size(r * 2, r * 2), style = Stroke(sw, cap = StrokeCap.Round))
        }
        androidx.compose.foundation.layout.Column(Modifier.clearAndSetSemantics { }, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(center, color = c.ink, fontFamily = Serif, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (sub != null) Text(sub, color = c.muted, fontSize = 11.sp)
        }
    }
}

// A line over time. Drawn only with 2 or more points.
@Composable
fun LineChart(values: List<Double>, startLabel: String, endLabel: String, height: Dp = 90.dp) {
    if (values.size < 2) return
    val c = colors()
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(height).semantics { contentDescription = "Line over ${values.size} weeks, from $startLabel to $endLabel" }) {
        val d = density
        val max = maxOf(1.0, values.max())
        val pad = 8f * d
        val w = size.width - pad * 2
        val h = size.height - 26f * d
        val pts = values.mapIndexed { i, v -> Offset((pad + w * i / (values.size - 1)).toFloat(), (6f * d + h - h * v / max).toFloat()) }
        drawLine(c.line, Offset(pad, 6f * d + h), Offset(size.width - pad, 6f * d + h), d)
        val path = Path().apply { moveTo(pts[0].x, pts[0].y); for (p in pts.drop(1)) lineTo(p.x, p.y) }
        drawPath(path, c.accent, style = Stroke(3f * d, cap = StrokeCap.Round, join = StrokeJoin.Round))
        for (p in pts) drawCircle(c.accent, 4f * d, p)
        val st = TextStyle(color = c.muted, fontSize = 11.sp)
        drawText(tm, startLabel, Offset(pad, size.height - 16f * d), st)
        val er = tm.measure(endLabel, st)
        drawText(er, topLeft = Offset(size.width - pad - er.size.width, size.height - 16f * d))
    }
}

// One full-height column per week, split by body part.
@Composable
fun StackedColumns(weeks: List<Map<String, Int>>, label: String, height: Dp = 120.dp) {
    val c = colors()
    val tm = rememberTextMeasurer()
    val cols = BODY_PARTS.associateWith { bodyColor(it) }
    Canvas(Modifier.fillMaxWidth().height(height).semantics { contentDescription = label }) {
        val d = density
        val max = maxOf(1, weeks.maxOfOrNull { w -> BODY_PARTS.sumOf { w[it] ?: 0 } } ?: 1)
        val gap = 8f * d
        val colW = (size.width - gap * (weeks.size + 1)) / weeks.size
        val h = size.height - 20f * d
        weeks.forEachIndexed { i, w ->
            var y = h
            for (p in BODY_PARTS) {
                val hh = h * (w[p] ?: 0) / max
                y -= hh
                if (hh > 0) drawRect(cols.getValue(p), Offset(gap + i * (colW + gap), y), Size(colW, hh))
            }
        }
        drawLine(c.line, Offset(0f, h), Offset(size.width, h), d)
        drawText(tm, label, Offset(gap, size.height - 16f * d), TextStyle(color = c.muted, fontSize = 11.sp))
    }
}

class DayDot(val day: String, val color: String, val trained: Boolean)

// Big dots for days: filled when trained (in the place color), outlined for a rest day.
@Composable
fun DayDots(days: List<DayDot>) {
    val c = colors()
    val cols = 7
    val rows = Math.ceil(days.size / cols.toDouble()).toInt()
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cell = maxWidth / cols
        Canvas(Modifier.fillMaxWidth().height(cell * rows).semantics { contentDescription = "${days.count { it.trained }} days trained" }) {
            val cellPx = size.width / cols
            val r = minOf(13f * density, cellPx / 2 - 5f * density)
            days.forEachIndexed { i, d ->
                val center = Offset((i % cols) * cellPx + cellPx / 2, (i / cols) * cellPx + cellPx / 2)
                if (d.trained) drawCircle(if (d.color.isEmpty()) c.accent else hexColor(d.color), r, center)
                else drawCircle(c.edge.copy(alpha = 0.45f), r, center, style = Stroke(2f * density))
            }
        }
    }
}
