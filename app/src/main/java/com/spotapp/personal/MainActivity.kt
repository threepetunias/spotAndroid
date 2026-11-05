package com.spotapp.personal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SpotApp() }
    }
}

private fun NavBackStackEntry.id(key: String): Long? = paramId(arguments?.getString(key))

@Composable
fun SpotApp() {
    val context = LocalContext.current
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val repo = remember { Repo(context) }
    val app = remember { AppState(repo, nav, scope) }
    val dark = systemDark()
    val colors = resolveColors(app.settings["theme"] ?: "system", app.settings["palette"] ?: "lagoon", dark)

    // First launch shows the welcome screen once.
    LaunchedEffect(app.settings["onboarded"]) {
        if (app.settings["onboarded"] != "1") nav.navigate("welcome") { launchSingleTop = true }
    }

    CompositionLocalProvider(LocalApp provides app, LocalColors provides colors) {
        NavHost(navController = nav, startDestination = "start") {
            composable("start") { StartScreen() }
            composable("welcome") { WelcomeScreen() }
            composable("help") { HelpScreen() }
            composable("workout/new") { NewWorkoutScreen() }
            composable("workout/{id}") { e -> WorkoutScreen(e.id("id")) }
            composable("add/{id}") { e -> AddExercisesScreen(e.id("id")) }
            composable("load/{id}") { e -> LoadPastScreen(e.id("id")) }
            composable("note/{id}") { e -> NoteScreen(e.id("id")) }
            composable("tagpick/{kind}/{id}") { e -> TagPickerScreen(e.arguments?.getString("kind"), e.id("id")) }
            composable("tags") { TagsScreen() }
            composable("exercises") { ExerciseListScreen() }
            composable("exercise/{id}") { e -> ExerciseEditScreen(e.arguments?.getString("id")) }
            composable("settings") { SettingsMenuScreen() }
            composable("settings/look") { LookScreen() }
            composable("settings/workouts") { WorkoutSettingsScreen() }
            composable("settings/voice") { VoiceScreen() }
            composable("settings/backup") { BackupScreen() }
            composable("settings/tend") { TendScreen() }
            composable("settings/about") { AboutScreen() }
        }
        ConfirmHost()
    }
}
