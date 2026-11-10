package com.spotapp.personal

import java.time.LocalDate

// The numbers behind the Home cards, Insights and Records, kept free of storage so they can be tested.

enum class Period(val key: String, val label: String) {
    WEEK("week", "Week"), MONTH("month", "Month"), YEAR("year", "Year"), ALL("all", "All time");
    companion object {
        fun of(key: String): Period = values().firstOrNull { it.key == key } ?: MONTH
    }
}

private const val NEVER = "2000-01-01"

data class DayRange(val from: String, val to: String)

// The first and last day of a period. Weeks start on Monday. offset -1 is the period before.
fun periodRange(p: Period, offset: Int = 0, today: String = todayStr()): DayRange {
    if (p == Period.ALL) return DayRange(NEVER, addDays(today, 36500))
    val d = LocalDate.parse(today)
    return when (p) {
        Period.WEEK -> {
            val start = d.minusDays((d.dayOfWeek.value - 1).toLong()).plusDays(7L * offset)
            DayRange(start.toString(), start.plusDays(6).toString())
        }
        Period.MONTH -> {
            val s = d.withDayOfMonth(1).plusMonths(offset.toLong())
            DayRange(s.toString(), s.plusMonths(1).minusDays(1).toString())
        }
        else -> {
            val y = d.year + offset
            DayRange(LocalDate.of(y, 1, 1).toString(), LocalDate.of(y, 12, 31).toString())
        }
    }
}

// 0 = nothing yet, 1 = a few sets, 2 = a good amount, 3 = in full bloom.
fun bloomLevel(sets: Int): Int = if (sets <= 0) 0 else if (sets <= 3) 1 else if (sets <= 9) 2 else 3

data class BalanceTip(val title: String, val tip: String, val pushPct: Int)

fun balanceTip(push: Int, pull: Int): BalanceTip {
    val total = push + pull
    if (total == 0) return BalanceTip("No push or pull yet", "Mark exercises as push or pull to see the balance.", 50)
    val pushPct = Math.round(push.toDouble() / total * 100).toInt()
    if (pushPct in 40..60) return BalanceTip("Nicely balanced", "Push and pull are close to even.", pushPct)
    return if (pushPct > 60) BalanceTip("Leaning push", "Add a pull exercise this week.", pushPct)
    else BalanceTip("Leaning pull", "Add a push exercise this week.", pushPct)
}

private class LadderStep(val label: String, val lb: Double)
private val LADDER = listOf(
    LadderStep("bags of groceries", 20.0), LadderStep("golden retrievers", 70.0), LadderStep("refrigerators", 250.0),
    LadderStep("grand pianos", 700.0), LadderStep("small cars", 2800.0), LadderStep("elephants", 6000.0), LadderStep("school buses", 24000.0),
)

data class FunCompare(
    val total: Double, val label: String, val count: Double, val nextLabel: String, val nextAt: Double, val progress: Double,
)

// total is in the person's unit; kg is changed to pounds only to pick the comparison.
fun funCompare(total: Double, unit: String): FunCompare {
    val kg = unit == "kg"
    val lb = if (kg) total * 2.20462 else total
    fun toUnit(x: Double): Double = if (kg) Math.round(x / 2.20462).toDouble() else x
    var step: LadderStep? = null
    for (s in LADDER) if (lb >= s.lb) step = s
    if (step == null) {
        val first = LADDER[0]
        return FunCompare(total, "a good start", 0.0, first.label, toUnit(first.lb), Math.min(1.0, lb / first.lb))
    }
    val count = lb / step.lb
    val nextCount = Math.floor(count) + 1
    val shown = if (count < 10) Math.round(count * 10) / 10.0 else Math.round(count).toDouble()
    return FunCompare(total, step.label, shown, step.label, toUnit(nextCount * step.lb), count - Math.floor(count))
}

// Consecutive weeks (Monday to Sunday) with at least `need` training days. The week in progress never breaks the streak.
fun streakWeeks(days: Collection<String>, today: String = todayStr(), need: Int = 3): Int {
    fun weekStart(d: String): String = periodRange(Period.WEEK, 0, d).from
    val counts = HashMap<String, Int>()
    for (d in days.toSet()) counts[weekStart(d)] = (counts[weekStart(d)] ?: 0) + 1
    var w = weekStart(today)
    var n = 0
    if ((counts[w] ?: 0) >= need) n += 1
    w = addDays(w, -7)
    while ((counts[w] ?: 0) >= need) { n += 1; w = addDays(w, -7) }
    return n
}

// Whether a ratio has enough behind it to show as a percent (5 or more), else show "2 of 3".
fun enoughForPercent(total: Int): Boolean = total >= 5

fun percentOf(part: Int, total: Int): Int = if (total <= 0) 0 else Math.round(part.toDouble() / total * 100).toInt()

data class TrainedDay(val day: String, val icon: String, val color: String, val place: String)

data class RecordRow(
    val exerciseId: Long, val name: String, val icon: String, val weight: Double, val reps: Int, val day: String, val unit: String,
    val locationId: Long?, val locationName: String, val placeIcon: String, val placeColor: String,
    val bodyParts: List<String>, val equipment: String, val movement: String, val exTagIds: List<Long>,
    val isNew: Boolean, // set inside the chosen period
)
data class RecordFilter(
    val text: String = "", val bodyParts: List<String> = emptyList(), val locationIds: List<Long> = emptyList(),
    val equipment: List<String> = emptyList(), val movement: String = "", val exTagIds: List<Long> = emptyList(), val period: Period? = null,
)
data class PrRow(
    val exerciseId: Long, val name: String, val icon: String, val weight: Double, val reps: Int, val day: String,
    val locationId: Long?, val locationName: String, val unit: String,
)
data class WeekPoint(val from: String, val to: String, val volume: Double, val sets: Map<String, Int>, val days: Int)
