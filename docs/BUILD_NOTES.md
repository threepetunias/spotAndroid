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
