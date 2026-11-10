package com.spotapp.personal

// Log helpers kept free of screens so they can be tested.

data class MonthGroup<T>(val month: String, val items: MutableList<T>)

// Rows must already be newest first. Each run of the same month becomes one group.
fun <T> groupByMonth(rows: List<T>, dayOf: (T) -> String): List<MonthGroup<T>> {
    val out = ArrayList<MonthGroup<T>>()
    for (w in rows) {
        val day = dayOf(w)
        val month = monthName(day.substring(0, 4).toInt(), day.substring(5, 7).toInt() - 1)
        val last = out.lastOrNull()
        if (last != null && last.month == month) last.items.add(w) else out.add(MonthGroup(month, mutableListOf(w)))
    }
    return out
}

// The newest month starts open and older ones closed, until the person taps a month.
// While searching or filtering, every month with a match is open so nothing is hidden.
fun isMonthOpen(searching: Boolean, chosen: Map<String, Boolean>, month: String, index: Int): Boolean {
    if (searching) return true
    return chosen[month] ?: (index == 0)
}
