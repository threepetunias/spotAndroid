package com.spotapp.personal

// One wording table per personality, matched in meaning with the iPhone app.
// Only the words change, never the counts or behavior. No personality counts misses or shames.

data class VoiceInfo(val key: String, val name: String, val blurb: String)

val VOICES = listOf(
    VoiceInfo("whimsical", "Whimsical", "Playful and imaginative"),
    VoiceInfo("cheeky", "Cheeky", "Teasing and light, never mean"),
    VoiceInfo("gentle", "Gentle", "Soft, warm and unhurried"),
    VoiceInfo("callout", "Call me out", "Direct and honest, without shaming"),
)

fun finishLine(v: String, sets: Int, exercises: Int): String {
    val counts = "$sets ${if (sets == 1) "set" else "sets"} across $exercises ${if (exercises == 1) "exercise" else "exercises"}"
    return when (v) {
        "whimsical" -> "The weights have been spotted. $counts, logged."
        "cheeky" -> "Look at you. $counts. The machines say thanks."
        "callout" -> "Done. $counts. That is what you did today."
        else -> "Nicely done. $counts, logged."
    }
}

fun sampleLine(v: String): String = finishLine(v, 9, 3)
