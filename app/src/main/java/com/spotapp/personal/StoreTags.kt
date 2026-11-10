package com.spotapp.personal

// Tags on workouts and exercises-in-a-workout. A tag with no parent is a group (for example Health).

private class TagRow(val id: Long, val name: String, val kind: String, val parentId: Long?)

private fun tagRow(c: android.database.Cursor) = TagRow(c.long(0), c.str(1), c.str(2), c.longOrNull(3))

// All tags in tree order: each group, then its tags, then their sub-tags.
fun Store.listTags(): List<Tag> {
    val rows = q.map("SELECT id, name, kind, parent_id FROM tags ORDER BY name COLLATE NOCASE") { tagRow(it) }
    val kids = HashMap<Long?, MutableList<TagRow>>()
    for (r in rows) kids.getOrPut(r.parentId) { ArrayList() }.add(r)
    val out = ArrayList<Tag>()
    val seen = HashSet<Long>()
    fun walk(parent: Long?, depth: Int) {
        for (r in kids[parent] ?: emptyList<TagRow>()) {
            if (r.id in seen || depth > 3) continue
            seen.add(r.id)
            out.add(Tag(r.id, r.name, if (r.kind == "health") "health" else "general", r.parentId, depth))
            walk(r.id, depth + 1)
        }
    }
    // groups first: health, then the rest
    val roots = kids[null] ?: mutableListOf()
    kids[null] = (roots.filter { it.kind == "health" } + roots.filter { it.kind != "health" }).toMutableList()
    walk(null, 1)
    return out
}

// For a group, the name and kind can change. A new tag goes under a parent; the kind comes from the group.
fun Store.saveTag(id: Long?, name: String, kind: String?, parentId: Long?): Long {
    val n = cleanText(name, "Tag name", 1, Limits.TAG)
    val dup = q.first("SELECT id FROM tags WHERE name = ? COLLATE NOCASE", n) { it.long(0) }
    if (dup != null && dup != id) throw InputError("A tag with that name already exists.")
    if (id != null) {
        val i = cleanInt(id, "Tag", 1, 1_000_000_000)
        val cur = q.first("SELECT id, name, kind, parent_id FROM tags WHERE id = ?", i) { tagRow(it) }
            ?: throw InputError("That tag no longer exists.")
        if (cur.parentId == null && kind != null) {
            val k = cleanOneOf(kind, "Group type", listOf("health", "general"))
            q.tx {
                q.exec("UPDATE tags SET name = ?, kind = ? WHERE id = ?", n, k, i)
                q.exec("UPDATE tags SET kind = ? WHERE parent_id = ?", k, i)
                q.exec("UPDATE tags SET kind = ? WHERE parent_id IN (SELECT id FROM tags WHERE parent_id = ?)", k, i)
            }
        } else {
            q.exec("UPDATE tags SET name = ? WHERE id = ?", n, i)
        }
        return i
    }
    if (parentId == null) {
        val k = cleanOneOf(kind ?: "general", "Group type", listOf("health", "general"))
        return q.insert("INSERT INTO tags (name, kind, parent_id) VALUES (?, ?, NULL)", n, k)
    }
    val pid = cleanInt(parentId, "Group", 1, 1_000_000_000)
    val parent = q.first("SELECT id, name, kind, parent_id FROM tags WHERE id = ?", pid) { tagRow(it) }
        ?: throw InputError("That group no longer exists.")
    val grand = if (parent.parentId == null) null
    else q.first("SELECT id, name, kind, parent_id FROM tags WHERE id = ?", parent.parentId) { tagRow(it) }
    if (grand != null && grand.parentId != null) throw InputError("Tags can only go three levels deep.")
    return q.insert("INSERT INTO tags (name, kind, parent_id) VALUES (?, ?, ?)", n, if (parent.kind == "health") "health" else "general", pid)
}

// Deleting a group or tag also deletes what is under it, and removes it from workouts.
fun Store.deleteTag(id: Long) {
    q.exec("DELETE FROM tags WHERE id = ?", cleanInt(id, "Tag", 1, 1_000_000_000))
}

// "Health › Knee pain › Left knee"
fun Store.tagPaths(): Map<Long, String> {
    val all = listTags()
    val byId = all.associateBy { it.id }
    val out = HashMap<Long, String>()
    for (t in all) {
        val parts = ArrayList<String>()
        parts.add(t.name)
        var p = t.parentId
        var guard = 0
        while (p != null && guard++ < 4) {
            val x = byId[p] ?: break
            parts.add(0, x.name)
            p = x.parentId
        }
        out[t.id] = parts.joinToString(" › ")
    }
    return out
}

// The id and everything under it. Used by Log filters.
fun tagWithChildren(all: List<Tag>, id: Long): List<Long> {
    val out = arrayListOf(id)
    repeat(3) { for (t in all) if (t.parentId != null && t.parentId in out && t.id !in out) out.add(t.id) }
    return out
}

sealed class TagTarget {
    class OfWorkout(val id: Long) : TagTarget()
    class OfItem(val id: Long) : TagTarget()
}

fun Store.getLinks(target: TagTarget, paths: Map<Long, String>? = null): List<TagLink> {
    val (where, id) = when (target) {
        is TagTarget.OfWorkout -> "l.workout_id = ?" to target.id
        is TagTarget.OfItem -> "l.we_id = ?" to target.id
    }
    val rows = q.map(
        "SELECT l.tag_id, t.name, t.kind, l.severity FROM tag_links l JOIN tags t ON t.id = l.tag_id WHERE $where ORDER BY t.kind, t.name COLLATE NOCASE",
        id,
    ) { listOf(it.long(0).toString(), it.str(1), it.str(2), if (it.isNull(3)) "" else it.getInt(3).toString()) }
    if (rows.isEmpty()) return emptyList()
    val p = paths ?: tagPaths()
    return rows.map {
        val tid = it[0].toLong()
        TagLink(tid, it[1], if (it[2] == "health") "health" else "general", it[3].toIntOrNull(), p[tid] ?: it[1])
    }
}

fun Store.setLinks(target: TagTarget, links: List<Pair<Long, Int?>>) {
    val clean = links.map { (t, s) -> cleanInt(t, "Tag", 1, 1_000_000_000) to (if (s == null) null else cleanInt(s, "Severity", 1, 5).toInt()) }
    val isWorkout = target is TagTarget.OfWorkout
    val id = when (target) { is TagTarget.OfWorkout -> target.id; is TagTarget.OfItem -> target.id }
    for ((tid, _) in clean) {
        val parent = q.first("SELECT parent_id FROM tags WHERE id = ?", tid) { it.longOrNull(0) to true }
            ?: throw InputError("That tag no longer exists.")
        if (parent.first == null) throw InputError("Pick a tag inside the group, not the group itself.")
    }
    q.tx {
        q.exec(if (isWorkout) "DELETE FROM tag_links WHERE workout_id = ?" else "DELETE FROM tag_links WHERE we_id = ?", id)
        for ((tid, sev) in clean) {
            q.exec(
                if (isWorkout) "INSERT INTO tag_links (tag_id, workout_id, severity) VALUES (?, ?, ?)"
                else "INSERT INTO tag_links (tag_id, we_id, severity) VALUES (?, ?, ?)",
                tid, id, sev,
            )
        }
    }
}
