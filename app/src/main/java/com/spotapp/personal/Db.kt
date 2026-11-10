package com.spotapp.personal

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteProgram

// Storage. Every query uses bound parameters (?). No SQL is built by joining user text.
// Placeholder lists are built only from a count of ids that were already checked as numbers.
// The tables match the iPhone app's data model version 5, so a backup file moves between the phones.

const val DB_VERSION = 5

private fun bindAll(p: SQLiteProgram, args: Array<out Any?>) {
    for ((i, a) in args.withIndex()) {
        val n = i + 1
        when (a) {
            null -> p.bindNull(n)
            is Long -> p.bindLong(n, a)
            is Int -> p.bindLong(n, a.toLong())
            is Boolean -> p.bindLong(n, if (a) 1L else 0L)
            is Double -> p.bindDouble(n, a)
            is String -> p.bindString(n, a)
            else -> throw IllegalArgumentException("Unsupported value type")
        }
    }
}

class SpotDb(context: Context) : SQLiteOpenHelper(context.applicationContext, "spot.db", null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    // A fresh install is created at the newest version. Later versions are added as numbered steps in onUpgrade;
    // an old step is never edited and nothing is dropped.
    override fun onCreate(db: SQLiteDatabase) {
        for (sql in SCHEMA) db.execSQL(sql)
        // The two starting tag groups, as on the iPhone.
        db.execSQL("INSERT INTO tags (name, kind, parent_id) VALUES ('Health', 'health', NULL)")
        db.execSQL("INSERT INTO tags (name, kind, parent_id) VALUES ('Notes', 'general', NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 5 is the first version of this app. Future steps go here, for example: if (oldVersion < 6) { ... }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw IllegalStateException("This data was made by a newer version of Spot.")
    }
}

// A thin wrapper so every read and write binds typed values (numbers stay numbers).
class Q(val db: SQLiteDatabase) {
    fun query(sql: String, vararg args: Any?): Cursor =
        db.rawQueryWithFactory({ _, driver, editTable, query ->
            bindAll(query, args)
            SQLiteCursor(driver, editTable, query)
        }, sql, null, null)

    fun exec(sql: String, vararg args: Any?) {
        db.compileStatement(sql).use { s -> bindAll(s, args); s.executeUpdateDelete() }
    }

    fun insert(sql: String, vararg args: Any?): Long =
        db.compileStatement(sql).use { s -> bindAll(s, args); s.executeInsert() }

    inline fun <T> map(sql: String, vararg args: Any?, f: (Cursor) -> T): List<T> {
        query(sql, *args).use { c ->
            val out = ArrayList<T>(c.count)
            while (c.moveToNext()) out.add(f(c))
            return out
        }
    }

    inline fun <T> first(sql: String, vararg args: Any?, f: (Cursor) -> T): T? {
        query(sql, *args).use { c -> return if (c.moveToFirst()) f(c) else null }
    }

    fun count(sql: String, vararg args: Any?): Int = first(sql, *args) { it.getInt(0) } ?: 0

    inline fun <T> tx(block: () -> T): T {
        db.beginTransaction()
        try {
            val r = block()
            db.setTransactionSuccessful()
            return r
        } finally {
            db.endTransaction()
        }
    }
}

fun Cursor.str(i: Int): String = if (isNull(i)) "" else getString(i)
fun Cursor.long(i: Int): Long = getLong(i)
fun Cursor.longOrNull(i: Int): Long? = if (isNull(i)) null else getLong(i)
fun Cursor.dbl(i: Int): Double = getDouble(i)

private val SCHEMA = listOf(
    """CREATE TABLE locations (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE COLLATE NOCASE,
        icon TEXT NOT NULL DEFAULT '', color TEXT NOT NULL DEFAULT '')""",
    """CREATE TABLE workout_types (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE COLLATE NOCASE,
        icon TEXT NOT NULL DEFAULT '')""",
    """CREATE TABLE exercises (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        machine TEXT NOT NULL DEFAULT '',
        gym TEXT NOT NULL DEFAULT '',
        unit TEXT NOT NULL DEFAULT 'lb',
        step REAL NOT NULL DEFAULT 5,
        cues TEXT NOT NULL DEFAULT '',
        archived INTEGER NOT NULL DEFAULT 0,
        icon TEXT NOT NULL DEFAULT '',
        equipment TEXT NOT NULL DEFAULT '',
        movement TEXT NOT NULL DEFAULT '',
        link TEXT NOT NULL DEFAULT ''
    )""",
    """CREATE TABLE exercise_locations (
        exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
        location_id INTEGER NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
        machine TEXT NOT NULL DEFAULT '',
        PRIMARY KEY (exercise_id, location_id)
    )""",
    """CREATE TABLE workouts (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title TEXT NOT NULL,
        day TEXT NOT NULL,
        done INTEGER NOT NULL DEFAULT 0,
        note TEXT NOT NULL DEFAULT '',
        location_id INTEGER REFERENCES locations(id) ON DELETE SET NULL,
        type_id INTEGER REFERENCES workout_types(id) ON DELETE SET NULL
    )""",
    """CREATE TABLE workout_exercises (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        workout_id INTEGER NOT NULL REFERENCES workouts(id) ON DELETE CASCADE,
        exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
        position INTEGER NOT NULL,
        status TEXT NOT NULL DEFAULT 'todo',
        note TEXT NOT NULL DEFAULT '',
        location_id INTEGER REFERENCES locations(id) ON DELETE SET NULL
    )""",
    """CREATE TABLE sets (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        we_id INTEGER NOT NULL REFERENCES workout_exercises(id) ON DELETE CASCADE,
        position INTEGER NOT NULL,
        weight REAL NOT NULL DEFAULT 0,
        reps INTEGER NOT NULL DEFAULT 0,
        done INTEGER NOT NULL DEFAULT 0
    )""",
    """CREATE TABLE tags (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL UNIQUE,
        kind TEXT NOT NULL DEFAULT 'general',
        parent_id INTEGER REFERENCES tags(id) ON DELETE CASCADE
    )""",
    """CREATE TABLE tag_links (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
        workout_id INTEGER REFERENCES workouts(id) ON DELETE CASCADE,
        we_id INTEGER REFERENCES workout_exercises(id) ON DELETE CASCADE,
        severity INTEGER,
        CHECK ((workout_id IS NULL) <> (we_id IS NULL)),
        CHECK (severity IS NULL OR (severity >= 1 AND severity <= 5))
    )""",
    """CREATE TABLE exercise_body_parts (
        exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
        part TEXT NOT NULL,
        PRIMARY KEY (exercise_id, part)
    )""",
    "CREATE TABLE ex_tags (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE COLLATE NOCASE)",
    """CREATE TABLE exercise_ex_tags (
        exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
        tag_id INTEGER NOT NULL REFERENCES ex_tags(id) ON DELETE CASCADE,
        PRIMARY KEY (exercise_id, tag_id)
    )""",
    "CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)",
    "CREATE INDEX idx_we_workout ON workout_exercises(workout_id)",
    "CREATE INDEX idx_we_exercise ON workout_exercises(exercise_id)",
    "CREATE INDEX idx_sets_we ON sets(we_id)",
    "CREATE INDEX idx_links_workout ON tag_links(workout_id)",
    "CREATE INDEX idx_links_we ON tag_links(we_id)",
    "CREATE INDEX idx_workouts_day ON workouts(day)",
    "CREATE INDEX idx_exloc_location ON exercise_locations(location_id)",
    "CREATE INDEX idx_tags_parent ON tags(parent_id)",
    "CREATE INDEX idx_exbp_part ON exercise_body_parts(part)",
    "CREATE INDEX idx_exet_tag ON exercise_ex_tags(tag_id)",
)
