package com.spotapp.personal

// Pure order maths for drag-to-reorder lists (kept apart from the screens so it can be tested).

fun sameOrder(a: List<String>, b: List<String>): Boolean = a == b

// Where the held row lands, given its start index, how far it moved and the row heights (including gap).
fun targetIndex(heights: List<Float>, from: Int, dy: Float): Int {
    val n = heights.size
    if (n == 0 || from < 0 || from >= n) return from
    var top = 0f
    for (i in 0 until from) top += heights[i]
    val centre = top + heights[from] / 2f + dy
    var acc = 0f
    for (i in 0 until n) {
        if (centre < acc + heights[i] || i == n - 1) return i
        acc += heights[i]
    }
    return from
}

// New key order after moving the row at `index` by `by` places, or null when it cannot move.
fun moveKey(keys: List<String>, index: Int, by: Int): List<String>? {
    val to = index + by
    if (index < 0 || index >= keys.size || to < 0 || to >= keys.size) return null
    val next = keys.toMutableList()
    val one = next.removeAt(index)
    next.add(to, one)
    return next
}
