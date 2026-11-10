package com.spotapp.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The Settings menu: a short list of topics. A topic with several pages opens a small page of tiles;
// a topic with one page opens that page straight away. Same topics and wording as the iPhone app.

class SettingsTile(val title: String, val icon: String, val route: Route)
class SettingsTopic(val key: String, val title: String, val icon: String, val color: Color, val tiles: List<SettingsTile>, val route: Route? = null)

private fun pg(name: String, arg: Long? = null) = Route.Page(name, arg)

val SETTINGS_TOPICS: List<SettingsTopic> = listOf(
    SettingsTopic("look", "Look and feel", "palette", Color(0xFFB4532F), listOf(
        SettingsTile("Theme and color", "palette", pg("look")), SettingsTile("Voice", "mic", pg("voice")), SettingsTile("App icon", "star", pg("app-icon")))),
    SettingsTopic("workouts", "Workouts", "dumbbell", Color(0xFF0F5E5C), listOf(
        SettingsTile("Units and buttons", "dumbbell", pg("workouts")), SettingsTile("Workout types", "list", pg("workout-types")))),
    SettingsTopic("places", "Places", "pin", Color(0xFF4F8A7B), emptyList(), pg("locations")),
    SettingsTopic("exercises", "Exercises", "body", Color(0xFF7C6BA6), listOf(
        SettingsTile("Exercises", "machine", Route.Exercises), SettingsTile("Body parts", "body", pg("body-parts")),
        SettingsTile("Exercise tags", "tag", pg("exercise-tags")), SettingsTile("Push and pull", "race", pg("push-pull")),
        SettingsTile("Tags", "notes", Route.Tags))),
    SettingsTopic("home", "Home and PRs", "grid", Color(0xFFC2571A), listOf(
        SettingsTile("Home cards", "grid", pg("home-widgets")), SettingsTile("PR Records Style", "trophy", pg("records-style")),
        SettingsTile("Phone widgets", "dumbbell", pg("phone")))),
    SettingsTopic("backup", "Back up", "save", Color(0xFF4A7FA6), emptyList(), pg("backup")),
    SettingsTopic("help", "Help", "help", Color(0xFF8A2D20), emptyList(), Route.Help()),
    SettingsTopic("about", "About", "cog", Color(0xFF4E3326), listOf(
        SettingsTile("Privacy and contact", "help", pg("about")), SettingsTile("Tend", "link", pg("tend")))),
)

// One search list for Settings. Add every new setting here with its everyday words.
class SettingsEntry(val name: String, val keywords: String, val route: Route, val pageName: String)

val SETTINGS_INDEX: List<SettingsEntry> = listOf(
    SettingsEntry("Theme", "dark mode light mode paper evening match phone system appearance night day", pg("look"), "Theme and color"),
    SettingsEntry("Color", "palette accent colors theme background buttons", pg("look"), "Theme and color"),
    SettingsEntry("Lagoon", "color teal blue green sea", pg("look"), "Theme and color"),
    SettingsEntry("Citrus", "color lime yellow green", pg("look"), "Theme and color"),
    SettingsEntry("Moss", "color green forest olive", pg("look"), "Theme and color"),
    SettingsEntry("Sky", "color blue light", pg("look"), "Theme and color"),
    SettingsEntry("Orange", "color tangerine persimmon", pg("look"), "Theme and color"),
    SettingsEntry("Terracotta", "color clay brown red earth", pg("look"), "Theme and color"),
    SettingsEntry("Lilac", "color purple violet lavender", pg("look"), "Theme and color"),
    SettingsEntry("Rose", "color pink", pg("look"), "Theme and color"),
    SettingsEntry("Cherry", "color red crimson", pg("look"), "Theme and color"),
    SettingsEntry("Gold", "color yellow mustard amber", pg("look"), "Theme and color"),
    SettingsEntry("Weight buttons", "plus minus step increase weight 1 lb 5 lb 2.5 kg jump amount increment", pg("workouts"), "Units and buttons"),
    SettingsEntry("Insights", "trends charts weeks months days trained weight lifted body parts push pull statistics progress", Route.Insights, "Insights"),
    SettingsEntry("App icon", "icon home screen picture logo barbell s plate change choose alternate launcher", pg("app-icon"), "App icon"),
    SettingsEntry("Phone widgets", "widget home screen notification workout in progress back to workout add workout week streak record fun total today", pg("phone"), "Phone widgets"),
    SettingsEntry("Workout in progress notice", "ongoing notification workout in progress return lock screen on off", pg("phone"), "Phone widgets"),
    SettingsEntry("Voice", "personality whimsical cheeky gentle call me out tone messages wording", pg("voice"), "Voice"),
    SettingsEntry("Units", "lb kg pounds kilograms weight default measure", pg("workouts"), "Units and buttons"),
    SettingsEntry("Home cards", "widgets home screen today workout garden push pull balance week streak fun total on off order reorder style look", pg("home-widgets"), "Home cards"),
    SettingsEntry("Muscle garden", "flowers body parts trained home card widget period", pg("home-widgets"), "Home cards"),
    SettingsEntry("Fun total", "elephants cars weight lifted compare home card widget", pg("home-widgets"), "Home cards"),
    SettingsEntry("PR Records Style", "prs records personal best night sky stars mountain range skyline ghost race plain list time period color", pg("records-style"), "PR Records Style"),
    SettingsEntry("Body parts", "arms shoulders back chest core legs glutes feet lungs colors color pictures muscle", pg("body-parts"), "Body parts"),
    SettingsEntry("Exercise tags", "labels my own tags warm-up rehab exercises filter", pg("exercise-tags"), "Exercise tags"),
    SettingsEntry("Place pictures and colors", "places gym home park pool track climbing yoga trail court icon color", pg("locations"), "Places"),
    SettingsEntry("Exercise link", "web address url video website open link exercise", Route.Exercises, "Exercises"),
    SettingsEntry("Places", "locations gym home outdoors where studio park", pg("locations"), "Places"),
    SettingsEntry("Workout types", "strength cardio yoga pilates walking swimming kind category pictures icons", pg("workout-types"), "Workout types"),
    SettingsEntry("Pictures", "icons images symbols exercise workout machine", pg("workout-types"), "Workout types"),
    SettingsEntry("Push and pull", "push pull movement machine cable free weights body weight cardio equipment on off exercise", pg("push-pull"), "Push and pull"),
    SettingsEntry("Exercises", "equipment machines places cues names pictures icons archive delete", Route.Exercises, "Exercises"),
    SettingsEntry("Tags", "groups sub-tags health issue severity injury pain tired labels notes", Route.Tags, "Tags"),
    SettingsEntry("Back up", "backup restore export import file save data copy move phone", pg("backup"), "Back up"),
    SettingsEntry("Restore", "import backup file load data replace", pg("backup"), "Back up"),
    SettingsEntry("Tend", "link connect goals intentions app share", pg("tend"), "Tend"),
    SettingsEntry("Help", "how to guide replay welcome onboarding start", Route.Help(), "Help"),
    SettingsEntry("Privacy policy", "privacy data collected policy legal", pg("about"), "About"),
    SettingsEntry("Medical statement", "medical device doctor disclaimer health professional", pg("about"), "About"),
    SettingsEntry("Version", "about build number support contact", pg("about"), "About"),
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

@Composable
fun SettingsMenuScreen() {
    val app = LocalApp.current
    val c = colors()
    var q by remember { mutableStateOf("") }
    val found = searchSettings(q)
    val searching = q.trim().isNotEmpty()
    app.inSettings = true
    ScreenFrame("Settings", pad = 16.dp) {
        Field("Search settings", q, { q = it }, max = 40, placeholder = "Try dark, backup or kg")
        if (searching) {
            Column(Modifier.padding(top = 12.dp)) {
                if (found.isEmpty()) Text("Nothing matches. Try another word.", color = c.muted, fontSize = 17.sp, modifier = Modifier.padding(top = 12.dp))
                found.forEachIndexed { i, e ->
                    if (i > 0) Divider()
                    ListRow(e.name, e.pageName, { app.go(e.route) }, right = { Box(Modifier.rotate(180f)) { SpotIcon("back", c.muted, 18.dp) } })
                }
            }
        } else {
            Column(Modifier.padding(top = 14.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (t in SETTINGS_TOPICS) Row(
                    Modifier.fillMaxWidth().heightIn(min = 68.dp).clip(RoundedCornerShape(20.dp)).background(c.card)
                        .clickable(role = Role.Button, onClickLabel = t.title) { app.go(t.route ?: Route.Topic(t.key)) }
                        .semantics { contentDescription = t.title }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CircleIcon(t.icon, t.color)
                    Text(t.title, Modifier.weight(1f), color = c.ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Box(Modifier.rotate(180f)) { SpotIcon("back", c.muted, 20.dp) }
                }
            }
        }
    }
}

// One Settings topic: a short list, each row a picture, a title and an arrow.
@Composable
fun SettingsTopicScreen(key: String) {
    val app = LocalApp.current
    val c = colors()
    val topic = SETTINGS_TOPICS.firstOrNull { it.key == key } ?: SETTINGS_TOPICS.first()
    ScreenFrame(topic.title, floating = { SettingsHome() }) {
        Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card).padding(horizontal = 14.dp)) {
            topic.tiles.forEachIndexed { i, t ->
                if (i > 0) Divider()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClickLabel = t.title) { app.go(t.route) }.semantics { contentDescription = t.title },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CircleIcon(t.icon, topic.color, 40.dp)
                    Text(t.title, Modifier.weight(1f), color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.rotate(180f)) { SpotIcon("back", c.muted, 18.dp) }
                }
            }
        }
    }
}

// Dispatches a Settings page by name. The pages live in SettingsPages*.kt.
@Composable
fun SettingsPage(name: String, arg: Long?) {
    when (name) {
        "look" -> LookPage()
        "voice" -> VoicePage()
        "app-icon" -> AppIconPage()
        "workouts" -> WorkoutsPage()
        "workout-types" -> WorkoutTypesPage()
        "workout-type" -> WorkoutTypePage(arg)
        "locations" -> LocationsPage()
        "body-parts" -> BodyPartsPage()
        "exercise-tags" -> ExerciseTagsPage()
        "push-pull" -> PushPullPage()
        "home-widgets" -> HomeCardsPage()
        "widget" -> WidgetPage(arg)
        "records-style" -> RecordsStylePage()
        "phone" -> PhonePage()
        "backup" -> BackupPage()
        "about" -> AboutPage()
        "tend" -> TendPage()
        else -> SettingsMenuScreen()
    }
}
