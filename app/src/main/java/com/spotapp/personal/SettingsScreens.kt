package com.spotapp.personal

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// One search list for Settings. Add every new setting here with its everyday words.
data class SettingsEntry(val name: String, val keywords: String, val route: String, val pageName: String)

val SETTINGS_INDEX = listOf(
    SettingsEntry("Theme", "dark mode light mode paper evening match phone system appearance night day", "settings/look", "Look and feel"),
    SettingsEntry("Color", "palette citrus lagoon accent colors teal lime green orange persimmon", "settings/look", "Look and feel"),
    SettingsEntry("Voice", "personality whimsical cheeky gentle call me out tone messages wording", "settings/voice", "Voice"),
    SettingsEntry("Units", "lb kg pounds kilograms weight default measure", "settings/workouts", "Workouts"),
    SettingsEntry("Exercises", "machines gym gyms locations cues names step archive", "exercises", "Exercises"),
    SettingsEntry("Tags", "health issue severity injury pain tired labels", "tags", "Tags"),
    SettingsEntry("Back up", "backup restore export import file save data copy move phone", "settings/backup", "Back up"),
    SettingsEntry("Restore", "import backup file load data replace", "settings/backup", "Back up"),
    SettingsEntry("Tend", "link connect goals intentions app share", "settings/tend", "Tend"),
    SettingsEntry("Help", "how to guide replay welcome onboarding start", "help", "Help"),
    SettingsEntry("Privacy policy", "privacy data collected policy legal", "settings/about", "About"),
    SettingsEntry("Medical statement", "medical device doctor disclaimer health professional", "settings/about", "About"),
    SettingsEntry("Version", "about build number support contact", "settings/about", "About"),
)

fun searchSettings(q: String): List<SettingsEntry> {
    val t = q.trim().lowercase()
    if (t.isEmpty()) return emptyList()
    val words = t.split(Regex("\\s+"))
    return SETTINGS_INDEX.filter { e ->
        val hay = "${e.name} ${e.keywords}".lowercase()
        words.all { hay.contains(it) }
    }
}

// ---------- Settings menu ----------

@Composable
fun SettingsMenuScreen() {
    val app = LocalApp.current
    val c = colors()
    var q by remember { mutableStateOf("") }
    val topics = listOf(
        Triple("Look and feel", null as String?, "settings/look"),
        Triple("Workouts", "Default units", "settings/workouts"),
        Triple("Voice", null, "settings/voice"),
        Triple("Back up", "Save and restore a file", "settings/backup"),
        Triple("Tend", "Open Tend from Spot", "settings/tend"),
        Triple("Help", null, "help"),
        Triple("About", "Privacy policy and contact", "settings/about"),
    )
    val found = searchSettings(q)
    ScreenFrame(title = "Settings") {
        Field("Search settings", q, { q = it }, max = 40, placeholder = "Try dark, backup or kg")
        Column(Modifier.padding(top = 12.dp, bottom = 32.dp)) {
            if (q.isNotBlank()) {
                if (found.isEmpty()) Text("Nothing matches. Try another word.", color = c.muted, fontSize = 17.sp, modifier = Modifier.padding(top = 12.dp))
                found.forEachIndexed { i, e ->
                    if (i > 0) Divider()
                    MenuRow(e.name, e.pageName) { app.go(e.route) }
                }
            } else {
                topics.forEachIndexed { i, t ->
                    if (i > 0) Divider()
                    MenuRow(t.first, t.second) { app.go(t.third) }
                }
            }
        }
    }
}

// ---------- Look and feel ----------

@Composable
fun LookScreen() {
    val app = LocalApp.current
    val c = colors()
    val s = app.settings
    ScreenFrame(title = "Look and feel", floating = { SettingsHome() }) {
        Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Theme")
            ChipRows(listOf(
                chipItem("Paper", s["theme"] == "paper", { app.setSetting("theme", "paper") }),
                chipItem("Evening", s["theme"] == "evening", { app.setSetting("theme", "evening") }),
                chipItem("Match phone", s["theme"] == "system", { app.setSetting("theme", "system") }),
            ))
        }
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Color")
            ChipRows(listOf(
                chipItem("Lagoon", s["palette"] == "lagoon", { app.setSetting("palette", "lagoon") }),
                chipItem("Citrus", s["palette"] == "citrus", { app.setSetting("palette", "citrus") }),
            ))
        }
        Column(
            Modifier.padding(top = 28.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.card).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Caption("Preview")
            Text("60 lb × 8", color = c.ink, fontFamily = Serif, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(c.nowFill).padding(horizontal = 11.dp, vertical = 5.dp)) {
                    Text("3", color = c.onNowFill, fontWeight = FontWeight.Bold)
                }
                Text("Now", color = c.accentText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.cue).padding(12.dp)) {
                Text("Chest up · Pull to collarbone", color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------- Workouts ----------

@Composable
fun WorkoutSettingsScreen() {
    val app = LocalApp.current
    val c = colors()
    val s = app.settings
    ScreenFrame(title = "Workouts", floating = { SettingsHome() }) {
        Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Default units")
            ChipRows(listOf(
                chipItem("lb", s["unit"] == "lb", { app.setSetting("unit", "lb") }),
                chipItem("kg", s["unit"] == "kg", { app.setSetting("unit", "kg") }),
            ))
            Text(
                "Used for new exercises. Each exercise keeps its own unit, which you can change on the exercise.",
                color = c.muted, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

// ---------- Voice ----------

@Composable
fun VoiceScreen() {
    val app = LocalApp.current
    val c = colors()
    ScreenFrame(title = "Voice", floating = { SettingsHome() }) {
        Text("Only the words change. Counts and behavior stay the same.", color = c.muted, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.padding(top = 12.dp))
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (v in VOICES) {
                val on = app.settings["voice"] == v.key
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.card)
                        .border(2.dp, if (on) c.nowEdge else c.card, RoundedCornerShape(18.dp))
                        .clickable(role = Role.Button) { app.setSetting("voice", v.key) }
                        .semantics { selected = on; contentDescription = "${v.name}. ${v.blurb}. Sample: ${sampleLine(v.key)}" }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(v.name, Modifier.weight(1f), color = c.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        if (on) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                SpotIcon("check", c.accentText, 18.dp)
                                Text("Chosen", color = c.accentText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(v.blurb, color = c.muted, fontSize = 15.sp)
                    Text("“${sampleLine(v.key)}”", color = c.ink, fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ---------- Back up ----------

@Composable
fun BackupScreen() {
    val app = LocalApp.current
    val c = colors()
    val context = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var pendingJson by remember { mutableStateOf<String?>(null) }

    // The backup is written straight to the place the person picks, so no copy is left in the cache.
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        val json = pendingJson
        pendingJson = null
        if (uri == null || json == null) { busy = false; return@rememberLauncherForActivityResult }
        app.io({
            context.contentResolver.openOutputStream(uri, "w").use { out ->
                if (out == null) throw InputError("Spot could not write the backup there.")
                out.write(json.toByteArray(Charsets.UTF_8))
            }
        }, { busy = false }, { busy = false })
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) { busy = false; return@rememberLauncherForActivityResult }
        app.io({
            val bytes = context.contentResolver.openInputStream(uri).use { input ->
                if (input == null) throw InputError("Spot could not read that file.")
                val buf = ByteArray(Limits.MAX_BACKUP_BYTES + 1)
                var n = 0
                while (n < buf.size) {
                    val r = input.read(buf, n, buf.size - n)
                    if (r < 0) break
                    n += r
                }
                if (n > Limits.MAX_BACKUP_BYTES) throw InputError("The backup file is too large.")
                buf.copyOf(n)
            }
            app.repo.importAll(String(bytes, Charsets.UTF_8))
        }, { r ->
            busy = false
            app.reloadSettings()
            app.confirm = Confirm(
                "Restored",
                "${r.first} ${if (r.first == 1) "workout" else "workouts"} and ${r.second} ${if (r.second == 1) "exercise" else "exercises"} are back.",
                "OK", null,
            )
        }, { busy = false })
    }

    ScreenFrame(title = "Back up", floating = { SettingsHome() }) {
        Text(
            "Spot keeps everything on this phone. A backup file lets you keep a copy somewhere safe, or move to a new phone.",
            color = c.ink, fontSize = 17.sp, lineHeight = 25.sp, modifier = Modifier.padding(top = 16.dp),
        )
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Caption("Save")
            Row(Modifier.fillMaxWidth()) {
                BigBtn("Save a backup", {
                    if (!busy) {
                        busy = true
                        app.io({ app.repo.exportAll() }, { json ->
                            pendingJson = json
                            saveLauncher.launch("spot-backup-${todayStr()}.json")
                        }, { busy = false })
                    }
                }, Modifier.weight(1f), enabled = !busy)
            }
        }
        Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Caption("Restore")
            Row(Modifier.fillMaxWidth()) {
                BigBtn("Restore from a file", {
                    if (!busy) {
                        app.confirm = Confirm(
                            "Restore from a file?",
                            "This replaces everything on this phone with the contents of the file. Back up first if you are not sure.",
                            "Choose file",
                        ) { busy = true; restoreLauncher.launch(arrayOf("*/*")) }
                    }
                }, Modifier.weight(1f), secondary = true, enabled = !busy)
            }
            Text("If the file is damaged or from something else, Spot says so and changes nothing.", color = c.muted, fontSize = 15.sp, lineHeight = 22.sp)
        }
        Text(
            "Backup files include your tags and notes. Keep them somewhere private.",
            color = c.muted, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 24.dp),
        )
    }
}

// ---------- Tend ----------

@Composable
fun TendScreen() {
    val app = LocalApp.current
    val c = colors()
    val context = LocalContext.current
    val on = app.settings["tend"] == "1"
    ScreenFrame(title = "Tend", floating = { SettingsHome() }) {
        Text("Spot can open Tend with one tap. Nothing is shared between the two apps.", color = c.ink, fontSize = 17.sp, lineHeight = 25.sp, modifier = Modifier.padding(top = 16.dp))
        Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Link to Tend")
            ChipRows(listOf(
                chipItem("Off", !on, { app.setSetting("tend", "0") }),
                chipItem("On", on, { app.setSetting("tend", "1") }),
            ))
        }
        if (on) {
            Row(Modifier.padding(top = 20.dp).fillMaxWidth()) {
                BigBtn("Open Tend", {
                    val intent = context.packageManager.getLaunchIntentForPackage(Config.TEND_PACKAGE)
                    if (intent == null) {
                        app.confirm = Confirm("Tend could not be opened", "Make sure Tend is installed on this phone.", "OK", null)
                    } else {
                        try { context.startActivity(intent) } catch (e: Exception) {
                            app.confirm = Confirm("Tend could not be opened", "Make sure Tend is installed on this phone.", "OK", null)
                        }
                    }
                }, Modifier.weight(1f))
            }
        }
        Text(
            "Health tags and notes stay in Spot and are never sent anywhere. Sharing a simple “worked out today” count with Tend may come later, only if you turn it on.",
            color = c.muted, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 24.dp),
        )
    }
}

// ---------- About ----------

@Composable
fun AboutScreen() {
    val app = LocalApp.current
    val c = colors()
    val context = LocalContext.current
    ScreenFrame(title = "About", floating = { SettingsHome() }) {
        Heading("Spot")
        Text("Version ${Config.APP_VERSION}", color = c.muted, fontSize = 16.sp)
        if (!Config.isPlaceholder(Config.DEVELOPER_NAME)) Text("By ${Config.DEVELOPER_NAME}", color = c.muted, fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))
        Heading("Your data", 20)
        Para("Everything stays on this phone. Spot has no accounts, does not send data anywhere and does not track you.")
        Heading("Not a medical device", 20)
        Para("Spot is not a medical device and does not diagnose, treat, cure, or prevent any medical condition. Talk to a doctor or other healthcare professional about pain, injury or any health concern before changing how you exercise.")
        Column(Modifier.padding(top = 26.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Caption("Links")
            Row(Modifier.fillMaxWidth()) {
                BigBtn("Privacy policy", {
                    if (Config.isPlaceholder(Config.PRIVACY_URL)) {
                        app.confirm = Confirm("Not published yet", "The privacy policy page has not been published yet.", "OK", null)
                    } else {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Config.PRIVACY_URL))) } catch (e: Exception) {
                            app.confirm = Confirm("Could not open the page", "Check your connection and try again.", "OK", null)
                        }
                    }
                }, Modifier.weight(1f), secondary = true)
            }
            Row(Modifier.fillMaxWidth()) {
                BigBtn("Contact support", {
                    if (Config.isPlaceholder(Config.SUPPORT_EMAIL)) {
                        app.confirm = Confirm("Not set up yet", "A support address has not been added yet.", "OK", null)
                    } else {
                        try { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Config.SUPPORT_EMAIL}"))) } catch (e: Exception) {
                            app.confirm = Confirm("Could not open Mail", "You can write to ${Config.SUPPORT_EMAIL}.", "OK", null)
                        }
                    }
                }, Modifier.weight(1f), secondary = true)
            }
        }
    }
}
