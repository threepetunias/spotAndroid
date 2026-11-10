package com.spotapp.personal

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The setup pages (Welcome). Same wording as the iPhone app (src/welcomePages.ts).

enum class WelcomeArt { LOGO, EXERCISES, START, ADD, LOG, NONE }

class WelcomePage(
    val art: WelcomeArt, val title: String, val body: String,
    val note: String? = null, val steps: List<String> = emptyList(), val checks: List<String> = emptyList(),
)

val WELCOME_PAGES: List<WelcomePage> = listOf(
    WelcomePage(
        WelcomeArt.LOGO, "Need a spot?",
        "Spot is a workout log. It fills in your sets from last time, so there is nothing to copy. Here is how it works:",
        steps = listOf("Set up your exercises", "Start a workout", "Add exercises to it", "Log sets and reps", "Make it yours"),
    ),
    WelcomePage(
        WelcomeArt.EXERCISES, "Set up your exercises",
        "Add the exercises you do, each with a picture, equipment, body parts and cues. Do it now and they are ready to pick later. Or add them as you work out, right from the workout.",
        note = "Find them any time in Settings > Exercises.",
    ),
    WelcomePage(
        WelcomeArt.START, "Start a workout",
        "Tap the + button. Pick a day on the calendar, a place and a kind of workout. Spot names the workout for you.",
        note = "Any day works: today, a past day or one coming up.",
    ),
    WelcomePage(
        WelcomeArt.ADD, "Add exercises",
        "Choose the exercises for this workout. You can also copy them from a past workout. Missing one? Make it on the spot.",
        note = "Do them in any order. Tap Later if a machine is busy.",
    ),
    WelcomePage(
        WelcomeArt.LOG, "Capture your workout in real time",
        "Log each set as you do it. Tap the weight to type it, use − and + for reps, then tap Log set. Spot shows what you did last time.",
        note = "Add a Spot widget to your home screen to jump straight back to your workout.",
    ),
    WelcomePage(
        WelcomeArt.NONE, "Make it yours",
        "Spot bends to how you work. In Settings you can customize:",
        checks = listOf("The look and feel", "The home page", "The PR page", "Workout types", "Tags and notes"),
    ),
)

@Composable
private fun WelcomeRow(name: String, color: Color, mark: String? = null) {
    val c = colors()
    Row(
        Modifier.padding(vertical = 3.dp).width(250.dp).clip(RoundedCornerShape(14.dp)).background(c.bg).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(color))
        Text(name, Modifier.weight(1f), color = c.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (mark != null) Text(if (mark == "on") "✓" else "○", color = if (mark == "on") c.accentText else c.muted, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun WelcomeArtView(kind: WelcomeArt) {
    val c = colors()
    when (kind) {
        WelcomeArt.LOGO -> Text("Spot", color = c.accentText, fontFamily = Serif, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        WelcomeArt.EXERCISES -> Column {
            WelcomeRow("Bench press", Color(0xFF0F5E5C)); WelcomeRow("Leg press", Color(0xFFC4694A)); WelcomeRow("Lat pulldown", Color(0xFF7C6BA6))
        }
        WelcomeArt.ADD -> Column {
            WelcomeRow("Bench press", Color(0xFF0F5E5C), "on"); WelcomeRow("Leg press", Color(0xFFC4694A), "on"); WelcomeRow("Lat pulldown", Color(0xFF7C6BA6), "off")
        }
        WelcomeArt.START -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(62.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) { SpotIcon("plus", c.onAccent, 32.dp) }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val rows = listOf(listOf("S", "M", "T", "W", "T", "F", "S"), listOf("", "2", "3", "4", "5", "6", "7"))
                for (r in rows) Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (d in r) Box(Modifier.size(24.dp).clip(CircleShape).background(if (d == "4") c.accent else Color.Transparent), contentAlignment = Alignment.Center) {
                        Text(d, color = if (d == "4") c.onAccent else c.ink, fontSize = 11.sp)
                    }
                }
            }
        }
        WelcomeArt.LOG -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.width(250.dp).clip(RoundedCornerShape(14.dp)).background(c.bg).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("Set 2 of 3", color = c.ink, fontSize = 14.sp)
                Text("60 lb × 8", color = c.ink, fontFamily = Serif, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.clip(RoundedCornerShape(22.dp)).background(c.accent).padding(horizontal = 18.dp, vertical = 9.dp)) {
                Text("Log set", color = c.onAccent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        WelcomeArt.NONE -> {}
    }
}

// Six short setup pages. Next and Back move between them; Skip (pages 1 to 5) and Get started finish.
@Composable
fun WelcomeScreen() {
    val app = LocalApp.current
    val c = colors()
    var page by remember { mutableIntStateOf(0) }
    val last = WELCOME_PAGES.size - 1
    val p = WELCOME_PAGES[page.coerceIn(0, last)]

    fun finish() {
        app.setSetting("onboarded", "1") { if (app.stack.size > 1) app.back() else app.home() }
    }

    ScreenFrame(
        "", back = false, pad = 24.dp,
        right = { if (page < last) Text("Skip", Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClickLabel = "Skip the welcome") { finish() }.padding(horizontal = 12.dp, vertical = 12.dp), color = c.accentText, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        bottom = {
            PinnedBar {
                if (page > 0) BigBtn("Back", { page -= 1 }, Modifier.weight(1f), secondary = true)
                if (page < last) BigBtn("Next", { page += 1 }, Modifier.weight(1f)) else BigBtn("Get started", { finish() }, Modifier.weight(1f))
            }
        },
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), contentAlignment = Alignment.Center) { WelcomeArtView(p.art) }
        Text(p.title, Modifier.padding(top = 12.dp).semantics { contentDescription = p.title }, color = c.ink, fontFamily = Serif, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.padding(top = 10.dp)) { Para(p.body) }
        for ((i, s) in p.steps.withIndex()) {
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) { Text("${i + 1}", color = c.onAccent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }
                Text(s, color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        for (s in p.checks) {
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✓", color = c.accentText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(s, color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (p.note != null) Box(Modifier.padding(top = 16.dp)) { Para(p.note, muted = true, size = 15) }
        Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp), horizontalArrangement = Arrangement.Center) {
            for (i in 0..last) Box(Modifier.padding(horizontal = 4.dp).size(8.dp).clip(CircleShape).background(if (i == page) c.accent else c.line))
        }
    }
}
