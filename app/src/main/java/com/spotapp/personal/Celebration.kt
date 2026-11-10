package com.spotapp.personal

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

// A short celebration for a new record. It closes by itself after a few seconds, and has no motion when the phone asks for less.
private val CONFETTI = listOf(0xFFC4694A, 0xFFC2A15A, 0xFF4F8A7B, 0xFFB35C6B, 0xFF7C6BA6, 0xFF4A7FA6, 0xFFC98AA0).map { Color(it) }

@Composable
fun Celebration(text: String, onClose: () -> Unit) {
    val c = colors()
    val context = LocalContext.current
    val still = remember {
        try { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } catch (e: Exception) { false }
    }
    val fall = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!still) fall.animateTo(1f, tween(1800)) }
    LaunchedEffect(Unit) { delay(5000); onClose() }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            if (!still) Canvas(Modifier.fillMaxSize()) {
                for (i in 0 until 16) {
                    val x = ((i * 53) % 100) / 100f * (size.width - 20f)
                    val sz = (8 + (i % 3) * 3).toFloat() * 2f
                    val p = ((fall.value * 1.4f) - (i % 6) * 0.07f).coerceIn(0f, 1f)
                    val y = -20f + p * size.height * 0.8f
                    rotate((200 + i * 20) * p, Offset(x + sz / 2, y + sz / 2)) {
                        drawRect(CONFETTI[i % CONFETTI.size].copy(alpha = if (p <= 0f) 0f else 0.9f - 0.2f * p), Offset(x, y), Size(sz, sz))
                    }
                }
            }
            Column(
                Modifier.padding(28.dp).widthIn(max = 360.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.bg).padding(24.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(84.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) { SpotIcon("trophy", c.onAccent, 46.dp) }
                Text("New record", Modifier.semantics { heading() }, color = c.ink, fontFamily = Serif, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(text, color = c.muted, fontSize = 18.sp, textAlign = TextAlign.Center)
                Row(Modifier.fillMaxWidth()) { BigBtn("Nice", onClose, Modifier.weight(1f)) }
            }
        }
    }
}
