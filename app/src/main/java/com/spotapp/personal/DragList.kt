package com.spotapp.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// A list whose rows can be reordered by pressing and holding a grip, then dragging.
// Rows stay in normal layout; the held row follows the finger and the others slide out of the way.
// People using TalkBack move a row with the grip's custom actions ("Move up", "Move down") instead of dragging.

@Composable
fun Grip(label: String, active: Boolean, onMove: (Int) -> Boolean, modifier: Modifier = Modifier) {
    val c = colors()
    Column(
        modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) c.accent else c.chip)
            .semantics {
                contentDescription = "$label. Press and hold, then drag, or use the actions to move it."
                customActions = listOf(
                    CustomAccessibilityAction("Move up") { onMove(-1) },
                    CustomAccessibilityAction("Move down") { onMove(1) },
                )
            },
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(3) { Box(Modifier.width(20.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (active) c.onAccent else c.ink)) }
    }
}

@Composable
fun <T> DragList(
    items: List<T>, keyOf: (T) -> String, gap: Int = 8,
    onReorder: (List<String>) -> Unit, onDragging: (Boolean) -> Unit = {},
    row: @Composable (item: T, index: Int, grip: @Composable () -> Unit) -> Unit,
) {
    val keys = items.map(keyOf)
    val sizes = remember { HashMap<String, Int>() }
    var heldKey by remember { mutableStateOf<String?>(null) }
    var heldFrom by remember { mutableStateOf(0) }
    var dy by remember { mutableStateOf(0f) }
    val liveKeys by rememberUpdatedState(keys)
    val liveReorder by rememberUpdatedState(onReorder)
    val liveDragging by rememberUpdatedState(onDragging)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val gapPx = with(density) { gap.dp.toPx() }

    fun heights(): List<Float> = liveKeys.map { (sizes[it] ?: 200).toFloat() + gapPx }

    val hs = heights()
    val to = if (heldKey != null) targetIndex(hs, heldFrom, dy) else -1
    val heldH = if (heldKey != null) hs.getOrElse(heldFrom) { 0f } else 0f

    Column(verticalArrangement = Arrangement.spacedBy(gap.dp)) {
        items.forEachIndexed { i, item ->
            val key = keys[i]
            val held = heldKey == key
            var shift = 0f
            if (heldKey != null && !held) {
                if (heldFrom < to && i > heldFrom && i <= to) shift = -heldH
                if (heldFrom > to && i < heldFrom && i >= to) shift = heldH
            }
            Box(
                Modifier
                    .onSizeChanged { sizes[key] = it.height }
                    .graphicsLayer { translationY = if (held) dy else shift; alpha = if (held) 0.95f else 1f }
                    .then(if (held) Modifier.shadow(6.dp, RoundedCornerShape(18.dp)) else Modifier),
            ) {
                row(item, i) {
                    Grip(
                        "Reorder handle ${i + 1} of ${items.size}", held,
                        onMove = { by ->
                            val next = moveKey(liveKeys, i, by)
                            if (next != null) { liveReorder(next); true } else false
                        },
                        modifier = Modifier.pointerInput(key) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    heldKey = key; heldFrom = liveKeys.indexOf(key); dy = 0f; liveDragging(true)
                                },
                                onDrag = { change, amount -> change.consume(); dy += amount.y },
                                onDragEnd = {
                                    val hh = heights()
                                    val dest = targetIndex(hh, heldFrom, dy)
                                    if (dest != heldFrom) {
                                        val next = liveKeys.toMutableList()
                                        val one = next.removeAt(heldFrom)
                                        next.add(dest, one)
                                        liveReorder(next)
                                    }
                                    heldKey = null; dy = 0f; liveDragging(false)
                                },
                                onDragCancel = { heldKey = null; dy = 0f; liveDragging(false) },
                            )
                        },
                    )
                }
            }
        }
    }
}
