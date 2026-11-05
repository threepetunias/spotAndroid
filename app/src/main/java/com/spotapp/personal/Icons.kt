package com.spotapp.personal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Every icon is drawn here from shapes, with the same geometry as the iPhone app. No emoji or text symbols.
private fun DrawScope.line(d: String, color: Color, w: Float, s: Float) {
    val p = PathParser().parsePathString(d).toPath()
    withTransform({ scale(s, s, Offset.Zero) }) {
        drawPath(p, color, style = Stroke(width = w / s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun DrawScope.fillPath(d: String, color: Color, s: Float) {
    val p = PathParser().parsePathString(d).toPath()
    withTransform({ scale(s, s, Offset.Zero) }) { drawPath(p, color) }
}

private fun DrawScope.dot(cx: Float, cy: Float, r: Float, color: Color, s: Float) =
    drawCircle(color, r * s, Offset(cx * s, cy * s))

private fun DrawScope.ring(cx: Float, cy: Float, r: Float, color: Color, w: Float, s: Float) =
    drawCircle(color, r * s, Offset(cx * s, cy * s), style = Stroke(width = w * s))

private fun DrawScope.roundRectLine(x: Float, y: Float, w: Float, h: Float, rx: Float, color: Color, sw: Float, s: Float) =
    drawRoundRect(color, Offset(x * s, y * s), Size(w * s, h * s), CornerRadius(rx * s), style = Stroke(width = sw * s))

private fun DrawScope.roundRectFill(x: Float, y: Float, w: Float, h: Float, rx: Float, color: Color, s: Float) =
    drawRoundRect(color, Offset(x * s, y * s), Size(w * s, h * s), CornerRadius(rx * s))

@Composable
fun SpotIcon(name: String, color: Color, size: Dp = 22.dp, hole: Color = Color.Transparent) {
    Box(Modifier.size(size).clearAndSetSemantics { }) {
        Canvas(Modifier.size(size)) {
            val s = this.size.width / 24f
            val sw = 2.2f * s
            when (name) {
                "back" -> line("M14.5 5.5 8 12l6.5 6.5", color, sw, s)
                "help" -> {
                    ring(12f, 12f, 9.25f, color, 2f, s)
                    dot(12f, 7.6f, 1.4f, color, s)
                    roundRectFill(11f, 10.6f, 2f, 6.6f, 1f, color, s)
                }
                "cog" -> {
                    dot(12f, 12f, 6.6f, color, s)
                    for (a in listOf(0f, 60f, 120f, 180f, 240f, 300f)) {
                        rotate(a, Offset(12f * s, 12f * s)) { roundRectFill(10.1f, 1.4f, 3.8f, 5f, 1f, color, s) }
                    }
                    dot(12f, 12f, 2.7f, hole, s)
                }
                "more" -> { dot(5f, 12f, 2f, color, s); dot(12f, 12f, 2f, color, s); dot(19f, 12f, 2f, color, s) }
                "plus" -> line("M12 5v14M5 12h14", color, 2.6f * s, s)
                "minus" -> line("M5 12h14", color, 2.6f * s, s)
                "check" -> line("M5 12.5l4.5 4.5L19 7.5", color, 2.6f * s, s)
                "close" -> line("M6 6l12 12M18 6L6 18", color, sw, s)
                "list" -> {
                    line("M9 6h11M9 12h11M9 18h11", color, sw, s)
                    dot(4.5f, 6f, 1.4f, color, s); dot(4.5f, 12f, 1.4f, color, s); dot(4.5f, 18f, 1.4f, color, s)
                }
                "machine" -> {
                    roundRectLine(5f, 3.5f, 14f, 4f, 1.5f, color, 2f, s)
                    roundRectLine(5f, 10f, 14f, 4f, 1.5f, color, 2f, s)
                    roundRectLine(5f, 16.5f, 14f, 4f, 1.5f, color, 2f, s)
                }
                "notes" -> {
                    line("M6 3h9l4 4v14H6z", color, 2f * s, s)
                    line("M9 12h7M9 16h7", color, 2f * s, s)
                }
                "history" -> { ring(12f, 12f, 8.5f, color, 2f, s); line("M12 7.5V12l3 2", color, 2f * s, s) }
                "tag" -> { line("M3.5 12.5V4.5h8l9 9-8 8z", color, 2f * s, s); dot(8f, 9f, 1.4f, color, s) }
                "home" -> line("M4 11.5 12 4l8 7.5V20h-5.5v-5.5h-5V20H4z", color, 2f * s, s)
                "dumbbell" -> { line("M7 8v8M17 8v8M7 12h10", color, 2.4f * s, s); line("M4 10v4M20 10v4", color, 2.4f * s, s) }
                "trash" -> line("M4.5 7h15M9 7V4.5h6V7M6.5 7l1 13h9l1-13", color, 2f * s, s)
                else -> {}
            }
        }
    }
}
