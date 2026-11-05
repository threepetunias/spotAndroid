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
