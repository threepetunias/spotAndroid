package com.spotapp.personal

// Builds the small snapshot the phone widgets and the "workout in progress" notification show.
// Pure functions: no storage, no Android libraries, so they can be tested. Matches the iPhone snapshot field for field.

data class TodayInfo(val id: Long, val title: String, val place: String, val done: Int, val total: Int, val finished: Boolean)
data class ProgressInfo(val id: Long, val title: String, val exercise: String, val setText: String, val next: String, val done: Int, val total: Int)
data class WeekDay(val label: String, val trained: Boolean, val color: String)
data class RecordInfo(val weight: String, val exercise: String, val dateLabel: String)
data class FunInfo(val eyebrow: String, val headline: String, val caption: String)
data class Snapshot(
    val day: String, val today: TodayInfo?, val progress: ProgressInfo?, val week: List<WeekDay>, val weekCount: Int,
    val streak: Int, val record: RecordInfo?, val fun_: FunInfo?,
)

class WorkoutLikeItem(val status: String, val exerciseName: String, val setsDone: List<Boolean>)
class WorkoutLike(val id: Long, val title: String, val done: Boolean, val locationName: String, val items: List<WorkoutLikeItem>)

fun WorkoutFull.asLike(): WorkoutLike =
    WorkoutLike(id, title, done, locationName, items.map { WorkoutLikeItem(it.status, it.exercise.name, it.sets.map { s -> s.done }) })

private const val DEFAULT_DOT = "#0F5E5C"
private val WEEK_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

fun todayInfo(w: WorkoutLike?): TodayInfo? {
    if (w == null) return null
    val done = w.items.count { it.status == "done" }
    return TodayInfo(w.id, w.title, w.locationName, done, w.items.size, w.done)
}

// The workout being done now: today's, not finished, with at least one exercise. Null when there is none.
fun progressInfo(w: WorkoutLike?): ProgressInfo? {
    if (w == null || w.done || w.items.isEmpty()) return null
    val doneEx = w.items.count { it.status == "done" }
    val at = w.items.indexOfFirst { it.status == "todo" }
    if (at < 0) return ProgressInfo(w.id, w.title, "All exercises done", "Finish when you are ready", "", doneEx, w.items.size)
    val cur = w.items[at]
    val logged = cur.setsDone.count { it }
    val planned = Math.max(cur.setsDone.size, 1)
    val upcoming = w.items.drop(at + 1).firstOrNull { it.status == "todo" }
    return ProgressInfo(
        w.id, w.title, cur.exerciseName, "Set ${Math.min(logged + 1, planned)} of $planned",
        upcoming?.exerciseName ?: "", doneEx, w.items.size,
    )
}

// True once a set has been logged: that is when the notification appears.
fun hasStarted(w: WorkoutLike?): Boolean = w != null && !w.done && w.items.any { i -> i.setsDone.any { it } }

// Monday to Sunday of the week that starts on `weekStart`, with the days that were trained.
fun weekDays(weekStart: String, trained: List<TrainedDay>): List<WeekDay> {
    val byDay = HashMap<String, String>()
    for (t in trained) if (!byDay.containsKey(t.day)) byDay[t.day] = t.color
    return WEEK_LETTERS.mapIndexed { i, label ->
        val color = byDay[addDays(weekStart, i.toLong())]
        WeekDay(label, color != null, if (color != null && color.isNotEmpty()) color else DEFAULT_DOT)
    }
}

fun recordInfo(r: PrRow?): RecordInfo? {
    if (r == null) return null
    val date = listOf(shortDay(r.day), r.locationName).filter { it.isNotEmpty() }.joinToString(" · ")
    return RecordInfo("${num(r.weight)} ${r.unit}", r.name, date)
}

fun funInfo(fun_: FunCompare, unit: String, period: String): FunInfo {
    val eyebrow = when (period) {
        "week" -> "This week you lifted"; "month" -> "This month you lifted"; "year" -> "This year you lifted"; else -> "You lifted"
    }
    val headline = if (fun_.count == 0.0) "A good start" else "About ${num(fun_.count)} ${fun_.label}"
    return FunInfo(eyebrow, headline, "${java.text.NumberFormat.getIntegerInstance().format(Math.round(fun_.total))} $unit in total")
}
