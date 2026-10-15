# Architecture and implementation plan

**Updated:** 2026-10-15
**Repository baseline:** Kotlin/Compose foundation with a pure Kotlin conversation reducer, UI shell, and Android CI. This plan tracks the working MVP increment; it is not a claim that the full voice-and-AI product is complete.

## 1. Current increment

### Implemented in source

- Android application using Kotlin, Jetpack Compose, Material 3, `minSdk = 31`; Gradle selects the highest stable installed SDK at build time.
- English and Bengali (`values-bn`) UI strings; conversation, translation, practice, phrasebook, and settings destinations.
- `:core:domain` conversation phases/events/reducer, transcript de-duplication, queue/context contracts, typed engine ports, bilingual phrase models, offline starter data, and quiz-question selection.
- Room database and repository for the bundled phrasebook, user-saved phrase pairs, and translation history. History is opt-in (off by default), bounded to the latest 100 database entries, and can be deleted independently from saved phrases.
- A 27-entry Korean–Bangla starter phrase set across everyday, shopping, transport, health, workplace, and housing contexts. The phrases are not professionally certified or native-editor-reviewed; urgent or high-impact meanings need confirmation.
- Typed Korean↔Bangla translation through Google ML Kit. The UI checks local model availability; it does not translate until the models are present. Download is explicitly confirmed by the user and requires Wi-Fi. This app's translation adapter passes text to the on-device ML Kit translator and has no translation-server or generative-AI request.
- Saved phrases can be reviewed in randomized Korean→Bangla and Bangla→Korean self-check flashcards. Guesses and learning progress are not recorded.
- Room and Compose instrumentation test sources plus pure domain unit tests.

### Not implemented / not verified

- Foreground speech recognition and microphone permission flow.
- Korean camera OCR and temporary-image lifecycle.
- AI reply generation, the secure remote-backend implementation, endpoint configuration, and explicit per-session consent for sending conversation text.
- Korean TTS playback.
- Full multi-turn conversation orchestration that links speech, translation, replies, and playback.
- Native-language review of phrasebook content, accessibility audits with assistive technology, and device performance testing.
- End-to-end airplane-mode translation testing on supported Android 12+ devices. The local inference path is implemented, but offline behavior must not be represented as device-verified until tested.

## 2. Current architecture and boundaries

```text
:app
  ui/                  Compose screens, lifecycle state, KBridgeViewModel
  data/local/          Room database, entities, DAOs
  data/repository/     Room-backed PhraseRepository
  platform/translation ML Kit adapter; model downloads require explicit UI consent

:core:domain
  conversation/        state, events, reducer, engine contracts
  phrasebook/          stable scenario/model IDs, starter phrases, quiz selection
```

The Room adapter is currently in `:app` to keep the first persistence increment small. Before feature growth, separate Android persistence and platform engines into focused modules while preserving the Android-independent domain. Keep Android/ML Kit/Room imports out of `:core:domain`.

The existing domain engine ports include `SpeechRecognitionEngine`, `TranslationEngine`, `ConversationAgent`, and `SpeechSynthesisEngine`. The current translation implementation is app/platform-specific; the other ports do not imply a working speech, AI, or TTS feature.

### Translation model lifecycle

- `INTERNET` is declared because ML Kit model files must be downloaded; Android does not show a runtime dialog for that permission.
- The app does not initiate a download at startup. Model availability is queried locally when the translation screen opens.
- If models are missing, the user sees what is downloaded, that Wi-Fi is required, and that typed content is not sent. Only a separate confirmation button starts ML Kit's `downloadModelIfNeeded`.
- After download, input and translation use the on-device ML Kit translator. Network loss, missing Google Play services, or model failure must produce an error, not fabricated output.
- Model download is not the same as offline verification. Test a clean install, download, disabled network, both directions, and real Korean/Bengali text on physical Android 12+ devices.
- ML Kit's supported-language reference lists Korean and Bengali. The exact runtime model availability and translation quality still need device testing: [ML Kit translation language support](https://developers.google.com/ml-kit/language/translation/translation-language-support), [Android guide](https://developers.google.com/ml-kit/language/translation/android).

### Room storage policy

- Bundled phrase IDs are stable and reinserted with `INSERT IGNORE`; Room's database is private to the app.
- Saved phrase pairs are created only when a user taps a bookmark or explicitly saves a translation. Removing a bundled phrase removes its bookmark; removing a user-created phrase deletes that row.
- Translation history defaults off and stores only successful typed source/target text when the setting is enabled. It is capped at 100 entries. Settings offers a confirmation before deleting all translation history; that action does not erase saved phrases.
- Raw audio is not captured or persisted. Android app backup is disabled.
- This policy should be revisited before adding speech, camera, or remote services. In particular, do not silently put transcripts into history, upload conversation context, or persist OCR images.

### Conversation lifecycle contract

The reducer models idle, permission request, listening, partial processing, finalization, translation, reply generation, speech playback, waiting, pause, stop, and recoverable error. Partial transcripts remain separate from committed turns. Final transcript IDs are de-duplicated; rapid turns queue; results for a superseded/stopped turn are ignored. The reducer has no Android/audio dependencies and can be tested with deterministic events.

When speech is added, effects must be cancellable and owned by a foreground session. Request microphone permission only after the user starts. The UI may say “listening” only after an engine confirms capture has opened. Stop, pause, lifecycle interruption, and denial must stop actual capture—not merely update a screen flag. There must be no permanent background microphone service for ordinary use.

## 3. Integration choices and constraints

### Korean and Bengali speech recognition — not started

- Android `SpeechRecognizer` can be capability-checked; an on-device recognizer is available only where the OS/service provides it. Locale support, partial results, offline operation, and audio routing vary by device. `EXTRA_PREFER_OFFLINE` is not proof of local-only inference. Never silently fall back to a network recognizer.
- A multilingual `whisper.cpp` checkpoint is another evaluation candidate, not a selected or bundled runtime. Measure Korean/Bengali accuracy, latency, battery/thermal impact, memory, model size, streaming behavior, and exact checkpoint/data licenses.
- Gate: manually reviewed Korean/Bangla samples covering accents, noise, pauses, negation, dates, amounts, and workplace directions; report measured WER/CER and latency only.

### Korean OCR — not started

- Candidate: ML Kit Text Recognition v2 Korean recognizer, processing a user-selected/captured image on-device. Confirm bundled vs dynamically delivered model behavior and license/dependency size before integration.
- Request camera access only when needed (or use a system camera/photo picker contract that does not require the app to hold a permanent camera session). Delete temporary images after OCR unless the user explicitly saves them. An OCR transcript is untrusted input; show it for correction before translating.

### AI replies and secure backend — not started

- The domain `ConversationAgent` is only an interface. No provider, endpoint, or credential is configured in this APK.
- Before remote replies: deploy a trusted HTTPS backend that keeps provider credentials server-side, validates request/response size and schema, rate-limits per account/device, sets usage/spending caps, and logs no raw conversation content by default.
- On Android, remote generation must be off by default, disclose exactly which current text and bounded context will be sent, and require explicit consent before each remote send (or an equally clear user-controlled session grant). Do not send raw audio. Provide an honest “backend unavailable / not configured” state and never substitute a fake result.
- The first implementation can offer explicit curated phrasebook responses while the backend is unavailable, but it must label these as phrasebook suggestions, not generated AI.

### Korean text-to-speech — not started

Android `TextToSpeech` is a candidate adapter. Check the installed Korean voice and language/network requirements at runtime. Offer installation guidance if unavailable; do not claim a local voice or offline speech unless verified. Playback controls and interruption belong to the speech adapter.

### Starter content and translation accuracy

The bundled pairs are pragmatic MVP examples rather than certified translations. Have Korean and Bangla native-language reviewers edit the exact wording, honorific level, scenario appropriateness, and sensitive health/workplace terms before production use. The on-device translation model can also miss context, tone, negation, or named entities; expose that limitation and do not show a confidence percentage unless the underlying engine supplies a calibrated score.

## 4. Roadmap / acceptance gates

1. **Android foundation — complete.** Compose navigation, Bengali resources, truthful unavailable states, domain contracts/reducer, tests, and CI.
2. **Local learning + typed translation — implemented in source; CI/device verification pending.** Room, offline phrasebook, save/delete, self-check quizzes, opt-in history/deletion, and consent-gated ML Kit model download/translation. Verify build, unit tests, Room instrumentation, translation both directions, and offline behavior.
3. **Foreground speech input — not started.** Runtime permission, on-device-only capability reporting, partial/final separation, stop/cancel/lifecycle handling, language-resource errors, no background mic.
4. **Korean camera OCR — not started.** Camera/photo contract, on-device Korean OCR, correction step, temporary-file deletion, permission and unsupported-device states.
5. **AI reply service — not started.** Secure backend contract and deployment sample, HTTPS validation, server-side secrets, request/response validation, per-user rate limits and spending caps, explicit user consent, timeout/poor-connectivity handling, and no fake replies.
6. **TTS and end-to-end conversation — not started.** Korean voice availability, playback, interruption, bounded context, turn queueing, correction/clarification, history policy, and complete multi-turn stop/restart behavior.
7. **Content, privacy, and accessibility review — not started.** Native review of phrasebook, accessibility testing, permission/privacy copy review, migrations/deletion verification, and error-state tests.
8. **Device quality and release — not started.** Install on clean Android 12+ devices, measure memory/latency, verify permission denial and poor connectivity, test offline after model download, inspect release signing, and publish a debug/release artifact only with accurate notes.

The final voice-first acceptance journey remains: choose a situation; speak or type; see Korean recognition where supported; receive Korean↔Bangla translation; receive exactly three useful Korean reply options with Bangla meanings only from a real configured backend; play a Korean reply; save phrases; practice saved phrases; delete local history. Critical offline claims require a network-disabled end-to-end test. The current increment meets only phrasebook, typed-translation, local-saving/history, and basic self-check portions of that journey.
