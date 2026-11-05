package com.spotapp.personal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Confirm(
    val title: String,
    val message: String,
    val okLabel: String,
    val cancelLabel: String? = "Cancel",
    val onOk: () -> Unit = {},
)

class AppState(val repo: Repo, val nav: NavHostController, val scope: CoroutineScope) {
    var settings by mutableStateOf(repo.getSettings())
        private set
    var confirm by mutableStateOf<Confirm?>(null)

    fun reloadSettings() {
        settings = repo.getSettings()
    }

    fun setSetting(key: String, value: String) {
        try {
            repo.setSetting(key, value)
            settings = settings + (key to value)
        } catch (e: Exception) {
            fail(e)
        }
    }

    fun fail(e: Throwable, title: String = "Could not do that") {
        val msg = if (e is InputError) (e.message ?: "That did not work.") else "Something went wrong. Please try again."
        confirm = Confirm(title, msg, "OK", null)
    }

    // Runs database work off the main thread. Errors become a plain message.
    fun <T> io(block: () -> T, then: (T) -> Unit = {}, onFail: () -> Unit = {}) {
        scope.launch {
            try {
                val r = withContext(Dispatchers.IO) { block() }
                then(r)
            } catch (e: Exception) {
                fail(e)
                onFail()
            }
        }
    }

    fun back() {
        if (!nav.popBackStack()) nav.navigate("start")
    }

    fun go(route: String) {
        nav.navigate(route)
    }
}

val LocalApp = staticCompositionLocalOf<AppState> { error("AppState is missing") }

// Route parameters are checked before use: a positive whole number or nothing.
fun paramId(v: String?): Long? {
    if (v == null || !Regex("^\\d{1,9}$").matches(v)) return null
    val n = v.toLong()
    return if (n >= 1) n else null
}
