# App audit — 2026-10-15

## Scope

Reviewed the Android manifest and application startup, Compose navigation/screens, translation state handling, ML Kit adapter boundary, Room repository/storage flows, privacy settings, automated checks, and APK signing/release configuration.

## Findings addressed

- **Bundled phrasebook initialization had no user-visible failure path.** Startup already caught the seed exception, but the saved-phrase screen could remain on its loading message indefinitely. The app now exposes initialization state, shows a retry action after failure, and keeps the local-first behavior explicit.
- **History persistence could make a successful translation appear to have failed.** History writes now have a separate storage-error path rather than being reported as translation errors. Clearing history is serialized with pending history writes; translations started before a delete cannot silently re-add themselves afterward, and turning history off prevents a pending write.
- **Phrase saving was not guarded against repeated taps or stale completion.** The save action now has a saving state, blocks duplicate taps, and only marks the matching translation as saved when the asynchronous write completes.
- **Some Room write failures could escape a ViewModel coroutine.** Phrasebook bookmark and history-delete failures are caught and reported with a localized snackbar rather than terminating the app.
- **History copy overstated the retention limit.** Settings now describes the five-item preview of the latest entries loaded for that screen; Room retains up to 100 entries.
- **The repository had no local JVM tests for the app data repository.** Added focused repository tests for idempotent phrase seeding, saved/custom phrase behavior, history retention, and deletion. Existing Room/Compose instrumentation tests remain in the instrumentation suite.

## Release and privacy status

- The repository has no configured release keystore secrets, so CI cannot produce a production-signed release APK/AAB. The new opt-in prerelease job publishes an **installable debug-signed preview APK**, explicitly marked as a prerelease and labeled in its notes. It is not appropriate for production distribution or updates signed with a future production key.
- No API credentials are added. Translation remains on-device after an explicit Wi-Fi-only ML Kit model download. Speech recognition, Korean OCR, generated replies, and TTS are still not implemented; this audit does not present those capabilities as working.

## Verification limits

- Before these audit changes, CI run [38726663405](https://github.com/smfahadelahi/K-Bridge-AI-Android-application/actions/runs/38726663405) passed Kotlin checks, unit tests, lint, instrumentation APK compilation, and debug APK assembly.
- Post-audit CI and release results will be recorded here after the workflow completes.
- The sandbox has no Java runtime, so Gradle cannot be run locally. The latest CI host lacked usable `/dev/kvm`; instrumentation tests were compiled but not executed. No real Android device validation has been completed for ML Kit model installation, translation output, airplane-mode behavior, Room UI flows, or accessibility.
