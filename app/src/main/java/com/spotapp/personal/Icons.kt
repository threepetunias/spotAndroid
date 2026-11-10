package com.spotapp.personal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Every icon is drawn here from shapes, with the same geometry as the iPhone app. No emoji or text symbols.

fun hexColor(hex: String): Color {
    if (!Regex("^#[0-9A-Fa-f]{6}$").matches(hex)) return Color.Gray
    return Color(0xFF000000 or hex.substring(1).toLong(16))
}

private fun parsePath(d: String): Path = PathParser().parsePathString(d).toPath()

private fun DrawScope.stroke(d: String, color: Color, w: Float, s: Float) {
    withTransform({ scale(s, s, Offset.Zero) }) {
        drawPath(parsePath(d), color, style = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun DrawScope.fillP(d: String, color: Color, s: Float) {
    withTransform({ scale(s, s, Offset.Zero) }) { drawPath(parsePath(d), color) }
}

private fun DrawScope.dot(cx: Float, cy: Float, r: Float, color: Color, s: Float) = drawCircle(color, r * s, Offset(cx * s, cy * s))

private fun DrawScope.ring(cx: Float, cy: Float, r: Float, color: Color, w: Float, s: Float) =
    drawCircle(color, r * s, Offset(cx * s, cy * s), style = Stroke(width = w * s))

private fun DrawScope.boxLine(x: Float, y: Float, w: Float, h: Float, rx: Float, color: Color, sw: Float, s: Float) =
    drawRoundRect(color, Offset(x * s, y * s), Size(w * s, h * s), CornerRadius(rx * s), style = Stroke(width = sw * s))

private fun DrawScope.boxFill(x: Float, y: Float, w: Float, h: Float, rx: Float, color: Color, s: Float) =
    drawRoundRect(color, Offset(x * s, y * s), Size(w * s, h * s), CornerRadius(rx * s))

// The same cog as Tend: a solid ring (radius 7.6) with six flat-topped teeth out to 10.5.
private fun cogPath(): Path {
    val ro = 10.5; val ri = 7.6; val ot = 0.2; val rt = 0.34
    val p = Path()
    var first = true
    fun pt(a: Double, r: Double) {
        val x = (12 + r * Math.cos(a)).toFloat(); val y = (12 + r * Math.sin(a)).toFloat()
        if (first) { p.moveTo(x, y); first = false } else p.lineTo(x, y)
    }
    for (i in 0 until 6) {
        val a = i * 2 * Math.PI / 6 - Math.PI / 2
        val na = (i + 1) * 2 * Math.PI / 6 - Math.PI / 2
        pt(a - rt, ri); pt(a - ot, ro); pt(a + ot, ro); pt(a + rt, ri)
        for (k in 1..3) pt(a + rt + (na - rt - (a + rt)) * (k / 4.0), ri)
    }
    p.close()
    return p
}

@Composable
fun SpotIcon(name: String, color: Color, size: Dp = 22.dp, hole: Color = Color.Transparent) {
    Box(Modifier.size(size).clearAndSetSemantics { }) {
        Canvas(Modifier.size(size)) {
            val s = this.size.width / 24f
            val sw = 2.2f
            when (name) {
                "back" -> stroke("M14.5 5.5 8 12l6.5 6.5", color, sw, s)
                "help" -> {
                    ring(12f, 12f, 9.25f, color, 2f, s); dot(12f, 7.6f, 1.4f, color, s); boxFill(11f, 10.6f, 2f, 6.6f, 1f, color, s)
                }
                "cog" -> {
                    withTransform({ scale(s, s, Offset.Zero) }) { drawPath(cogPath(), color) }
                    dot(12f, 12f, 3.2f, hole, s)
                }
                "more" -> { dot(5f, 12f, 2f, color, s); dot(12f, 12f, 2f, color, s); dot(19f, 12f, 2f, color, s) }
                "plus" -> stroke("M12 5v14M5 12h14", color, 2.6f, s)
                "minus" -> stroke("M5 12h14", color, 2.6f, s)
                "check" -> stroke("M5 12.5l4.5 4.5L19 7.5", color, 2.6f, s)
                "close" -> stroke("M6 6l12 12M18 6L6 18", color, sw, s)
                "list" -> {
                    stroke("M9 6h11M9 12h11M9 18h11", color, sw, s)
                    dot(4.5f, 6f, 1.4f, color, s); dot(4.5f, 12f, 1.4f, color, s); dot(4.5f, 18f, 1.4f, color, s)
                }
                "machine" -> {
                    boxLine(5f, 3.5f, 14f, 4f, 1.5f, color, 2f, s); boxLine(5f, 10f, 14f, 4f, 1.5f, color, 2f, s); boxLine(5f, 16.5f, 14f, 4f, 1.5f, color, 2f, s)
                }
                "notes" -> { stroke("M6 3h9l4 4v14H6z", color, 2f, s); stroke("M9 12h7M9 16h7", color, 2f, s) }
                "history" -> { ring(12f, 12f, 8.5f, color, 2f, s); stroke("M12 7.5V12l3 2", color, 2f, s) }
                "tag" -> { stroke("M3.5 12.5V4.5h8l9 9-8 8z", color, 2f, s); dot(8f, 9f, 1.4f, color, s) }
                "timer" -> { ring(12f, 13.5f, 7.5f, color, 2f, s); stroke("M12 13.5V9.5M9.5 3h5", color, 2f, s) }
                "trophy" -> {
                    stroke("M7.5 4h9v5a4.5 4.5 0 0 1-9 0zM7.5 6H4.5v1.5a3 3 0 0 0 3 3M16.5 6h3v1.5a3 3 0 0 1-3 3", color, 2f, s)
                    stroke("M12 13.5V17M8.5 20h7M12 17v3", color, 2f, s)
                }
                "search" -> { ring(10.5f, 10.5f, 6.5f, color, 2f, s); stroke("M15.5 15.5 21 21", color, 2f, s) }
                "home" -> stroke("M4 11.5 12 4l8 7.5V20h-5.5v-5.5h-5V20H4z", color, 2f, s)
                "dumbbell" -> { stroke("M7 8v8M17 8v8M7 12h10", color, 2.4f, s); stroke("M4 10v4M20 10v4", color, 2.4f, s) }
                "link" -> {
                    stroke("M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1", color, 2f, s)
                    stroke("M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1", color, 2f, s)
                }
                "trash" -> stroke("M4.5 7h15M9 7V4.5h6V7M6.5 7l1 13h9l1-13", color, 2f, s)
                "palette" -> {
                    stroke("M12 3a9 9 0 1 0 0 18c1.5 0 2-1 1.5-2s0-2.4 1.5-2.4H17a4 4 0 0 0 4-4C21 6.5 17 3 12 3z", color, 2f, s)
                    dot(8f, 11f, 1.3f, color, s); dot(11f, 7.5f, 1.3f, color, s); dot(15.5f, 8.5f, 1.3f, color, s)
                }
                "pin" -> { stroke("M12 21s-6.5-6-6.5-11a6.5 6.5 0 0 1 13 0c0 5-6.5 11-6.5 11z", color, 2f, s); dot(12f, 10f, 2.3f, color, s) }
                "body" -> { dot(12f, 5f, 2.4f, color, s); stroke("M7 21v-8l-3-3M17 21v-8l3-3M9 21v-6h6v6M8 9.5h8", color, 2f, s) }
                "grid" -> {
                    boxFill(3.5f, 3.5f, 7.5f, 7.5f, 2f, color, s); boxFill(13f, 3.5f, 7.5f, 7.5f, 2f, color, s)
                    boxFill(3.5f, 13f, 7.5f, 7.5f, 2f, color, s); boxLine(13f, 13f, 7.5f, 7.5f, 2f, color, 2f, s)
                }
                "save" -> { stroke("M5 4h11l3 3v13H5z", color, 2f, s); stroke("M8 4v5h7V4M8 20v-6h8v6", color, 2f, s) }
                "mic" -> { boxFill(9f, 3f, 6f, 11f, 3f, color, s); stroke("M5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21", color, 2f, s) }
                "up" -> stroke("M5.5 14.5 12 8l6.5 6.5", color, sw, s)
                "down" -> stroke("M5.5 9.5 12 16l6.5-6.5", color, sw, s)
                "star" -> { fillP("M12 3.5l2.6 5.6 6 .7-4.5 4.1 1.3 6-5.4-3.1-5.4 3.1 1.3-6-4.5-4.1 6-.7z", color, s); stroke("M12 3.5l2.6 5.6 6 .7-4.5 4.1 1.3 6-5.4-3.1-5.4 3.1 1.3-6-4.5-4.1 6-.7z", color, 1.2f, s) }
                "peak" -> { fillP("M2.5 19.5 9 7l4 7 2.5-4 6 9.5z", color, s); stroke("M2.5 19.5 9 7l4 7 2.5-4 6 9.5z", color, 1.2f, s) }
                "tower" -> { boxFill(3.5f, 11f, 5f, 9f, 1f, color, s); boxFill(9.5f, 4f, 5f, 16f, 1f, color, s); boxFill(15.5f, 8f, 5f, 12f, 1f, color, s) }
                "race" -> stroke("M4 6l6 6-6 6M12 6l6 6-6 6", color, sw, s)
                "chart" -> {
                    stroke("M4 20h16", color, sw, s); boxFill(5.5f, 11f, 3.2f, 7f, 1f, color, s)
                    boxFill(10.4f, 6f, 3.2f, 12f, 1f, color, s); boxFill(15.3f, 9f, 3.2f, 9f, 1f, color, s)
                }
                else -> {}
            }
        }
    }
}

// ---------- pictures for exercises, places and body parts: drawn on a 24 by 24 grid ----------

class PicDef(val id: String, val name: String, val group: String, val keywords: String, val s: List<String>, val f: List<String>, val src: String) {
    val sPaths: List<Path> by lazy { s.map { parsePath(it) } }
    val fPaths: List<Path> by lazy { f.map { parsePath(it) } }
}

object Pictures {
    @Volatile var all: List<PicDef> = emptyList()
    @Volatile var groups: List<String> = emptyList()
    private var byId: Map<String, PicDef> = emptyMap()

    fun byId(id: String): PicDef? = byId[id]

    // Reads the picture list from the app's own files. A bad file shows no pictures but never stops the app.
    fun load(context: android.content.Context) {
        if (all.isNotEmpty()) return
        try {
            val text = context.assets.open("workout_icons.json").bufferedReader().use { it.readText() }
            val root = Json.parse(text) as Map<*, *>
            groups = (root["groups"] as List<*>).map { it.toString() }
            val list = (root["icons"] as List<*>).map { e ->
                val m = e as Map<*, *>
                PicDef(
                    m["id"].toString(), m["name"].toString(), m["group"].toString(), m["keywords"].toString(),
                    (m["s"] as List<*>).map { it.toString() }, (m["f"] as List<*>).map { it.toString() }, m["src"].toString(),
                )
            }
            all = list
            byId = list.associateBy { it.id }
        } catch (e: Exception) {
            all = emptyList()
        }
    }

    // Every typed word must appear in the name, group or keywords. Name matches come first.
    fun search(q: String): List<PicDef> {
        val text = q.trim().lowercase()
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return all
        val scored = ArrayList<Triple<PicDef, Int, Int>>()
        all.forEachIndexed { n, i ->
            val name = i.name.lowercase()
            val hay = "$name ${i.group} ${i.keywords}".lowercase()
            if (!words.all { hay.contains(it) }) return@forEachIndexed
            val score = if (name == text) 0 else if (name.startsWith(text)) 1 else if (name.contains(text)) 2 else if (words.all { name.contains(it) }) 3 else 4
            scored.add(Triple(i, score, n))
        }
        return scored.sortedWith(compareBy({ it.second }, { it.third })).map { it.first }
    }
}

private fun DrawScope.glyph(s: List<Path>, f: List<Path>, color: Color) {
    val k = size.width / 24f
    withTransform({ scale(k, k, Offset.Zero) }) {
        for (p in s) drawPath(p, color, style = Stroke(width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        for (p in f) drawPath(p, color)
    }
}

private fun dotPath(x: Float, y: Float, r: Float): String = "M${x - r} ${y}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

private class Def(val s: List<String>, val f: List<String> = emptyList()) {
    val sP: List<Path> by lazy { s.map { parsePath(it) } }
    val fP: List<Path> by lazy { f.map { parsePath(it) } }
}

private val PLACE_DEFS: Map<String, Def> = mapOf(
    "gym" to Def(listOf("M8 12 L16 12", "M6.5 9.4 L6.5 14.6", "M4.5 10.2 L4.5 13.8", "M17.5 9.4 L17.5 14.6", "M19.5 10.2 L19.5 13.8")),
    "home" to Def(listOf("M4 11 L12 4 L20 11", "M6 10 L6 20 L18 20 L18 10", "M10 20 L10 14 L14 14 L14 20")),
    "dance_studio" to Def(listOf("M12 7 L12 13", "M5 9 L12 8 L19 4", "M12 13 L8 20", "M12 13 L17 16 L20 12"), listOf(dotPath(12f, 4.4f, 1.7f))),
    "park" to Def(listOf(dotPath(12f, 9f, 5.5f), "M12 14.5 L12 20", "M6 20.5 L18 20.5")),
    "pool" to Def(listOf("M3 16 q3 -3 6 0 t6 0 t6 0", "M3 20 q3 -3 6 0 t6 0 t6 0", "M11 11 L17 8", "M7 12 L11 11"), listOf(dotPath(7f, 8.5f, 1.8f))),
    "track" to Def(listOf("M7 6 H17 a6 6 0 0 1 0 12 H7 a6 6 0 0 1 0 -12 Z", "M8.5 9.5 H15.5 a2.5 2.5 0 0 1 0 5 H8.5 a2.5 2.5 0 0 1 0 -5 Z")),
    "climbing_wall" to Def(listOf("M5 4 H19 V20 H5 Z"), listOf(dotPath(9f, 8f, 1.2f), dotPath(15f, 10f, 1.2f), dotPath(10f, 14f, 1.2f), dotPath(16f, 16f, 1.2f))),
    "yoga_studio" to Def(listOf("M12 9 L12 14", "M6 19 L12 14 L18 19", "M7 11 L12 11 L17 11", "M4 20.5 L20 20.5"), listOf(dotPath(12f, 5.8f, 1.8f))),
    "trail" to Def(listOf("M3 20 L9 9 L13 15 L16 11 L21 20 Z")),
    "court" to Def(listOf("M3 6 H21 V18 H3 Z", "M12 6 L12 18", dotPath(12f, 12f, 2.8f))),
)
private val FEET = Def(listOf("M8 4 Q7 11 8 15 Q9 17 8 20 L14 20 Q16 19 15 16 L14 12 Q15 7 12 4 Z"))
private val LUNGS = Def(listOf("M12 4 L12 12", "M12 8 Q7 8 5 13 Q4 18 8 19 Q11 19 11 15", "M12 8 Q17 8 19 13 Q20 18 16 19 Q13 19 13 15"))
private val BP_ICON_ID = mapOf(
    "arms" to "bp_arms", "shoulders" to "bp_shoulders", "back" to "bp_back", "chest" to "bp_chest", "core" to "bp_abs", "legs" to "bp_legs", "glutes" to "bp_glutes",
)

@Composable
private fun Round(size: Dp, bg: Color, ring: Color? = null, content: @Composable () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).background(bg).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun GlyphBox(size: Dp, s: List<Path>, f: List<Path>, color: Color) {
    Canvas(Modifier.size(size)) { glyph(s, f, color) }
}

@Composable
private fun PlaceGlyphBox(icon: String, size: Dp, color: Color) {
    val d = PLACE_DEFS[icon] ?: PLACE_DEFS.getValue("gym")
    GlyphBox(size, d.sP, d.fP, color)
}

// The color a place shows in: the one the person chose, else the theme color.
@Composable
fun placeColor(color: String): Color = if (color.isEmpty()) colors().accent else hexColor(color)

@Composable
fun PlaceBadge(icon: String, color: String, size: Dp = 40.dp) {
    Round(size, placeColor(color)) { PlaceGlyphBox(icon, size * 0.58f, Color.White) }
}

@Composable
fun PlaceGlyph(icon: String, color: Color, size: Dp = 22.dp) = PlaceGlyphBox(icon, size, color)

@Composable
fun bodyColor(part: String): Color {
    val app = LocalApp.current
    val key = BP_COLOR_KEY[part] ?: return colors().accent
    return hexColor(app.settings[key] ?: BODY_PART_COLORS[part] ?: "#0F5E5C")
}

@Composable
private fun BodyGlyphBox(part: String, size: Dp, color: Color) {
    val pic = BP_ICON_ID[part]?.let { Pictures.byId(it) }
    if (pic != null) GlyphBox(size, pic.sPaths, pic.fPaths, color)
    else {
        val d = if (part == "feet") FEET else LUNGS
        GlyphBox(size, d.sP, d.fP, color)
    }
}

@Composable
fun BodyBadge(part: String, size: Dp = 44.dp, off: Boolean = false) {
    val c = colors()
    Round(size, if (off) c.chip else bodyColor(part)) { BodyGlyphBox(part, size * 0.58f, if (off) c.muted else Color.White) }
}

@Composable
fun BodyGlyph(part: String, color: Color, size: Dp = 22.dp) = BodyGlyphBox(part, size, color)

// A workout picture on a round background in the current color theme (accent with its matching text color).
@Composable
fun WorkoutPicture(id: String, size: Dp = 44.dp, plain: Boolean = false, tint: Color? = null) {
    val c = colors()
    val def = Pictures.byId(id)
    val fg = if (plain) c.ink else if (tint != null) Color.White else c.onAccent
    Round(size, if (plain) c.chip else tint ?: c.accent) {
        if (def != null) GlyphBox(size * 0.58f, def.sPaths, def.fPaths, fg)
    }
}

// An exercise picture. When "Color pictures by body part" is on, it takes the color of the exercise's first body part.
@Composable
fun ExerciseBadge(iconId: String, parts: List<String>, size: Dp = 44.dp) {
    val app = LocalApp.current
    val first = parts.firstOrNull()
    val tint = if (app.settings["colorByBody"] == "on" && first != null) bodyColor(first) else null
    WorkoutPicture(iconId, size, tint = tint)
}
