package com.spotapp.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The dashed "+ Add ..." row used at the end of lists.
@Composable
fun DashedAdd(label: String, description: String = label, onClick: () -> Unit, height: Int = 48) {
    val c = colors()
    Row(
        Modifier.fillMaxWidth().heightIn(min = height.dp).clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRoundRect(c.edge, cornerRadius = CornerRadius(16.dp.toPx()), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
            }
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        SpotIcon("plus", c.ink, 20.dp)
        Text(label, color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

// The rows of sets (or rounds) for one exercise: tap a row to edit it, Add set to add one.
@Composable
fun SetList(sets: List<SetRow>, curSetId: Long?, ex: Exercise, onSelect: (Long) -> Unit, onAdd: () -> Unit) {
    val c = colors()
    val word = rowWord(ex.tracks)
    val wordCap = word.replaceFirstChar { it.uppercase() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        sets.forEachIndexed { i, s ->
            val isCur = curSetId == s.id
            val line = setLine(s.nums, ex.tracks, ex.otherLabel, ex.unit)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(c.card)
                    .border(2.dp, if (isCur) c.nowEdge else c.card, RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClickLabel = "Edit $word ${i + 1}") { onSelect(s.id) }
                    .semantics { contentDescription = "$wordCap ${i + 1}, $line, ${if (s.done) "done" else if (isCur) "editing now" else "not yet done"}. Tap to edit." }
                    .padding(start = 8.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(if (isCur) c.nowFill else c.chip), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = if (isCur) c.onNowFill else c.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Text(line, Modifier.weight(1f), color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (s.done) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SpotIcon("check", c.ok, 20.dp)
                        Text("Done", color = c.ok, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(if (isCur) "Now" else "Next", color = if (isCur) c.accentText else c.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        DashedAdd("Add $word", "Add a $word", onAdd)
    }
}

// The numbers for the set being edited, one row for each thing the exercise writes down: type a number, or use the minus and plus buttons.
@Composable
fun SetEditor(set: SetRow, fields: List<FieldDef>, onBump: (FieldDef, Int) -> Unit, onValue: (FieldDef, Double) -> Unit) {
    val c = colors()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (f in fields) {
            val shownUnit = if (f.key == "other") f.label else f.unit
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CircleBtn("minus", "${f.label} minus ${num(f.step)} ${f.unit}".trim(), { onBump(f, -1) }, size = 48.dp)
                Row(Modifier.width(170.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    NumberField(numberOf(set.nums, f.key), f.label, if (f.key == "weight") 48 else 36, f.max, integer = f.integer, unit = f.unit.ifEmpty { null }, onCommit = { onValue(f, it) })
                    Text(shownUnit, color = c.muted, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
                }
                CircleBtn("plus", "${f.label} plus ${num(f.step)} ${f.unit}".trim(), { onBump(f, 1) }, size = 48.dp)
            }
        }
    }
}

private fun wordFor(st: String) = if (st == "done") "Done" else if (st == "skipped") "Skipped" else ""

// The exercise list of one workout: tap an exercise to open it, Later to send it to the bottom, and press and hold the
// handle at the right of a row, then drag, to move it (the same way as the goals in Tend).
@Composable
fun ExerciseListView(
    w: WorkoutFull, doneCount: Int, onOpen: (Int) -> Unit, onLater: (Int) -> Unit, onReorder: (List<Long>) -> Unit,
    onAdd: () -> Unit, onDragging: (Boolean) -> Unit,
) {
    val c = colors()
    val items = w.items

    @Composable
    fun row(it: WeFull, i: Int, grip: @Composable (Boolean) -> Unit) {
        val logged = it.sets.count { s -> s.done }
        val place = it.exercise.machines.firstOrNull { m -> m.locationId == it.locationId }?.locationName ?: ""
        val sub = listOf(it.exercise.machine, if (place != w.locationName) place else "", "$logged of ${it.sets.size} sets").filter { s -> s.isNotEmpty() }.joinToString(" · ")
        val dim = it.status != "todo"
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.weight(1f).heightIn(min = 68.dp).alpha(if (dim) 0.7f else 1f).clip(RoundedCornerShape(18.dp)).background(c.card)
                    .border(2.dp, if (it.status == "done") c.ok else c.card, RoundedCornerShape(18.dp))
                    .clickable(role = Role.Button, onClickLabel = "Open ${it.exercise.name}") { onOpen(i) }
                    .semantics { contentDescription = "${it.exercise.name}, $sub${if (wordFor(it.status).isNotEmpty()) ", " + wordFor(it.status) else ""}. Opens it." }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (it.exercise.icon.isNotEmpty()) ExerciseBadge(it.exercise.icon, it.exercise.bodyParts, 44.dp) else Box(Modifier.width(44.dp))
                Column(Modifier.weight(1f)) {
                    Text(it.exercise.name, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(sub, color = c.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                }
                if (it.status == "done") SpotIcon("check", c.ok, 22.dp)
                else if (it.status == "skipped") Text("Skipped", color = c.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            if (it.status == "todo" && i < items.size - 1) {
                Box(
                    Modifier.width(56.dp).heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp)).background(c.chip)
                        .clickable(role = Role.Button, onClickLabel = "${it.exercise.name}: do this later, machine busy") { onLater(i) },
                    contentAlignment = Alignment.Center,
                ) { Text("Later", color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
            }
            if (items.size > 1) grip(true)
        }
    }

    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$doneCount of ${items.size} done · do them in any order", Modifier.weight(1f), color = c.muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        DragList(items, { it.id.toString() }, onReorder = { keys -> onReorder(keys.map { k -> k.toLong() }) }, onDragging = onDragging) { it, i, grip -> row(it, i, grip) }
        Box(Modifier.padding(top = 4.dp)) { DashedAdd("Add exercise", "Add exercises", onAdd, height = 54) }
    }
}

// "Where I left off": the last, best or other-place session of this exercise, with a button to copy its sets.
@Composable
fun PastPanel(past: List<Session>, placeId: Long?, hist: String, onHist: (String) -> Unit, ex: Exercise, onUse: (Long) -> Unit, onBrowse: () -> Unit) {
    val c = colors()
    val atPlace = if (placeId == null) past else past.filter { it.locationId == placeId }
    val elsewhere = if (placeId == null) emptyList() else past.filter { it.locationId != placeId }
    fun bestOf(list: List<Session>): Session? {
        var b: Session? = null
        for (x in list) if (b == null || betterSession(x, b)) b = x
        return b
    }
    val shown: Session? = when (hist) { "last" -> atPlace.firstOrNull(); "best" -> bestOf(atPlace); else -> elsewhere.firstOrNull() }
    val emptyWord = if (hist == "other") "Nothing logged at another place yet" else if (placeId != null && past.isNotEmpty()) "Nothing logged here yet. Try Other." else "No earlier session yet"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Segmented(listOf("last" to "Last", "best" to "Best", "other" to "Other"), hist, onHist)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (shown != null) {
                Text(describeSets(shown.sets, ex.tracks, ex.otherLabel, ex.unit), color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(listOf(prettyDay(shown.day), shown.locationName).filter { it.isNotEmpty() }.joinToString(" · "), color = c.muted, fontSize = 14.sp)
                Row(Modifier.padding(top = 4.dp)) { BigBtn("Use these sets", { onUse(shown.weId) }, secondary = true) }
            } else Text(emptyWord, color = c.muted, fontSize = 15.sp)
            if (past.isNotEmpty()) LinkText("Browse all past sessions…", onBrowse)
        }
    }
}
