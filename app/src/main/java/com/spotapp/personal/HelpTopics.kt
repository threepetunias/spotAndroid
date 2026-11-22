package com.spotapp.personal

// The words of the Help pages. One topic per page. Same wording as the iPhone app (src/helpTopics.ts),
// except the phone topic, which is worded for Android widgets and the workout notice.

class HelpTopic(val key: String, val title: String, val body: String)
class HelpGroup(val title: String, val keys: List<String>)

val HELP_TOPICS: List<HelpTopic> = listOf(
    HelpTopic("start", "Starting a workout", "Tap the + button, pick a day on the calendar (any past, present or future day), choose where and what kind of workout, and choose exercises. Spot names the workout for you from the day and place. You can copy the exercises from a past workout. Spot fills in the sets from the last time you did each exercise."),
    HelpTopic("doing", "Doing a workout", "A workout opens as a list of exercises. Tap any one, in any order. Tap Later on an exercise if a machine is busy, and to move an exercise press and hold the dots (⋮⋮) at the right of its row, then drag it up or down. When you log the first set of an exercise that is lower in the list, it moves up to just after the last exercise you finished. Tap Finish workout when you are done. If you beat your heaviest weight for an exercise, Spot celebrates a new record."),
    HelpTopic("logging", "Logging sets", "Each set is a row. Tap the weight to type it. Use the minus and plus buttons for reps. Tap Log set when you finish one. Spot does not add a set by itself: tap Add set for another. Tap Done with this exercise, the second button at the bottom, when you are finished. A new exercise starts with the number of sets from Settings, Units and buttons (or last time's), and you can set this for any one exercise. Cardio and other exercises can write down time, distance, intensity, incline or your own things instead of weight and reps; they count rounds instead of sets. The minus and plus buttons move a number by its own step; for weight it is the amount in Settings, Units and buttons. Numbers you change stay for the later rows of that exercise. Tap any row to change it."),
    HelpTopic("left-off", "Where I left off", "Under the sets, choose Last, Best or Other. Last and Best look at the place you are using today; Other shows the latest time at a different place. Tap Use these sets to copy them in, or Browse all past sessions for any session, place or tag."),
    HelpTopic("exercises", "Exercises and cues", "An exercise has a picture (Spot suggests one from the name until you choose), its equipment, what to log (for cardio machine, cardio and other), how many sets it starts with, the body parts it works, a link to a page or video about it (tap Open link to go there), your own tags, push or pull (turn this on or off per equipment in Settings, Push and pull), and the places you do it. Each place keeps its own history. Cues are short reminders such as \"press feet into floor\"; they show on the exercise screen. Edit them from the Exercise tile or Settings, Exercises."),
    HelpTopic("tags", "Tags", "Tags are labels you make up to describe a workout or an exercise, such as Heavy day, Travel or Shoulders. Put them in groups, and give a tag sub-tags beneath it. For example: Equipment, then Dumbbells, then Hex. Sub-tags can go up to three levels deep. Manage them in Settings, Tags. Tag a whole workout or a single exercise. A group can be a notes group, which is just a label, or a health group, which also lets you give each tag a strength from 1 to 5 when you use it. Tags help you look back later. They never leave your phone."),
    HelpTopic("prs", "Records", "The PRs tab shows your heaviest weight for each exercise, with reps, date and place. Tap See all records as a list to search and filter them by body part, place, equipment, push or pull and your tags. Settings, PR Records Style chooses how the page looks. With a scene, the card at the bottom shows your heaviest record until you tap a star, peak or building. In Ghost race, tap a lane to see the numbers."),
    HelpTopic("log", "The Log", "The Log tab lists your workouts by month. The newest month is open and older months are closed; tap a month to open or close it. Searching or filtering opens every month that has a match. Search it, filter by tag, place, type or dates, and press and hold a workout to delete it."),
    HelpTopic("home", "Home", "Home shows cards: today's workout (it says Completed workout once you finish, and you can still open and edit it), a muscle garden (with a key; tap it to change the colors), push and pull balance, this week and a fun total of the weight you lifted. Settings, Home and PRs, Home cards turns each on or off, moves them and gives each one three looks."),
    HelpTopic("insights", "Insights", "The Insights button, at the top beside Help and Settings, shows trends over the weeks: days trained, weight lifted, sets by body part and push against pull. Each page ends with a Next card for the following page. Numbers appear only once there is enough to show."),
    HelpTopic("timer", "Rest timer", "The Timer tab counts down a rest of 30 seconds, 1, 1.5 or 2 minutes, or a number you type. It keeps running when you go to another screen or close Spot, and shows in a notification with the time left. When time is up Spot buzzes, and the notification tells you rest is over (allow notifications when asked)."),
    HelpTopic("phone", "Phone widgets and workout notice", "Add a Spot widget from the Android home screen: press and hold an empty spot, tap Widgets, and find Spot. Widgets show today's workout, this week, weeks in a row, your newest record, a fun total, and a button back to your workout. After you log your first set, a notice shows where you are; tap it to return. Settings, Home and PRs, Phone widgets turns the notice off."),
    HelpTopic("places", "Places and body parts", "Give each place a picture and a color in Settings, Places. Body parts (arms, shoulders, back, chest, core, legs, glutes, feet, lungs, heart, mobility and power) have colors in Settings, Body parts. Your own exercise labels are in Settings, Exercise tags."),
    HelpTopic("icon", "App icon", "Settings, Look and feel, App icon lets you choose the picture Spot shows on your home screen."),
    HelpTopic("backup", "Back up", "Settings, Back up saves a file you keep. Restore replaces what is on this phone with the file."),
    HelpTopic("welcome", "Replay the welcome", "The welcome pages show how Spot works, from setting up your exercises to logging sets. You can walk through them again any time."),
    HelpTopic("medical", "Not a medical device", "Spot is not a medical device and does not diagnose, treat, cure, or prevent any medical condition. Talk to a doctor or other healthcare professional about pain, injury or health concerns."),
)

val HELP_GROUPS: List<HelpGroup> = listOf(
    HelpGroup("Working out", listOf("start", "doing", "logging", "left-off", "timer")),
    HelpGroup("Exercises and tags", listOf("exercises", "tags", "places")),
    HelpGroup("Looking back", listOf("home", "log", "prs", "insights")),
    HelpGroup("Your phone", listOf("phone", "icon", "backup")),
    HelpGroup("About Spot", listOf("welcome", "medical")),
)

fun findHelp(key: String?): HelpTopic? = HELP_TOPICS.firstOrNull { it.key == key }
