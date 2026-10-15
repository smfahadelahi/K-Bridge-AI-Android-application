# K-Bridge AI

A local-first Korean–Bangla communication assistant for Bengali speakers in South Korea. The repository is being built in small, verifiable increments; the current app is an installable MVP slice, **not yet the complete voice-and-AI workflow**.

## What works in this increment

- **Offline phrasebook:** 27 bundled Korean–Bangla starter phrases across everyday, shopping, transport, health, workplace, and housing situations. The phrasebook is stored in Room on first launch and is available without downloading a model or opening a network connection.
- **Phrase saving and review:** bookmark/unbookmark bundled phrases, save a successful translation as a phrase pair, browse by situation, and practice saved phrases in randomized Korean→Bangla / Bangla→Korean self-check flashcards. Quiz answers and learning progress are not scored or stored.
- **Typed on-device translation:** translate Korean↔Bangla with Google ML Kit after the user explicitly approves a Wi-Fi-only model download. Translation input is passed to the on-device ML Kit translator and this app does not send it to a translation server. Model downloads require Internet access and Google Play services.
- **Private local history:** translation-history saving is off by default. If enabled in Settings, successful typed translations are stored in the Room database on the device; the user can clear all history. Saved phrases are independent of translation-history deletion.
- **Bengali UI strings**, Material 3 Compose screens, domain contracts/reducer, Room data layer, and GitHub Actions build checks.

The bundled phrase pairs are starter content, **not professionally certified or native-editor-reviewed**. Machine translation and phrasebook wording can miss tone or context. Verify important medical, legal, financial, employment, or safety information with a qualified interpreter or trusted professional.

## Not implemented yet (do not assume these work)

- Foreground speech recognition or microphone capture. The app does not request microphone permission and does not save raw audio.
- Camera capture / Korean OCR.
- Context-aware AI replies or a hosted backend. No conversation text is sent to a generative AI service, and no API key is in the APK.
- Korean text-to-speech playback.
- Full multi-turn conversation orchestration, production-quality native-reviewed phrase content, and accessibility/device performance validation.

The conversation screen labels these capabilities unavailable instead of displaying fabricated transcripts or replies. The app declares `INTERNET` only for the user-approved ML Kit model download; model availability checks and translation are not intended to upload typed content. Offline translation is designed to run after download, but **the complete offline path has not yet been verified across real devices**. Do not rely on it for critical communication until device and airplane-mode tests are completed.

## Project structure

- `app/` — Android application, Compose UI, ML Kit translation adapter, Room database/repository, JVM repository tests, and instrumentation tests.
- `core/domain/` — Android-independent conversation models/reducer, engine contracts, phrasebook content, quiz selection, and unit tests.
- `docs/architecture/PLAN.md` — current implementation status, architecture, privacy boundaries, validation gates, and remaining roadmap.
- `docs/quality/APP_AUDIT.md` — latest app audit findings and verified limitations.
- `.github/workflows/android.yml` — Kotlin checks, unit tests, lint, debug APK, instrumentation APK compilation, optional emulator tests, signed-release artifacts when secrets are configured, and an opt-in debug APK prerelease.

## Requirements

- JDK 17
- Android SDK Platform 36 or newer (Gradle selects the highest stable platform installed at build time; CI selects the highest platform pre-installed on the runner)
- Android 12 / API 31 or newer
- Android Studio or the checked-in Gradle wrapper

## Build, test, and install

```bash
./gradlew :core:domain:test :app:testDebugUnitTest
./gradlew :core:domain:ktlintCheck :app:ktlintCheck
./gradlew :app:lintDebug :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install a locally built copy on a connected Android device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions uploads a debug APK named `k-bridge-ai-debug-apk` after the Gradle checks/build. Download it from the successful run’s **Artifacts** section. It is debug-signed, not a release-signed APK. The audit build is [run 38734644468](https://github.com/smfahadelahi/K-Bridge-AI-Android-application/actions/runs/38734644468); its APK artifact is available there.

CI always compiles the instrumentation-test APK. It runs Android UI/storage instrumentation tests only when the runner exposes readable/writable `/dev/kvm`; without KVM it warns and skips emulator execution. Run 38734644468 passed Kotlin formatting, unit tests (including the new repository tests), lint, debug build, and instrumentation-test APK compilation. Android UI/Room instrumentation tests were skipped because KVM was unavailable. A successful workflow on a host without KVM is not evidence that instrumentation tests ran.

By default, Gradle selects the highest numbered Android platform installed in `ANDROID_HOME` / `ANDROID_SDK_ROOT`, then uses that API for `compileSdk` and `targetSdk`. Pin the stable API explicitly with `-PandroidCompileSdk=<api> -PandroidTargetSdk=<api>` or `ANDROID_COMPILE_SDK` / `ANDROID_TARGET_SDK` if needed. `minSdk` remains API 31.

## Translation model and privacy

1. Open **Translate** and enter Korean or Bangla text.
2. If the language models are missing, review the in-app notice. Nothing downloads until you tap **Download** in that confirmation.
3. The app requests the Korean↔Bengali ML Kit models over Wi-Fi. Download size/storage use can vary and Google Play services must be available.
4. After the model is installed, the translator processes entered text on-device. The current feature sends no typed text or audio to a backend.
5. Machine translation can be wrong. Confirm high-impact content with a qualified person.

No model is automatically downloaded at startup. Translation history is separately opt-in and defaults to off. The local Room database contains the phrasebook, saved phrases, and—only after opt-in—translation history. Settings provides a delete-history action. Android backup is disabled in the app manifest.

## Release signing

A release bundle/APK is signed only when all four values are provided through the environment:

- `KBRIDGE_RELEASE_STORE_FILE`
- `KBRIDGE_RELEASE_STORE_PASSWORD`
- `KBRIDGE_RELEASE_KEY_ALIAS`
- `KBRIDGE_RELEASE_KEY_PASSWORD`

Keep the keystore outside the repository. Then run:

```bash
./gradlew :app:assembleRelease :app:bundleRelease
```

GitHub Actions uses repository secrets `ANDROID_RELEASE_KEYSTORE_BASE64`, `ANDROID_RELEASE_STORE_PASSWORD`, `ANDROID_RELEASE_KEY_ALIAS`, and `ANDROID_RELEASE_KEY_PASSWORD`. Secrets are not written to the checkout. Debug builds and test/lint checks do not require signing secrets.

### Publishing an installable preview

From **Actions → Android CI → Run workflow**, select the branch to publish and set `publish_debug_preview` to `true`; an optional `release_tag` can be provided. The workflow publishes the verified debug APK as a GitHub prerelease and checks its APK signature/package before upload. This artifact uses the Android debug key, is not production-signed, and cannot be upgraded in place to an APK signed with a future production key. Configure the release-signing secrets above for a production-signed release build.

The current installable preview is [K-Bridge AI v0.1.0-preview.34](https://github.com/smfahadelahi/K-Bridge-AI-Android-application/releases/tag/v0.1.0-preview.34); download the [APK](https://github.com/smfahadelahi/K-Bridge-AI-Android-application/releases/download/v0.1.0-preview.34/app-debug.apk) (about 125 MB).

## Current validation limits

The Android instrumentation suite includes Room phrase-save/history-deletion checks and Compose UI checks. Run 38734644468 compiled the instrumentation APK but did not execute those tests because the CI host lacks usable `/dev/kvm`. Verify the increment on an Android 12+ device/emulator, including explicit model download, Korean↔Bangla output, loss of Wi-Fi during download, translation with networking disabled after download, and history deletion before treating offline behavior as validated.
