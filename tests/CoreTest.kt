import com.spotapp.personal.*

var passed = 0
var failed = 0
fun check(name: String, ok: Boolean) { if (ok) passed++ else { failed++; println("FAIL: $name") } }
fun rejects(name: String, block: () -> Unit) {
    try { block(); failed++; println("FAIL (not rejected): $name") } catch (e: InputError) { passed++ }
}
fun <T> eq(name: String, a: T, b: T) { if (a == b) passed++ else { failed++; println("FAIL: $name  got=$a want=$b") } }

fun v5(): MutableMap<String, Any?> = linkedMapOf(
    "app" to "spot", "version" to 5L,
    "locations" to listOf(linkedMapOf("id" to 1L, "name" to "Gym A", "icon" to "gym", "color" to "#0F5E5C")),
    "workout_types" to listOf(linkedMapOf("id" to 1L, "name" to "Strength", "icon" to "barbell")),
    "exercises" to listOf(linkedMapOf("id" to 1L, "name" to "Bench", "icon" to "", "equipment" to "machine", "movement" to "push", "cues" to "Chest up\nSlow down", "archived" to 0L, "link" to "")),
    "exercise_body_parts" to listOf(linkedMapOf("exercise_id" to 1L, "part" to "chest")),
    "ex_tags" to listOf(linkedMapOf("id" to 1L, "name" to "Warm-up")),
    "exercise_ex_tags" to listOf(linkedMapOf("exercise_id" to 1L, "tag_id" to 1L)),
    "exercise_locations" to listOf(linkedMapOf("exercise_id" to 1L, "location_id" to 1L, "machine" to "Chest press")),
    "workouts" to listOf(linkedMapOf("id" to 1L, "title" to "Thu 5 Nov · Gym A", "day" to "2026-11-05", "done" to 1L, "note" to "", "location_id" to 1L, "type_id" to 1L)),
    "workout_exercises" to listOf(linkedMapOf("id" to 1L, "workout_id" to 1L, "exercise_id" to 1L, "position" to 0L, "status" to "done", "note" to "", "location_id" to null)),
    "sets" to listOf(linkedMapOf("id" to 1L, "we_id" to 1L, "position" to 0L, "weight" to 42.5, "reps" to 10L, "done" to 1L)),
    "tags" to listOf(
        linkedMapOf("id" to 1L, "name" to "Health", "kind" to "health", "parent_id" to null),
        linkedMapOf("id" to 2L, "name" to "Knee", "kind" to "health", "parent_id" to 1L),
        linkedMapOf("id" to 3L, "name" to "Left knee", "kind" to "health", "parent_id" to 2L),
    ),
    "tag_links" to listOf(linkedMapOf("id" to 1L, "tag_id" to 3L, "workout_id" to 1L, "we_id" to null, "severity" to 2L)),
    "settings" to linkedMapOf("theme" to "evening", "palette" to "moss", "onboarded" to "1", "bpArms" to "#112233", "widgetOrder" to "fun,today,garden,balance,week", "voice" to "nope"),
)
fun parse(m: Map<String, Any?>) = parseBackup(Json.write(m))
@Suppress("UNCHECKED_CAST")
fun mut(m: MutableMap<String, Any?>, key: String, f: (MutableMap<String, Any?>) -> Unit): MutableMap<String, Any?> {
    val rows = (m[key] as List<Map<String, Any?>>).map { LinkedHashMap(it) as MutableMap<String, Any?> }
    f(rows[0]); m[key] = rows; return m
}

fun main() {
    // ---- validate
    eq("trim", cleanText("  hi ", "X", 1, 5), "hi")
    rejects("empty") { cleanText("  ", "X", 1, 5) }
    rejects("long") { cleanText("abcdef", "X", 1, 5) }
    rejects("ctrl") { cleanText("a\u0001b", "X", 1, 5) }
    rejects("newline when not allowed") { cleanText("a\nb", "X", 1, 5) }
    eq("newline allowed", cleanText("a\nb", "X", 1, 5, true), "a\nb")
    rejects("not text") { cleanText(5L, "X", 1, 5) }
    eq("optional empty", cleanOptionalText(null, "X", 5), "")
    eq("int", cleanInt(5.0, "X", 0, 10), 5L)
    rejects("int frac") { cleanInt(5.5, "X", 0, 10) }
    rejects("int range") { cleanInt(11L, "X", 0, 10) }
    rejects("int nan") { cleanInt(Double.NaN, "X", 0, 10) }
    rejects("num inf") { cleanNum(Double.POSITIVE_INFINITY, "X", 0.0, 10.0) }
    rejects("oneOf") { cleanOneOf("z", "X", listOf("a", "b")) }
    eq("day ok", cleanDay("2026-11-09"), "2026-11-09")
    rejects("day bad") { cleanDay("2026-02-30") }
    rejects("day shape") { cleanDay("2026-2-3") }
    rejects("day range") { cleanDay("1999-01-01") }
    eq("param id", paramId("42"), 42L)
    rejects("param id bad") { paramId("4x") }
    rejects("param id zero") { paramId("0") }
    rejects("param id null") { paramId(null) }
    eq("param or null", paramIdOrNull("abc"), null)
    eq("parseTyped", parseTyped("12,5"), 12.5)
    eq("parseTyped bad", parseTyped("1e5"), null)
    eq("parseTyped dot", parseTyped("."), null)
    eq("link ok", cleanLink("https://example.com/a"), "https://example.com/a")
    rejects("link js") { cleanLink("javascript:alert(1)") }
    rejects("link file") { cleanLink("file:///etc/passwd") }
    rejects("link nodot") { cleanLink("http://localhost") }
    eq("link empty", cleanLink(""), "")
    eq("hex", cleanHex("#aabbcc"), "#AABBCC")
    rejects("hex bad") { cleanHex("red") }

    // ---- dates
    eq("titleDay", titleDay("2026-11-05"), "Thu 5 Nov")
    eq("monthName", monthName(2026, 10), "Nov 2026")
    eq("addDays", addDays("2026-11-30", 1), "2026-12-01")

    // ---- json
    eq("json round", Json.parse(Json.write(linkedMapOf("a" to listOf(1L, 2.5, "x\n\"y", null, true)))), linkedMapOf("a" to listOf(1L, 2.5, "x\n\"y", null, true)) as Any?)
    for (bad in listOf("{", "[1,]", "{\"a\":1,}", "{\"a\":1}x", "'a'", "{\"a\":1,\"a\":2}", "01", "1.", "\"\\q\"", "nul", "")) {
        try { Json.parse(bad); failed++; println("FAIL json accepted: $bad") } catch (e: JsonError) { passed++ }
    }
    try { Json.parse("[".repeat(100) + "]".repeat(100)); failed++; println("FAIL deep") } catch (e: JsonError) { passed++ }
    eq("json unicode", Json.parse("\"\\u00e9\""), "é" as Any?)

    // ---- backup v5
    val b = parse(v5())
    eq("b exercises", b.exercises.size, 1)
    eq("b equipment", b.exercises[0].equipment, "machine")
    eq("b cues", b.exercises[0].cues, "Chest up\nSlow down")
    eq("b sets weight", b.sets[0].weight, 42.5)
    eq("b link severity", b.links[0].severity, 2)
    eq("b we loc null", b.wes[0].locationId, null)
    eq("b settings theme", b.settings["theme"], "evening")
    eq("b settings palette", b.settings["palette"], "moss")
    eq("b settings no onboarded", b.settings.containsKey("onboarded"), false)
    eq("b settings color", b.settings["bpArms"], "#112233")
    eq("b settings order", b.settings["widgetOrder"], "fun,today,garden,balance,week")
    eq("b settings invalid dropped", b.settings.containsKey("voice"), false)
    eq("insert order", tagInsertOrder(b.tags.reversed()).map { it.id }, listOf(1L, 2L, 3L))
    rejects("not json") { parseBackup("hello") }
    rejects("empty") { parseBackup("") }
    rejects("array") { parseBackup("[]") }
    rejects("other app") { parse(v5().also { it["app"] = "tend" }) }
    rejects("version 6") { parse(v5().also { it["version"] = 6L }) }
    rejects("version text") { parse(v5().also { it["version"] = "5" }) }
    rejects("missing sets") { parse(v5().also { it.remove("sets") }) }
    rejects("big weight") { parse(mut(v5(), "sets") { it["weight"] = 99999.0 }) }
    rejects("neg reps") { parse(mut(v5(), "sets") { it["reps"] = -1L }) }
    rejects("frac reps") { parse(mut(v5(), "sets") { it["reps"] = 1.5 }) }
    rejects("bad day") { parse(mut(v5(), "workouts") { it["day"] = "2026-13-01" }) }
    rejects("bad status") { parse(mut(v5(), "workout_exercises") { it["status"] = "x" }) }
    rejects("orphan set") { parse(mut(v5(), "sets") { it["we_id"] = 9L }) }
    rejects("orphan we") { parse(mut(v5(), "workout_exercises") { it["workout_id"] = 9L }) }
    rejects("orphan loc") { parse(mut(v5(), "workouts") { it["location_id"] = 9L }) }
    rejects("we loc orphan") { parse(mut(v5(), "workout_exercises") { it["location_id"] = 9L }) }
    rejects("bad link") { parse(mut(v5(), "exercises") { it["link"] = "javascript:1" }) }
    rejects("link not text") { parse(mut(v5(), "exercises") { it["link"] = 5L }) }
    rejects("bad part") { parse(mut(v5(), "exercise_body_parts") { it["part"] = "tail" }) }
    rejects("bad icon") { parse(mut(v5(), "exercises") { it["icon"] = "../x" }) }
    rejects("bad place icon") { parse(mut(v5(), "locations") { it["icon"] = "castle" }) }
    rejects("bad place color") { parse(mut(v5(), "locations") { it["color"] = "#123456" }) }
    rejects("push w/o equipment") { parse(mut(v5(), "exercises") { it["equipment"] = "" }) }
    rejects("ctrl in name") { parse(mut(v5(), "exercises") { it["name"] = "a\u0000b" }) }
    rejects("link to group") { parse(mut(v5(), "tag_links") { it["tag_id"] = 1L }) }
    rejects("link both") { parse(mut(v5(), "tag_links") { it["we_id"] = 1L }) }
    rejects("link neither") { parse(mut(v5(), "tag_links") { it["workout_id"] = null }) }
    rejects("severity 6") { parse(mut(v5(), "tag_links") { it["severity"] = 6L }) }
    rejects("dup exercise ids") {
        val m = v5(); @Suppress("UNCHECKED_CAST") val l = m["exercises"] as List<Map<String, Any?>>; m["exercises"] = l + l; parse(m)
    }
    rejects("tag kind mismatch") {
        val m = v5(); @Suppress("UNCHECKED_CAST") val l = (m["tags"] as List<Map<String, Any?>>).map { LinkedHashMap(it) }
        l[1]["kind"] = "general"; m["tags"] = l; parse(m)
    }
    rejects("tag loop") {
        val m = v5(); @Suppress("UNCHECKED_CAST") val l = (m["tags"] as List<Map<String, Any?>>).map { LinkedHashMap(it) }
        l[0]["parent_id"] = 3L; m["tags"] = l; parse(m)
    }
    rejects("too deep tags") {
        val m = v5(); @Suppress("UNCHECKED_CAST") val l = (m["tags"] as List<Map<String, Any?>>).map { LinkedHashMap(it) }.toMutableList()
        l.add(linkedMapOf("id" to 4L, "name" to "Deep", "kind" to "health", "parent_id" to 3L)); m["tags"] = l; parse(m)
    }
    rejects("dup tag names") {
        val m = v5(); @Suppress("UNCHECKED_CAST") val l = (m["tags"] as List<Map<String, Any?>>).map { LinkedHashMap(it) }
        l[1]["name"] = "HEALTH"; m["tags"] = l; parse(m)
    }
    rejects("dup location names") {
        val m = v5(); @Suppress("UNCHECKED_CAST") val l = (m["locations"] as List<Map<String, Any?>>).map { LinkedHashMap(it) }.toMutableList()
        l.add(linkedMapOf("id" to 2L, "name" to "gym a", "icon" to "gym", "color" to "")); m["locations"] = l; parse(m)
    }
    rejects("dup body part") {
        val m = v5(); @Suppress("UNCHECKED_CAST") val l = m["exercise_body_parts"] as List<Map<String, Any?>>; m["exercise_body_parts"] = l + l; parse(m)
    }
    rejects("too large") { parseBackup("x".repeat(Limits.MAX_BACKUP_BYTES + 1)) }

    // ---- older backups
    val v1: Map<String, Any?> = linkedMapOf(
        "app" to "spot", "version" to 1L,
        "exercises" to listOf(
            linkedMapOf("id" to 1L, "name" to "Press", "machine" to "M1", "gym" to "Home Gym", "cues" to "", "archived" to 0L),
            linkedMapOf("id" to 2L, "name" to "Curl", "machine" to "", "gym" to "home gym", "cues" to "", "archived" to 0L),
            linkedMapOf("id" to 3L, "name" to "Run", "machine" to "", "gym" to "", "cues" to "", "archived" to 0L),
        ),
        "workouts" to listOf(linkedMapOf("id" to 1L, "title" to "W", "day" to "2025-01-02", "done" to 0L, "note" to "")),
        "workout_exercises" to listOf(linkedMapOf("id" to 1L, "workout_id" to 1L, "exercise_id" to 1L, "position" to 0L, "status" to "todo", "note" to "")),
        "sets" to listOf(linkedMapOf("id" to 1L, "we_id" to 1L, "position" to 0L, "weight" to 10L, "reps" to 5L, "done" to 0L)),
        "tags" to listOf(
            linkedMapOf("id" to 1L, "name" to "Sore", "kind" to "health"),
            linkedMapOf("id" to 2L, "name" to "Notes", "kind" to "general"),
        ),
        "tag_links" to listOf(linkedMapOf("id" to 1L, "tag_id" to 1L, "workout_id" to 1L, "we_id" to null, "severity" to 3L)),
    )
    val o1 = parse(v1)
    eq("v1 locations", o1.locations.size, 1)
    eq("v1 location name", o1.locations[0].name, "Home Gym")
    eq("v1 location icon", o1.locations[0].icon, "home")
    eq("v1 exloc", o1.exLocs.size, 2)
    eq("v1 equipment machine", o1.exercises[0].equipment, "machine")
    eq("v1 equipment none", o1.exercises[2].equipment, "")
    eq("v1 tag groups", o1.tags.map { it.name }.sorted(), listOf("Health", "Notes", "Notes 2", "Sore"))
    eq("v1 sore under health", o1.tags.first { it.name == "Sore" }.parentId, o1.tags.first { it.name == "Health" }.id)
    eq("v1 types empty", o1.types.size, 0)
    val o3 = parse(linkedMapOf<String, Any?>("app" to "spot", "version" to 3L) + v1.filterKeys { it !in listOf("app", "version") }.let { m ->
        m.toMutableMap().also { it["locations"] = emptyList<Any?>(); it["workout_types"] = emptyList<Any?>(); it["exercise_locations"] = emptyList<Any?>()
            it["exercises"] = listOf(linkedMapOf("id" to 1L, "name" to "Press", "icon" to "", "equipment" to "cable", "cues" to "", "archived" to 0L))
            it["tags"] = listOf(linkedMapOf("id" to 1L, "name" to "Parent", "kind" to "health", "parent_id" to null), linkedMapOf("id" to 2L, "name" to "Child", "kind" to "health", "parent_id" to 1L))
            it["tag_links"] = listOf(linkedMapOf("id" to 1L, "tag_id" to 2L, "workout_id" to 1L, "we_id" to null, "severity" to null))
            it["workouts"] = listOf(linkedMapOf("id" to 1L, "title" to "W", "day" to "2025-01-02", "done" to 0L, "note" to "", "location_id" to null, "type_id" to null))
            it["workout_exercises"] = listOf(linkedMapOf("id" to 1L, "workout_id" to 1L, "exercise_id" to 1L, "position" to 0L, "status" to "todo", "note" to "", "location_id" to null))
        }
    })
    eq("v3 keeps tree", o3.tags.size, 2)
    eq("v3 movement empty", o3.exercises[0].movement, "")

    // ---- stats
    eq("week range", periodRange(Period.WEEK, 0, "2026-11-09"), DayRange("2026-11-09", "2026-11-15"))
    eq("week range sunday", periodRange(Period.WEEK, 0, "2026-11-15"), DayRange("2026-11-09", "2026-11-15"))
    eq("week prev", periodRange(Period.WEEK, -1, "2026-11-09"), DayRange("2026-11-02", "2026-11-08"))
    eq("month range", periodRange(Period.MONTH, 0, "2026-11-09"), DayRange("2026-11-01", "2026-11-30"))
    eq("month prev jan", periodRange(Period.MONTH, -1, "2026-01-15"), DayRange("2025-12-01", "2025-12-31"))
    eq("feb leap", periodRange(Period.MONTH, 0, "2028-02-10"), DayRange("2028-02-01", "2028-02-29"))
    eq("year range", periodRange(Period.YEAR, 0, "2026-11-09"), DayRange("2026-01-01", "2026-12-31"))
    eq("all range from", periodRange(Period.ALL, 0, "2026-11-09").from, "2000-01-01")
    eq("bloom", listOf(0, 1, 3, 4, 9, 10).map { bloomLevel(it) }, listOf(0, 1, 1, 2, 2, 3))
    eq("tip none", balanceTip(0, 0).pushPct, 50)
    eq("tip even", balanceTip(5, 5).title, "Nicely balanced")
    eq("tip push", balanceTip(8, 2).title, "Leaning push")
    eq("tip pull", balanceTip(2, 8).title, "Leaning pull")
    eq("tip edge 60", balanceTip(3, 2).title, "Nicely balanced")
    val f0 = funCompare(5.0, "lb")
    eq("fun start", f0.label, "a good start"); eq("fun start progress", f0.progress, 0.25)
    val f1 = funCompare(100.0, "lb")
    eq("fun dogs", f1.label, "golden retrievers"); eq("fun count", f1.count, 1.4)
    eq("fun kg", funCompare(120.0, "kg").label, "refrigerators")
    eq("fun big", funCompare(48000.0, "lb").count, 2.0)
    eq("streak none", streakWeeks(emptyList(), "2026-11-09"), 0)
    val wk = listOf("2026-11-02", "2026-11-03", "2026-11-05", "2026-10-26", "2026-10-27", "2026-10-30", "2026-10-19", "2026-10-20")
    eq("streak 2 weeks, this week open", streakWeeks(wk, "2026-11-09"), 2)
    eq("streak counts current", streakWeeks(wk + listOf("2026-11-09", "2026-11-10", "2026-11-11"), "2026-11-11"), 3)
    eq("streak dup days", streakWeeks(listOf("2026-11-02", "2026-11-02", "2026-11-02"), "2026-11-09"), 0)
    eq("percent", enoughForPercent(4), false)
    eq("percentOf zero", percentOf(1, 0), 0)

    // ---- snapshot
    fun item(s: String, n: String, vararg d: Boolean) = WorkoutLikeItem(s, n, d.toList())
    val w = WorkoutLike(7, "Thu 5 Nov", false, "Gym A", listOf(item("done", "Squat", true, true), item("todo", "Bench", true, false, false), item("todo", "Row", false)))
    eq("today", todayInfo(w), TodayInfo(7, "Thu 5 Nov", "Gym A", 1, 3, false))
    eq("progress", progressInfo(w), ProgressInfo(7, "Thu 5 Nov", "Bench", "Set 2 of 3", "Row", 1, 3))
    eq("started", hasStarted(w), true)
    eq("not started", hasStarted(WorkoutLike(1, "x", false, "", listOf(item("todo", "A", false)))), false)
    eq("finished no progress", progressInfo(WorkoutLike(1, "x", true, "", listOf(item("done", "A", true)))), null)
    eq("empty no progress", progressInfo(WorkoutLike(1, "x", false, "", emptyList())), null)
    eq("all done", progressInfo(WorkoutLike(1, "x", false, "", listOf(item("done", "A", true))))?.exercise, "All exercises done")
    eq("null workout", progressInfo(null), null)
    eq("last set clamp", progressInfo(WorkoutLike(1, "x", false, "", listOf(item("todo", "A", true, true))))?.setText, "Set 2 of 2")
    val wd = weekDays("2026-11-09", listOf(TrainedDay("2026-11-10", "", "#112233", ""), TrainedDay("2026-11-12", "", "", "")))
    eq("weekdays letters", wd.map { it.label }.joinToString(""), "MTWTFSS")
    eq("weekdays trained", wd.map { it.trained }, listOf(false, true, false, true, false, false, false))
    eq("weekdays color", wd[1].color, "#112233"); eq("weekdays default", wd[3].color, "#0F5E5C")
    eq("record", recordInfo(PrRow(1, "Bench", "", 42.5, 8, "2026-11-05", null, "Gym A", "lb")), RecordInfo("42.5 lb", "Bench", "Nov 5 · Gym A"))
    eq("fun info", funInfo(funCompare(5.0, "lb"), "lb", "month").headline, "A good start")
    eq("fun info eyebrow", funInfo(funCompare(100.0, "lb"), "lb", "week").eyebrow, "This week you lifted")

    // ---- drag order / log groups / format / voice
    val hs = listOf(50f, 50f, 50f)
    eq("target stay", targetIndex(hs, 1, 0f), 1); eq("target down", targetIndex(hs, 0, 60f), 1); eq("target up", targetIndex(hs, 2, -120f), 0)
    eq("target past end", targetIndex(hs, 0, 999f), 2); eq("target empty", targetIndex(emptyList(), 0, 5f), 0)
    eq("move down", moveKey(listOf("a", "b", "c"), 0, 1), listOf("b", "a", "c")); eq("move off end", moveKey(listOf("a", "b"), 1, 1), null)
    eq("same order", sameOrder(listOf("a"), listOf("a")), true)
    val g = groupByMonth(listOf("2026-11-05", "2026-11-01", "2026-10-30", "2026-11-02")) { it }
    eq("groups", g.map { it.month to it.items.size }, listOf("Nov 2026" to 2, "Oct 2026" to 1, "Nov 2026" to 1))
    eq("open newest", isMonthOpen(false, emptyMap(), "Nov 2026", 0), true); eq("closed old", isMonthOpen(false, emptyMap(), "Oct 2026", 1), false)
    eq("chosen wins", isMonthOpen(false, mapOf("Oct 2026" to true), "Oct 2026", 1), true); eq("searching opens", isMonthOpen(true, emptyMap(), "Oct 2026", 3), true)
    eq("num int", num(5.0), "5"); eq("num frac", num(2.456), "2.46")
    eq("summarize", summarize(listOf(SetLite(60.0, 10), SetLite(60.0, 10), SetLite(55.0, 8)), "lb"), "60 × 10, 10 · 55 × 8 lb")
    eq("summarize empty", summarize(emptyList()), "No sets")
    eq("finish one", finishLine("gentle", 1, 1), "Nicely done. 1 set across 1 exercise, logged.")
    eq("sample callout", sampleLine("callout"), "Done. 9 sets across 3 exercises. That is what you did today.")
    eq("guess icon", guessPlaceIcon("My Home"), "home"); eq("guess default", guessPlaceIcon("Iron Works"), "gym")
    eq("card order default", readCardOrder("x,y"), HOME_WIDGETS); eq("card order ok", readCardOrder("fun,today,garden,balance,week")[0], "fun")
    eq("card order dup", readCardOrder("today,today,garden,balance,week"), HOME_WIDGETS)
    eq("setting valid", validSettingValue("palette", "gold"), true); eq("setting invalid", validSettingValue("palette", "neon"), false)
    eq("setting unknown key", validSettingValue("bogus", "on"), false); eq("setting color", validSettingValue("bpCore", "#abcdef"), true)
    eq("settings all have defaults", SETTING_KEYS.all { SETTING_DEFAULTS.containsKey(it) }, true)
    eq("defaults valid", SETTING_KEYS.all { validSettingValue(it, SETTING_DEFAULTS.getValue(it)) }, true)
    eq("push pull", pushPullOn(SETTING_DEFAULTS, "machine"), true); eq("push pull cardio", pushPullOn(SETTING_DEFAULTS, "cardio"), false)

    println("passed=$passed failed=$failed")
    if (failed > 0) System.exit(1)
}
