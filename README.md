# K-Bridge AI

A privacy-first, voice-led Korean–Bangla communication assistant for Bengali speakers in South Korea.

> **Current status: foundation preview.** This repository started with only a README. The first increment creates the Android/Compose application shell, Bengali-localized navigation, domain contracts, a tested conversation reducer, and CI. Speech capture, translation, local inference, synthesis, and persistent learning features are intentionally **not** connected yet. The app does not ask for microphone permission and does not display simulated translations or model results.

## Project structure

- `app/` — Android application, Jetpack Compose / Material 3 UI, resources, and UI tests.
- `core/domain/` — Android-independent conversation state, bounded context, and engine contracts.
- `docs/architecture/PLAN.md` — architecture decisions, feasible speech/model paths, validation gates, and phased roadmap.
- `.github/workflows/android.yml` — Kotlin checks, tests, lint, debug build/artifact, and conditional signed release artifacts.

The UI currently marks unavailable features clearly. Text typed into the translator screen remains in screen state; there is no network permission, audio capture, model download, or conversation database in this build.

## Requirements

- JDK 17
- Android SDK Platform 36 or newer (the latest stable installed platform is selected by default)
- Android 12 / API 31 or newer for installation
- Android Studio or the checked-in Gradle wrapper

## Build, test, and install

```bash
./gradlew :core:domain:test :app:testDebugUnitTest
./gradlew :core:domain:ktlintCheck :app:ktlintCheck
./gradlew :app:lintDebug :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install on a connected Android device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

By default, Gradle selects the highest numbered Android platform installed in `ANDROID_HOME` / `ANDROID_SDK_ROOT`, then applies that API to both `compileSdk` and `targetSdk`. CI selects the highest platform pre-installed on the pinned runner image; if a preview platform is installed locally, pin the current stable API explicitly with `-PandroidCompileSdk=<api> -PandroidTargetSdk=<api>` or `ANDROID_COMPILE_SDK` / `ANDROID_TARGET_SDK`. If no SDK platform is installed, the development fallback is API 36. `minSdk` remains API 31.

## Release signing

A release bundle/APK is signed only when all four values are provided through the environment:

- `KBRIDGE_RELEASE_STORE_FILE`
- `KBRIDGE_RELEASE_STORE_PASSWORD`
- `KBRIDGE_RELEASE_KEY_ALIAS`
- `KBRIDGE_RELEASE_KEY_PASSWORD`

For local builds, keep the keystore outside the repository and set those variables in the shell. Then run:

```bash
./gradlew :app:assembleRelease :app:bundleRelease
```

GitHub Actions uses the corresponding repository secrets `ANDROID_RELEASE_KEYSTORE_BASE64`, `ANDROID_RELEASE_STORE_PASSWORD`, `ANDROID_RELEASE_KEY_ALIAS`, and `ANDROID_RELEASE_KEY_PASSWORD`. The keystore is decoded into the runner's temporary directory; secrets are not written to the checkout. The signed AAB and APK are uploaded only when all secrets are configured. Debug builds and test/lint checks do not require signing secrets.

## Current capability and privacy statement

This foundation build is **not** a translator or speech assistant yet:

- No microphone permission is declared; no audio is recorded.
- No Korean/Bengali speech engine, translation model, or chat model is loaded.
- No system TTS adapter is connected.
- No network service or API key is used.
- No conversation transcript, raw audio, or saved phrase is persisted.
- Offline inference is **not verified** and is not claimed.

See the architecture plan before selecting/downloading any future model. Every model/checkpoint and redistribution must pass language-quality, latency, peak-memory, device, and license review.
