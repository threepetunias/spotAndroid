package com.spotapp.personal

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate

// Settings pages that touch the phone itself: app icon, phone widgets and the workout notice, back up, about, Tend.
// Every page has the Settings home button, a Help line and a Settings search entry.

// ---------- App icon ----------

private class IconChoice(val key: String, val name: String, val alias: String, val preview: Int)

private val ICON_CHOICES = listOf(
    IconChoice("A", "Barbell and spot", "IconA", R.drawable.icon_preview_a),
    IconChoice("B", "S and spot", "IconB", R.drawable.icon_preview_b),
    IconChoice("C", "Plate", "IconC", R.drawable.icon_preview_c),
)

private fun aliasName(ctx: Context, alias: String) = ComponentName(ctx, "${ctx.packageName}.$alias")

private fun currentIconKey(ctx: Context): String {
    val pm = ctx.packageManager
    for (c in ICON_CHOICES.drop(1)) {
        if (pm.getComponentEnabledSetting(aliasName(ctx, c.alias)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) return c.key
    }
    return "A"
}

// Turns the chosen launcher entry on first, then the others off, so there is never a moment with none.
private fun chooseIcon(ctx: Context, choice: IconChoice) {
    val pm = ctx.packageManager
    pm.setComponentEnabledSetting(aliasName(ctx, choice.alias), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
    for (c in ICON_CHOICES) {
        if (c.key == choice.key) continue
        pm.setComponentEnabledSetting(aliasName(ctx, c.alias), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    }
}

@Composable
fun AppIconPage() {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val c = colors()
    var chosen by remember { mutableStateOf(try { currentIconKey(ctx) } catch (e: Exception) { "A" }) }
    SPage("App icon") {
        Intro("The picture Spot shows on your home screen. Your home screen may take a moment to update.")
        Column(Modifier.padding(top = 16.dp, bottom = 24.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card).padding(horizontal = 14.dp)) {
            ICON_CHOICES.forEachIndexed { i, ic ->
                if (i > 0) Divider()
                val on = ic.key == chosen
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 80.dp).clickable(role = Role.Button) {
                        if (!on) {
                            try { chooseIcon(ctx, ic); chosen = ic.key } catch (e: Exception) {
                                app.say("The icon could not be changed", "Your phone did not accept the change. Nothing else was affected. Please try again.")
                            }
                        }
                    }.semantics { contentDescription = ic.name + if (on) ", chosen" else ""; selected = on }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Image(painterResource(ic.preview), contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)))
                    Text(ic.name, Modifier.weight(1f), color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (on) {
                        SpotIcon("check", c.accentText, 18.dp)
                        Text("Chosen", color = c.accentText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------- Phone widgets and the workout notice ----------

@Composable
fun PhonePage() {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val c = colors()
    val on = (app.settings["lockCard"] ?: "on") == "on"
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { PhoneSync.update(app) }

    fun pick(v: String) {
        app.setSetting("lockCard", v) {
            PhoneSync.update(app)
            if (v == "on" && Build.VERSION.SDK_INT >= 33 && !Notice.allowed(ctx)) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    SPage("Phone widgets") {
        Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Home screen widgets")
            Text("Press and hold an empty spot on your home screen, tap Widgets, find Spot, and pick one. There are widgets for today's workout, this week, weeks in a row, your newest record, a fun total, and one that takes you back to your workout.", color = c.muted, fontSize = 16.sp, lineHeight = 23.sp)
        }
        Column(Modifier.padding(top = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Workout notice")
            Text("After you log your first set, a notice shows your exercise and set. Tap it to return to your workout. It goes away when you finish.", color = c.muted, fontSize = 16.sp, lineHeight = 23.sp)
            ChipRows {
                Chip("On", on, { pick("on") })
                Chip("Off", !on, { pick("off") })
            }
            Text("If it does not appear, check that notifications are allowed for Spot in the Android settings.", color = c.muted, fontSize = 16.sp, lineHeight = 23.sp)
            Text("Latest answer: ${PhoneSync.status}.", color = c.muted, fontSize = 16.sp, lineHeight = 23.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigBtn("Check again", { PhoneSync.update(app) }, secondary = true)
                if (!Notice.allowed(ctx)) BigBtn("Notification settings", {
                    try {
                        ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {
                        app.say("Could not open Settings", "Open the Android Settings app, then Apps, then Spot, then Notifications.")
                    }
                }, secondary = true)
            }
        }
    }
}

// ---------- Back up ----------

// Reads at most `max` bytes, so a huge or endless file cannot fill the phone's memory.
internal fun readTextLimited(ctx: Context, uri: Uri, max: Int): String {
    val input = ctx.contentResolver.openInputStream(uri) ?: throw InputError("That file could not be opened.")
    input.use { s ->
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val n = s.read(buf)
            if (n < 0) break
            total += n
            if (total > max) throw InputError("The backup file is too large.")
            out.write(buf, 0, n)
        }
        return out.toString("UTF-8")
    }
}

@Composable
fun BackupPage() {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val c = colors()
    var busy by remember { mutableStateOf(false) }

    fun saveTo(uri: Uri) {
        busy = true
        app.read({ exportAll() }, onFail = { busy = false }) { text ->
            app.scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val out = ctx.contentResolver.openOutputStream(uri, "wt") ?: throw InputError("That place could not be written to.")
                        out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    }
                    app.say("Backup saved", "Keep the file somewhere private.")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    app.fail(e, "Backup failed")
                } finally {
                    busy = false
                }
            }
        }
    }

    fun restoreFrom(uri: Uri) {
        busy = true
        app.scope.launch {
            try {
                val text = withContext(Dispatchers.IO) { readTextLimited(ctx, uri, Limits.MAX_BACKUP_BYTES) }
                app.write({ importAll(text) }, onFail = { busy = false }) { r ->
                    app.reloadSettings()
                    PhoneSync.update(app)
                    busy = false
                    app.say("Restored", "${r.workouts} ${if (r.workouts == 1) "workout" else "workouts"} and ${r.exercises} ${if (r.exercises == 1) "exercise" else "exercises"} are back.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                busy = false
                app.fail(e, "Restore failed")
            }
        }
    }

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) saveTo(uri) }
    val opener = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) restoreFrom(uri) }

    SPage("Back up") {
        Text("Spot keeps everything on this phone. A backup file lets you keep a copy somewhere safe, or move to a new phone.", color = c.ink, fontSize = 17.sp, lineHeight = 25.sp, modifier = Modifier.padding(top = 16.dp))
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Caption("Save")
            BigBtn("Save a backup", { saver.launch("spot-backup-${LocalDate.now()}.json") }, Modifier.fillMaxWidth(), enabled = !busy)
        }
        Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Caption("Restore")
            BigBtn("Restore from a file", {
                app.ask("Restore from a file?", "This replaces everything on this phone with the contents of the file. Back up first if you are not sure.", "Choose file", destructive = true) {
                    opener.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }
            }, Modifier.fillMaxWidth(), secondary = true, enabled = !busy)
            Text("If the file is damaged or from something else, Spot says so and changes nothing.", color = c.muted, fontSize = 15.sp, lineHeight = 22.sp)
        }
        Text("Backup files include your tags and notes. Keep them somewhere private.", color = c.muted, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 24.dp, bottom = 24.dp))
    }
}

// ---------- About ----------

private fun versionName(ctx: Context): String = try {
    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
} catch (e: Exception) { "" }

@Composable
fun AboutPage() {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val c = colors()

    fun openPrivacy() {
        if (AppInfo.isPlaceholder(AppInfo.PRIVACY_URL) || !AppInfo.isHttps(AppInfo.PRIVACY_URL)) {
            app.say("Not published yet", "The privacy policy page has not been published yet."); return
        }
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppInfo.PRIVACY_URL)).addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            app.say("Could not open the page", "Check your connection and try again.")
        }
    }

    fun mail() {
        if (AppInfo.isPlaceholder(AppInfo.SUPPORT_EMAIL) || !AppInfo.isEmail(AppInfo.SUPPORT_EMAIL)) {
            app.say("Not set up yet", "A support address has not been added yet."); return
        }
        try {
            ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_EMAIL, arrayOf(AppInfo.SUPPORT_EMAIL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            app.say("Could not open email", "You can write to ${AppInfo.SUPPORT_EMAIL}.")
        }
    }

    SPage("About") {
        Heading("Spot")
        Text("Version ${versionName(ctx)}", color = c.muted, fontSize = 16.sp)
        if (!AppInfo.isPlaceholder(AppInfo.DEVELOPER_NAME)) Text("By ${AppInfo.DEVELOPER_NAME}", color = c.muted, fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))
        Heading("Your data", 20)
        Para("Everything stays on this phone. Spot has no accounts, does not send data anywhere and does not track you.")
        Heading("Not a medical device", 20)
        Para("Spot is not a medical device and does not diagnose, treat, cure, or prevent any medical condition. Talk to a doctor or other healthcare professional about pain, injury or any health concern before changing how you exercise.")
        Heading("Pictures", 20)
        Para("Many of the workout pictures are from Tabler Icons (MIT license, tabler.io/icons). Others were drawn for Spot.")
        Column(Modifier.padding(top = 26.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Caption("Links")
            BigBtn("Privacy policy", { openPrivacy() }, Modifier.fillMaxWidth(), secondary = true)
            BigBtn("Contact support", { mail() }, Modifier.fillMaxWidth(), secondary = true)
        }
    }
}

// ---------- Tend ----------

@Composable
fun TendPage() {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val c = colors()
    val on = app.settings["tend"] == "1"

    fun open() {
        val intent = try { ctx.packageManager.getLaunchIntentForPackage(AppInfo.TEND_PACKAGE) } catch (e: Exception) { null }
        if (intent == null) { app.say("Tend could not be opened", "Make sure Tend is installed on this phone."); return }
        try { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {
            app.say("Tend could not be opened", "Make sure Tend is installed on this phone.")
        }
    }

    SPage("Tend") {
        Text("Spot can open Tend with one tap. Nothing is shared between the two apps.", color = c.ink, fontSize = 17.sp, lineHeight = 25.sp, modifier = Modifier.padding(top = 16.dp))
        Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption("Link to Tend")
            ChipRows {
                Chip("Off", !on, { app.setSetting("tend", "0") })
                Chip("On", on, { app.setSetting("tend", "1") })
            }
        }
        if (on) Row(Modifier.padding(top = 20.dp)) { BigBtn("Open Tend", { open() }) }
        Text("Health tags and notes stay in Spot and are never sent anywhere. Sharing a simple “worked out today” count with Tend may come later, only if you turn it on.", color = c.muted, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 24.dp, bottom = 24.dp))
    }
}
