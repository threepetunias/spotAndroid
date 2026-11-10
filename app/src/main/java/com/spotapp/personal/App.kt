package com.spotapp.personal

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

// Where the person can be. Each screen gets only what it needs, and ids are checked before a screen opens.
sealed class Route {
    object Home : Route()
    object Prs : Route()
    object Timer : Route()
    object Log : Route()
    object Insights : Route()
    data class Help(val topic: String? = null) : Route()
    object Welcome : Route()
    object Settings : Route()
    object Tags : Route()
    object Exercises : Route()
    object WorkoutNew : Route()
    object Records : Route()
    data class Topic(val key: String) : Route()
    data class Page(val name: String, val arg: Long? = null) : Route()
    data class Workout(val id: Long) : Route()
    data class WorkoutEdit(val id: Long) : Route()
    data class ExerciseEdit(val id: Long?) : Route()
    data class AddExercises(val workoutId: Long) : Route()
    data class LoadPast(val weId: Long) : Route()
    data class Note(val weId: Long) : Route()
    data class TagPicker(val workoutId: Long?, val weId: Long?) : Route()
    data class PickIcon(val current: String) : Route()
}

val TAB_ROUTES: List<Route> = listOf(Route.Home, Route.Prs, Route.Timer, Route.Log)

class Confirm(
    val title: String, val message: String, val okLabel: String,
    val cancelLabel: String? = "Cancel", val destructive: Boolean = false, val onOk: () -> Unit = {},
)

sealed class Load<out T> {
    object Busy : Load<Nothing>()
    object Failed : Load<Nothing>()
    class Ready<T>(val value: T) : Load<T>()
}

class AppState(val appContext: Context, val scope: CoroutineScope) {
    private val dbThread: CoroutineDispatcher = Executors.newSingleThreadExecutor { r -> Thread(r, "spot-db") }.asCoroutineDispatcher()
    val dispatcher: CoroutineDispatcher get() = dbThread

    private var helper: SpotDb? = null
    private var storeRef: Store? = null

    // Only used on the database thread.
    val store: Store
        get() {
            storeRef?.let { return it }
            val h = SpotDb(appContext)
            helper = h
            val s = Store(Q(h.writableDatabase))
            storeRef = s
            return s
        }

    val stack: SnapshotStateList<Route> = mutableStateListOf<Route>(Route.Home)
    val current: Route get() = stack.last()

    var settings by mutableStateOf<Map<String, String>>(SETTING_DEFAULTS)
        private set
    var ready by mutableStateOf(false)
        private set
    var loadFailed by mutableStateOf(false)
        private set

    // Goes up each time data is saved, so every open screen reloads what it shows.
    var version by mutableStateOf(0)
        private set

    var confirm by mutableStateOf<Confirm?>(null)
    var inSettings = false

    // A picture chosen on the full-screen picker, handed back to the screen that opened it.
    var pickedIcon: String? = null

    fun start() {
        loadFailed = false
        scope.launch {
            try {
                val s = withContext(dispatcher) { store.getSettings() }
                settings = s
                ready = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadFailed = true
            }
        }
    }

    // ---- navigation ----

    fun go(r: Route) {
        stack.add(r)
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.size - 1)
    }

    fun replace(r: Route) {
        if (stack.size > 1) stack.removeAt(stack.size - 1)
        stack.add(r)
    }

    // The four main tabs replace each other.
    fun tab(r: Route) {
        inSettings = false
        stack.clear()
        stack.add(r)
    }

    fun home() = tab(Route.Home)

    fun goSettings(r: Route) {
        inSettings = true
        stack.add(r)
    }

    fun toSettingsHome() {
        inSettings = true
        while (stack.size > 1 && stack.last() != Route.Settings) stack.removeAt(stack.size - 1)
        if (stack.last() != Route.Settings) stack.add(Route.Settings)
    }

    // ---- messages ----

    fun fail(e: Throwable, title: String = "Could not do that") {
        val msg = if (e is InputError) (e.message ?: "That did not work.") else "Something went wrong. Please try again."
        confirm = Confirm(title, msg, "OK", null)
    }

    fun ask(title: String, message: String, ok: String, destructive: Boolean = false, onOk: () -> Unit) {
        confirm = Confirm(title, message, ok, "Cancel", destructive, onOk)
    }

    fun say(title: String, message: String, onOk: () -> Unit = {}) {
        confirm = Confirm(title, message, "OK", null, false, onOk)
    }

    // ---- data ----

    // Runs a change on the database thread. Afterwards every open screen reloads. Errors become a plain message.
    fun <T> write(block: Store.() -> T, onFail: () -> Unit = {}, then: (T) -> Unit = {}) {
        scope.launch {
            try {
                val r = withContext(dispatcher) { store.block() }
                version += 1
                PhoneSync.update(this@AppState) // widgets and the workout notice follow every saved change
                then(r)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e)
                onFail()
            }
        }
    }

    // Reads without a reload afterwards.
    fun <T> read(block: Store.() -> T, onFail: () -> Unit = {}, then: (T) -> Unit) {
        scope.launch {
            try {
                then(withContext(dispatcher) { store.block() })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e)
                onFail()
            }
        }
    }

    fun setSetting(key: String, value: String, then: () -> Unit = {}) {
        write({ setSetting(key, value) }) { settings = settings + (key to value); then() }
    }

    fun reloadSettings() {
        read({ getSettings() }) { settings = it }
    }
}

val LocalApp = staticCompositionLocalOf<AppState> { error("AppState is missing") }

// Loads data for a screen. While it loads the first time the screen shows nothing (never a false "empty" message).
// When saved data changes, it loads again but keeps showing the old data until the new data arrives.
@Composable
fun <T> load(vararg keys: Any?, block: Store.() -> T): Load<T> {
    val app = LocalApp.current
    var state by remember { mutableStateOf<Load<T>>(Load.Busy) }
    val k = keys.toList() + app.version
    LaunchedEffect(k) {
        try {
            val v = withContext(app.dispatcher) { app.store.block() }
            state = Load.Ready(v)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (state !is Load.Ready) state = Load.Failed
        }
    }
    return state
}
