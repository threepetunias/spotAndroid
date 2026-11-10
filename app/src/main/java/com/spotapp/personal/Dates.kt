package com.spotapp.personal

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun todayStr(): String = LocalDate.now().toString()

fun addDays(day: String, n: Long): String = LocalDate.parse(day).plusDays(n).toString()

fun prettyDay(day: String): String {
    val today = todayStr()
    return when (day) {
        today -> "Today"
        addDays(today, 1) -> "Tomorrow"
        addDays(today, -1) -> "Yesterday"
        else -> LocalDate.parse(day).format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    }
}

fun shortDay(day: String): String = LocalDate.parse(day).format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))

private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

// A fixed English form used in saved workout titles, so the title does not change with the phone's language.
fun titleDay(day: String): String {
    val d = LocalDate.parse(day)
    return "${WEEKDAYS[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${MONTHS[d.monthValue - 1]}"
}

fun monthName(year: Int, month0: Int): String = "${MONTHS[month0]} $year"
