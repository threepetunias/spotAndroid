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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class HomeData(
    val rows: List<WorkoutRow>, val today: TodayData?, val garden: Map<String, Int>, val push: Int, val pull: Int,
    val fn: FunCompare, val days: List<TrainedDay>, val streak: Int,
)

@Composable
fun HomeScreen() {
    val app = LocalApp.current
    val c = colors()
    val s = app.settings
    val unit = if (s["unit"] == "kg") "kg" else "lb"
    val pGarden = Period.of(s["pGarden"] ?: "week")
    val pBalance = Period.of(s["pBalance"] ?: "month")
    val pFun = Period.of(s["pFun"] ?: "month")

    val state = load("home", pGarden, pBalance, pFun, unit) {
        val today = todayStr()
        val rows = listWorkouts()
        val todays = rows.filter { it.day == today }.sortedBy { if (it.done) 1 else 0 }.firstOrNull()
        var todayData: TodayData? = null
        if (todays != null) {
            val full = getWorkout(todays.id)
            if (full != null) {
                todayData = TodayData(
                    full.id, full.title, full.locationName, todays.placeIcon, todays.placeColor,
                    full.items.count { it.status == "done" }, full.items.size,
                    full.items.map { TodayItem(it.exercise.name, it.status == "done", it.exercise.icon, it.exercise.bodyParts) },
                )
            }
        }
        val week = periodRange(Period.WEEK, 0, today)
        val back27 = addDays(today, -27)
        val from = if (back27 < week.from) back27 else week.from
        val bal = balanceStats(pBalance)
        HomeData(rows, todayData, gardenStats(pGarden), bal.first, bal.second, funCompare(funTotal(pFun), unit), trainedDays(from, week.to), streakNow())
    }
    // Keep the phone widgets and the lock-screen card up to date after the data has loaded.
    val ready = (state as? Load.Ready)?.value
    LaunchedEffect(ready) { if (ready != null) PhoneSync.update(app) }

    val today = todayStr()
    val order = readCardOrder(s["widgetOrder"] ?: "")
    val on = { k: String -> s[WIDGET_INFO.getValue(k).onKey] == "on" }

    ScreenFrame(
        title = "Spot", back = false, help = "home", cog = true, insights = true,
        bottom = { TabBar(Route.Home) },
        floating = { FloatingAdd("New workout", { app.go(Route.WorkoutNew) }, raised = true) },
    ) {
        Loaded(state) { d ->
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (d.rows.isEmpty()) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.card).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Ready when you are", color = c.ink, fontFamily = Serif, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("Tap the + button, pick a day, and choose exercises. Spot fills in where you left off, so there is nothing to copy.", color = c.muted, fontSize = 17.sp, lineHeight = 24.sp)
                    }
                }
                for (k in order.filter(on)) when (k) {
                    "today" -> TodayWidget(s["sToday"] ?: "button", d.today, { d.today?.let { app.go(Route.Workout(it.id)) } }, { app.go(Route.WorkoutNew) })
                    "garden" -> GardenWidget(s["sGarden"] ?: "garden", d.garden, pGarden.key)
                    "balance" -> BalanceWidget(s["sBalance"] ?: "seesaw", d.push, d.pull, pBalance.key)
                    "week" -> WeekWidget(s["sWeek"] ?: "strip", d.days, d.streak, today)
                    "fun" -> FunWidget(s["sFun"] ?: "comparison", d.fn, unit, pFun.key)
                }
                val upcoming = d.rows.filter { it.day > today }.reversed()
                if (upcoming.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Caption("Coming up")
                        for (w in upcoming) Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.card).heightIn(min = 60.dp)
                                .clickable(onClickLabel = "Open ${w.title}") { app.go(Route.Workout(w.id)) }
                                .semantics { contentDescription = "${w.title}, ${prettyDay(w.day)}, ${w.exCount} exercises" }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            PlaceBadge(w.placeIcon, w.placeColor, 40.dp)
                            Column(Modifier.weight(1f)) {
                                Text(w.title, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${prettyDay(w.day)} · ${w.exCount} ${if (w.exCount == 1) "exercise" else "exercises"}", color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                            Chevron()
                        }
                    }
                }
            }
        }
    }
}
