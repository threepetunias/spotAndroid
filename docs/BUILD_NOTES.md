# Spot (Android): build notes

Full rebuild written 9 November 2026 to match the iPhone app through update 14 (data model v5, six setup pages, Help topics, screen-specific i buttons, empty-workout delete menu). Nothing here has been compiled with Gradle or run on a phone. See "What was and was not tested".

## Stack (Tend-proven versions)
Android Gradle Plugin 8.9.1, Kotlin 1.9.24, Compose compiler 1.5.14, Compose BOM 2024.06.00, Gradle 8.11.1, Java 17. minSdk 26, compileSdk and targetSdk 36 (Play requires API 36 or higher from 31 Aug 2026). Plain `SQLiteOpenHelper` with bound parameters. One added dependency: kotlinx-coroutines-android 1.8.1 (the app's background work).

## Documented differences from the iPhone app
- Storage: `SQLiteOpenHelper` with bound parameters instead of Room (fewer moving parts, same schema as the iPhone data model v5, same backup file).
- Widgets: five home screen widgets (Today, This week, Newest record, Fun total, Add workout / Back to workout) instead of the iPhone's eight variants. One card layout, text only.
- "Lock screen card" is an ongoing "Workout in progress" notice, with the lock-card setting wording kept as the on/off switch.
- App icon: three choices (the same pictures as the iPhone) as launcher entries. The pictures are square PNGs, so some launchers show them with a plain backing. VERIFY on a phone.
- Backup writes straight to the file the person picks (no copy is left in the cache).

## Before the first build
1. Upload the files to the `spotAndroid` repo (steps in the delivery message). The Gradle wrapper files are already in the project. Six old Kotlin files (AppState, Config, NumberField, Screens1, Screens2, SettingsScreens) and the old launcher icon files are replaced by empty or unused copies so an upload over the old project cannot clash; they can be deleted by hand later.
2. Run the Actions workflow "Build Spot APK" (the one already in the repo; it is unchanged). Green means the app compiled. The debug APK is attached to the run.
3. `AppInfo.kt`: replace DEVELOPER_NAME, SUPPORT_EMAIL and PRIVACY_URL before any store upload. While they say REPLACE_, the About buttons say "Not published yet".
4. `docs/privacy-policy.html`: fill in the placeholders and publish as a public web page (not a PDF).
5. Check the name "Spot" for trademark and Play availability.

## Signing
Release signing reads four environment variables only: SPOT_UPLOAD_KEYSTORE (path), SPOT_UPLOAD_STORE_PASSWORD, SPOT_UPLOAD_KEY_ALIAS, SPOT_UPLOAD_KEY_PASSWORD. The workflow fills them from four GitHub secrets (SPOT_UPLOAD_KEYSTORE_BASE64, SPOT_UPLOAD_STORE_PASSWORD, SPOT_UPLOAD_KEY_ALIAS, SPOT_UPLOAD_KEY_PASSWORD) and deletes the key file from the runner afterwards. Nothing secret is in the repo. Create the upload key, back it up offline, enrol in Play App Signing. Release builds a signed `.aab`, minified, not debuggable.

## Permissions (all of them)
- VIBRATE: the rest timer buzzes.
- POST_NOTIFICATIONS: the "workout in progress" notice. Asked once, right after the first logged set, never at launch. Spot works fully without it.
Nothing else: no network, storage, location, contacts, exact alarms or full-screen intents. One `<queries>` entry (Tend) so Spot can open it.

## Exported components
- Three launcher entries (icon choices) and the main screen. The main screen accepts only `spot://` links; every link is re-checked in code (`workout/<id>`, `workout/new`, `prs`, `log`) and anything else is ignored.
- Five widget receivers (exported because Android sends the update message). They ignore everything except that message.

## Security review (OWASP Mobile Top 10, 2024)
- M1/M3: no accounts, keys or tokens. M5: no network permission, no network calls.
- M4: every text field, number, route id, deep link and restore file goes through `Validate.kt` / `BackupParse.kt`; bad input is rejected, not repaired. Restore checks the whole file (type, length, range, links, repeats, size up to 10 MB, read in a bounded loop) before writing, then writes in one transaction.
- M6: no logging of personal data (no log calls at all). The notice shows the exercise name on the lock screen; turn the notice off in Settings, Phone widgets.
- M8: minimal permissions, listed above. M9: database in app-private storage; Auto Backup rules are explicit (`backup_rules.xml`, `data_extraction_rules.xml`): only the database goes to the person's own Google backup. Check they match the Data safety form. Tags and notes may be health related; encryption of the database is a possible later step.
- SQL: bound parameters or ContentValues only; the only built SQL text is fixed table names and fixed statements.
- Server Actions and RLS: not applicable (no server, no accounts). A server, sync or accounts would trigger both.
- Dependencies: only AndroidX, Compose and kotlinx-coroutines, versions pinned. CI actions are pinned to commit hashes.

## What was and was not tested
Done in the cloud workspace:
- The plain-Kotlin logic (Validate, Dates, Format, Voice, Model, Json, BackupParse, DragOrder, LogGroups, StatsLogic, PhoneSnapshot) was compiled with Kotlin 2.2.0 and `tests/CoreTest.kt` run: 184 checks passed, 0 failed. (Build uses Kotlin 1.9.24; the logic files use nothing newer.)
- Code scans for `!!`, empty catch blocks, GlobalScope, log calls, string-built SQL and unchecked first()/last(): the findings were fixed.
- Names and resource references in the new files were checked against the project by a script.

NOT done: no Gradle build, no Compose compile, no run on a phone, no TalkBack, large-text or small-screen check, no widget or notification seen. Your GitHub build is the first real compile and will likely show a few small errors to fix. The release test script (project store requirements, section 8) still has to be run on an internal test track.

## Compile fixes after the first GitHub build (9 Nov 2026, 23:10)
The first Gradle build reported three compile errors; all fixed:
- Db.kt: `rawQueryWithFactory` needs a non-null table name; now passes an empty one (the cursor is read-only).
- ExerciseScreens.kt: `Modifier.padding(horizontal =, bottom =)` is not a valid mix; now start, end and bottom.
- SettingsHomePages.kt: removed an import for `matchParentSize` (it is a Box member and needs no import).
Whether more errors follow in later compile stages is not known until the next build.

## Update 15 (22 Nov 2026)
What changed (numbers match the "Spot update 15 - change checklist"):
- 1, 2: exercise picture guessed from the name until one is chosen (`Pictures.guess`); new clearer elliptical picture.
- 3, 5: Settings "Number of sets" (1-6, 8, 10; default 4) and "Starting sets" (last time / default / one); each exercise can override ("Starting sets" on the exercise page). Logic in `SetLogic.kt` (`planSets`).
- 4: no extra set is added after you log the last one; "Add a set" / "Add a round" adds one on purpose.
- 6: "Done with this exercise" is the second button in the bottom bar.
- 7, 8, 9: "What to log" per exercise: weight, reps, time, distance, intensity, incline, other. New equipment "Cardio" (no machine). Cardio machine and Cardio start with time + distance; Other lets you choose and name what to log. Database version 6 (new columns, nothing dropped); backup version 6 (older backups still restore). Existing cardio-machine exercises that already have sets keep weight + reps.
- 10: a finished workout on Home says "Completed workout" and can still be edited.
- 11, 19: garden key under the muscle garden with a link to Settings colors; three new body parts: heart, mobility, power (12 in all).
- 12, 13, 14: PR "ghost race" is animated (a ghost per lane runs to its place; Reduce Motion / animations off = no animation); tap a lane for numbers; key below; PR scene labels no longer hidden; the exercise card says what it shows.
- 15: Back after "Settings home" leaves Settings (already how Android worked; unchanged).
- 16: rest timer keeps running when you leave the screen. `RestTimer.kt` keeps the end time; an ongoing notification counts down by itself (chronometer, no per-second work); at the end Spot buzzes and the notification becomes "Rest is over". A backup alarm (`setAndAllowWhileIdle`, no exact-alarm permission, may be a little late) covers Spot being closed. `RestTimerReceiver` is not exported. Notification permission is explained and asked once.
- 17: Back inside a workout goes menu -> exercise -> exercise list before leaving; Back on a tab that is not Home goes Home before leaving the app.
- 18: starting a lower exercise moves it up to just after the last finished one (never moves down).
- 20 (battery): see "Battery" below.

Battery (investigation, not measured on a phone): likely causes were (a) widgets and notice redrawn after every save even when nothing changed, (b) the rest timer waking the screen 5 times a second, (c) a query per exercise when opening a workout. Fixed: widgets/snapshot only rewritten when the snapshot changed or every 15 minutes (Settings "Check again" forces it); timer screen ticks once a second and only while it is showing and running; workout loads exercises and sets in two queries. Android's own battery screen (Settings, Battery, Spot) will show whether it improved after a day or two of use.

Security: new inputs (set numbers, tracks, start mode, other label) are validated against fixed lists and limits in `Store` and `BackupParse`; SQL uses bound parameters only; the new receiver is not exported and ignores anything when no rest is pending; no new permissions (POST_NOTIFICATIONS already declared; no exact-alarm or full-screen permission).

Tested here: plain-Kotlin logic including the new `SetLogic.kt`: 210 checks, 0 failed (`KOTLINC=... bash tests/run.sh`).
NOT tested: no Gradle build or Compose compile, nothing run on a phone. The first GitHub build is the real compile and may report a few small errors. VERIFY on the phone: rest-timer notification and alert (screen locked, app closed), Back button paths, ghost-race animation, picture guess, Cardio fields, move-up, widgets still update.
Known limit: the ghost race uses weight x reps, so a body part trained only with cardio shows no lane.

21 (added the same day): the workout exercise list no longer has a "Reorder" button. Each row with more than one exercise has a quiet "⋮⋮" handle on the right; press and hold it, then drag, as with the goals in Tend. TalkBack: "Move up" / "Move down" actions on the handle. Same on iPhone. The Home cards page uses the same list with its larger grip. Not compiled here (Compose); VERIFY on a phone that dragging still scrolls correctly and does not open the exercise.

Late fix (22 Nov): the lock screen card, Android notification and widgets say "Round 2 of 2" instead of "Set 2 of 2" for exercises that do not write down reps (cardio). Test added (iPhone phonetest 30 checks; Android CoreTest 212 checks).
