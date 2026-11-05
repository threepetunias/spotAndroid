# Spot (Android): build notes, slice 1

Written 5 November 2026. Nothing here has been compiled or run on a phone. See "What was and was not tested".

Same features, wording and backup file format as the iPhone app (see the iPhone repo's BUILD_NOTES). Not in slice 1: rest timer, reminders, Insights, widgets, Tend sharing.

## Stack (versions written from memory, unverified)
Kotlin 2.2.0, Android Gradle Plugin 8.11.1, Compose BOM 2025.07.00, navigation-compose 2.9.2. compileSdk/targetSdk 36 (Play requires API 36 or higher from 31 Aug 2026), minSdk 26. Plain SQLiteOpenHelper with bound parameters. If the first build complains about a version, change it to the latest stable and rebuild.

## Before the first build
1. Gradle wrapper: this zip has `gradle-wrapper.properties` but not `gradle-wrapper.jar`, `gradlew` or `gradlew.bat`. Copy those three files (and keep the properties file) from the Tend repo into the same places (`gradlew`, `gradlew.bat`, `gradle/wrapper/`).
2. CI workflow: copy the build workflow from the Tend repo into `.github/workflows/` and change the app name. Check that its actions are pinned to full commit hashes.
3. `Config.kt`: replace DEVELOPER_NAME, SUPPORT_EMAIL, PRIVACY_URL. `TEND_PACKAGE` (`com.tendapp.personal`) is a guess; check Tend's applicationId. The same id is in the manifest `<queries>` block.
4. `docs/privacy-policy.html`: fill in the placeholders and publish as a public web page (not a PDF).
5. Launcher icon is a first draft vector (weight stack). Review before release.
6. Check the name "Spot" for trademark and Play availability.

## Signing
Release signing reads four environment variables only: SPOT_UPLOAD_KEYSTORE (path), SPOT_UPLOAD_STORE_PASSWORD, SPOT_UPLOAD_KEY_ALIAS, SPOT_UPLOAD_KEY_PASSWORD. Store them as GitHub secrets. Nothing is in the repo. Create the upload key, back it up offline, enrol in Play App Signing. Build the `.aab` (bundleRelease), not a debug APK. Release is minified.

## Security review (OWASP Mobile Top 10, 2024)
- M1/M3: no accounts, keys or tokens. M5: no network permission, no network calls.
- M4: every text field, number, route id and restore file goes through Validate.kt; bad input is rejected, not repaired. Restore checks the whole file (type, length, range, links, repeats, size) before writing, then writes in one transaction.
- M6: no logging of personal data. M8: no permissions declared; one `<queries>` entry for Tend only; MainActivity is the only exported component.
- M9: database is in app-private storage. Tags and notes may be health related; consider encrypting later. Backups go straight to the file the person picks (no cache copy). Auto Backup rules are explicit in `backup_rules.xml` and `data_extraction_rules.xml`; check they match what the Data safety form says.
- SQL: bound parameters or ContentValues only; the only built SQL text is fixed strings.
- Server Actions and RLS: not applicable (no server, no accounts). A server, sync or accounts would trigger both.

## What was and was not tested
Done in the cloud workspace:
- Brackets balanced in all 16 Kotlin files; no duplicate top-level names; every capitalized call resolves to something defined or imported; every manifest and resource reference exists; all 19 navigation routes have screens.
- All 21 SQL statements and the schema were prepared against a real SQLite engine (syntax only).
- Strings (voices, settings search list, about text, medical statement) compared with the iPhone app.

NOT done: no compile (no Android toolchain here), no run, no SQL data test against the Kotlin code, no screen seen, no TalkBack, large-text or small-screen check. Your GitHub build is the first real compile and will likely show a few small errors to fix. The release test script (project store requirements, section 6) still has to be run on an internal test track.
