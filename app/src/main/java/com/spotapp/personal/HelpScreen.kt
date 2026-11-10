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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Help. With no topic it is a menu of separate sections; with a topic it shows that one topic.
// The i button on a screen opens the topic for that screen.
@Composable
fun HelpScreen(topicKey: String?) {
    val app = LocalApp.current
    val c = colors()
    val topic = findHelp(topicKey)
    if (topic != null) {
        ScreenFrame(topic.title, cog = true, floating = { SettingsHomeHere() }) {
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Para(topic.body)
                if (topic.key == "welcome") BigBtn("Replay the welcome", { app.go(Route.Welcome) }, Modifier.fillMaxWidth())
                BigBtn("All help topics", { app.go(Route.Help()) }, Modifier.fillMaxWidth(), secondary = true)
            }
        }
        return
    }
    ScreenFrame("Help", cog = true, floating = { SettingsHomeHere() }) {
        for (g in HELP_GROUPS) {
            Box(Modifier.padding(top = 18.dp)) { Caption(g.title) }
            Column(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card).padding(horizontal = 16.dp)) {
                var first = true
                for (k in g.keys) {
                    val t = findHelp(k) ?: continue
                    if (!first) Divider()
                    first = false
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClickLabel = t.title) { app.go(Route.Help(t.key)) }
                            .semantics { contentDescription = t.title },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(t.title, Modifier.weight(1f), color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Box(Modifier.rotate(180f)) { SpotIcon("back", c.muted, 18.dp) }
                    }
                }
            }
        }
    }
}
