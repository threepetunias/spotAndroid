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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Settings pages for the Home cards, how each card looks, and PR Records Style.

private val PERIODS = listOf("week" to "Week", "month" to "Month", "year" to "Year", "all" to "All")

@Composable
fun HomeCardsPage() {
    val app = LocalApp.current
    val c = colors()
    var dragging by remember { mutableStateOf(false) }
    val order = readCardOrder(app.settings["widgetOrder"] ?: "")
    ScreenFrame("Home cards", scrollEnabled = !dragging, floating = { SettingsHome() }) {
        app.inSettings = true
        Intro("Turn cards on or off, press and hold the grip to drag one into place, and tap one to choose how it looks.")
        Column(Modifier.padding(top = 14.dp)) {
            DragList(order, { it }, gap = 0, onDragging = { dragging = it }, onReorder = { keys -> app.setSetting("widgetOrder", keys.joinToString(",")) }) { w, i, grip ->
                val info = WIDGET_INFO.getValue(w)
                val on = app.settings[info.onKey] == "on"
                Column(Modifier.background(c.bg)) {
                    if (i > 0) Divider()
                    Row(Modifier.heightIn(min = 76.dp).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(
                            Modifier.weight(1f).clickable(role = Role.Button) { app.go(Route.Page("widget", HOME_WIDGETS.indexOf(w).toLong())) }
                                .semantics { contentDescription = "${info.title}. Choose how it looks." },
                        ) {
                            Text(info.title, color = if (on) c.ink else c.muted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(info.sub, color = c.muted, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Chip(if (on) "On" else "Off", on, { app.setSetting(info.onKey, if (on) "off" else "on") })
                        grip()
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetPage(arg: Long?) {
    val app = LocalApp.current
    val c = colors()
    val w = HOME_WIDGETS.getOrNull(arg?.toInt() ?: 0) ?: "today"
    val info = WIDGET_INFO.getValue(w)
    val current = app.settings[info.styleKey] ?: info.styles.first().key
    val rawPeriod = info.periodKey?.let { app.settings[it] } ?: "week"
    val period = if (PERIODS.any { it.first == rawPeriod }) rawPeriod else "week"
    val today = todayStr()
    val sampleDays = listOf(0L, 1, 3, 4, 8, 10, 15, 17, 22).mapIndexed { i, n ->
        TrainedDay(addDays(today, -n), if (i % 2 == 0) "gym" else "home", if (i % 2 == 0) "#0F5E5C" else "#C4694A", if (i % 2 == 0) "Gym" else "Home")
    }
    ScreenFrame(info.title, floating = { SettingsHome() }) {
        app.inSettings = true
        Intro("Choose how this card looks on Home. The pictures below use sample numbers.")
        if (info.periodKey != null) Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Time period")
            Segmented(PERIODS, period) { app.setSetting(info.periodKey, it) }
        }
        for (s in info.styles) {
            val on = current == s.key
            Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.label, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(s.sub, color = c.muted, fontSize = 15.sp)
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).border(3.dp, if (on) c.nowEdge else c.line, RoundedCornerShape(22.dp)).padding(8.dp)) {
                    when (w) {
                        "today" -> TodayWidget(s.key, Sample.today, {}, {})
                        "garden" -> GardenWidget(s.key, Sample.sets, period)
                        "balance" -> BalanceWidget(s.key, Sample.push, Sample.pull, period)
                        "fun" -> FunWidget(s.key, Sample.fn, "lb", period)
                        else -> WeekWidget(s.key, sampleDays, 3, today)
                    }
                    // A clear layer on top: tapping anywhere chooses this look and the sample itself does nothing.
                    Box(
                        Modifier.matchParentSize().clickable(role = Role.RadioButton) { app.setSetting(info.styleKey, s.key) }
                            .semantics { this.selected = on; contentDescription = if (on) "${s.label}, chosen" else "Use ${s.label}" },
                    )
                    if (on) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(26.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                        SpotIcon("check", c.onAccent, 16.dp)
                    }
                }
            }
        }
    }
}

private val STYLE_ICON = mapOf("sky" to "star", "range" to "peak", "skyline" to "tower", "ghost" to "race", "list" to "list")

@Composable
fun RecordsStylePage() {
    val app = LocalApp.current
    val c = colors()
    val style = app.settings["recStyle"] ?: "sky"
    SPage("PR Records Style") {
        Intro("How the PRs page looks. Your full list, with search and filters, is always one tap away.")
        Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Style")
            TileRows(RECORDS_STYLES) { s, m ->
                PictureTile(s.label, style == s.key, { app.setSetting("recStyle", s.key) }, modifier = m) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(c.chip), contentAlignment = Alignment.Center) { SpotIcon(STYLE_ICON[s.key] ?: "list", c.accentText, 24.dp) }
                }
            }
            Text(RECORDS_STYLES.firstOrNull { it.key == style }?.sub ?: "", color = c.muted, fontSize = 15.sp, lineHeight = 22.sp)
        }
        Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Time period it opens on")
            Segmented(PERIODS, app.settings["recPeriod"] ?: "month") { app.setSetting("recPeriod", it) }
        }
        Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Color by")
            Segmented(listOf("body" to "Body part", "place" to "Place", "theme" to "Theme"), app.settings["recColorBy"] ?: "body") { app.setSetting("recColorBy", it) }
        }
    }
}
