package com.spotapp.personal

import android.Manifest
import android.content.Intent
import android.os.Build
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private var pendingLink: Uri? = null
    private var app: AppState? = null

    // Asks for permission to show the workout notice (Android 13 and newer). Answered either way: the notice then updates.
    private val notifyPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { app?.let { PhoneSync.update(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Pictures.load(applicationContext)
        PhoneSync.requestPermission = { if (Build.VERSION.SDK_INT >= 33) notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        if (savedInstanceState == null) pendingLink = intent?.data
        setContent {
            val scope = rememberCoroutineScope()
            val state = remember { AppState(applicationContext, scope).also { it.start(); app = it } }
            SpotApp(state, this)
        }
    }

    override fun onDestroy() {
        PhoneSync.requestPermission = null
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingLink = intent.data
        app?.let { openLink(it) }
    }

    // Deep links (spot://workout/ID, spot://workout/new, spot://prs, spot://log). Anything else is ignored.
    fun openLink(app: AppState) {
        val uri = pendingLink ?: return
        pendingLink = null
        if (uri.scheme != "spot" || !app.ready) return
        val host = uri.host ?: return
        val seg = uri.pathSegments
        when (host) {
            "prs" -> if (seg.isEmpty()) app.tab(Route.Prs)
            "log" -> if (seg.isEmpty()) app.tab(Route.Log)
            "workout" -> {
                if (seg.size == 1 && seg[0] == "new") { app.tab(Route.Home); app.go(Route.WorkoutNew) }
                else if (seg.size == 1) {
                    val id = paramIdOrNull(seg[0])
                    if (id != null) { app.tab(Route.Home); app.go(Route.Workout(id)) }
                }
            }
        }
    }

    fun linkWaiting(): Boolean = pendingLink != null
}

@Composable
fun SpotApp(app: AppState, activity: MainActivity) {
    val dark = isSystemInDarkTheme()
    val colors = resolveColors(app.settings["theme"] ?: "system", app.settings["palette"] ?: "lagoon", dark)

    LaunchedEffect(app.ready) {
        if (app.ready) {
            if (activity.linkWaiting()) activity.openLink(app)
            else if (app.settings["onboarded"] != "1") app.go(Route.Welcome)
        }
    }
    BackHandler(enabled = app.stack.size > 1) { app.back() }

    CompositionLocalProvider(LocalApp provides app, LocalColors provides colors) {
        Box(Modifier.fillMaxSize().background(colors.bg)) {
            if (app.loadFailed) {
                Column(Modifier.statusBarsPadding().padding(24.dp)) {
                    Text("Spot could not open its data. Your data has not been changed. Close the app and open it again.", color = colors.ink, fontSize = 18.sp, lineHeight = 26.sp)
                }
            } else if (app.ready) {
                StackHost(app)
            }
            ConfirmHost()
        }
    }
}

val LocalIsTop = androidx.compose.runtime.compositionLocalOf { true }

// Every screen on the back stack stays composed, so what was typed on a form is still there after a picker closes.
// Only the top one is placed on screen, so the others take no space, draw nothing and cannot be touched.
@Composable
fun StackHost(app: AppState) {
    Box(Modifier.fillMaxSize()) {
        app.stack.forEachIndexed { i, r ->
            androidx.compose.runtime.key(i, r) {
                val top = i == app.stack.size - 1
                CompositionLocalProvider(LocalIsTop provides top) {
                    Box(
                        if (top) Modifier.fillMaxSize()
                        else Modifier.fillMaxSize().layout { m, c -> m.measure(c); layout(0, 0) {} }
                            .clearAndSetSemantics { },
                    ) { RouteHost(r) }
                }
            }
        }
    }
}

@Composable
fun RouteHost(r: Route) {
    when (r) {
        Route.Home -> HomeScreen()
        Route.Prs -> PrsScreen()
        Route.Timer -> TimerScreen()
        Route.Log -> LogScreen()
        Route.Insights -> InsightsScreen()
        is Route.Help -> HelpScreen(r.topic)
        Route.Welcome -> WelcomeScreen()
        Route.Settings -> SettingsMenuScreen()
        Route.Tags -> TagsScreen()
        Route.Exercises -> ExercisesScreen()
        Route.WorkoutNew -> WorkoutFormScreen(null)
        Route.Records -> RecordsScreen()
        is Route.Topic -> SettingsTopicScreen(r.key)
        is Route.Page -> SettingsPage(r.name, r.arg)
        is Route.Workout -> WorkoutScreen(r.id)
        is Route.WorkoutEdit -> WorkoutFormScreen(r.id)
        is Route.ExerciseEdit -> ExerciseEditScreen(r.id)
        is Route.AddExercises -> AddExercisesScreen(r.workoutId)
        is Route.LoadPast -> LoadPastScreen(r.weId)
        is Route.Note -> NoteScreen(r.weId)
        is Route.TagPicker -> TagPickerScreen(r.workoutId, r.weId)
        is Route.PickIcon -> PickIconScreen(r.current)
    }
}
