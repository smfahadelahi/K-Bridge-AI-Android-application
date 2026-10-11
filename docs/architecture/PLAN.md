# Architecture and implementation plan

**Plan date:** 2026-10-11

**Repository baseline:** initial commit contained only `README.md`; no Android project, model assets, tests, or CI were present.

## 1. First increment delivered

- Kotlin Android app with Jetpack Compose and Material 3, `minSdk = 31`; Gradle selects the highest numbered installed platform, with API 36 as a fallback. CI selects the highest platform pre-installed on its pinned runner image.
- Bengali (`values-bn`) strings and English default strings; five reachable sections: conversation, text entry, practice roadmap, saved-phrase empty state, and readiness/privacy.
- Pure Kotlin `:core:domain` module containing the conversation phases/events/reducer, transcript de-duplication, queued turns, bounded context, and injectable engine contracts.
- Domain unit tests and Compose instrumentation tests for truthful unavailable states.
- KtLint, Android lint, unit/instrumentation tests, debug APK CI artifact, and optional secret-backed release signing.
- No `RECORD_AUDIO` or `INTERNET` permission; no model, cloud client, audio capture, database, or fake inference implementation.

The app's visible disabled states are intentional. This phase establishes an honest, buildable base; it does not satisfy the voice-workflow release criteria.

## 2. Target architecture

Keep UI, policy, and platform/model integrations separated. The current app and domain modules are the first slice; later integrations should be isolated as Gradle modules rather than added to Compose screens.

```text
:app                         Compose screens, navigation, lifecycle wiring
:feature:conversation        session orchestration and presentation state
:core:domain                 conversation models, reducer, engine ports (present)
:core:speech                 AudioRecord/VAD, recognizer adapters, TTS adapter
:core:translation            Korean ↔ Bangla translation implementations
:core:ai                     bounded-context local reply agent and model runtime
:core:storage                local history, saved phrases, deletion, migrations
:core:modelmanager           versioned model manifests, downloads, checksums, licenses
```

The domain ports currently include `SpeechRecognitionEngine`, `TranslationEngine`, `ConversationAgent`, and `SpeechSynthesisEngine`. Platform implementations should report typed availability (not configured, model missing, download needed, unsupported language, error, ready) and may report `offlineVerified = true` only after an end-to-end airplane-mode test.

### Conversation lifecycle contract

The reducer models idle, permission request, listening, partial processing, finalization, translation, reply generation, speech playback, waiting, pause, stop, and recoverable error. Partial transcripts remain separate from committed turns. Final transcript IDs are de-duplicated; rapid turns queue; results for a superseded/stopped turn are ignored; stop closes the capture state and cancels pending work at the orchestration layer. The reducer has no Android/audio dependencies and is testable with deterministic events.

Effects must remain cancellable and owned by a foreground session. Request microphone permission only when the user starts. The UI may say “listening” only after the audio engine confirms capture has opened. Stop/pause/interruption must stop the actual audio/TTS engine, not merely update a screen flag.

## 3. Speech and local-model feasibility

No default model is selected or shipped in this increment. There are no model files in the checkout, and this environment has no Android SDK/device on which to verify quality, memory, or offline behavior. Selecting model weights before that validation would be an unsupported product claim.

### Korean and Bengali speech recognition

- **Platform option:** Android `SpeechRecognizer` can be capability-checked and an on-device service can be used when the OS/service provides one. Locale support, incremental results, offline operation, and whether audio leaves the device vary by installed service. A preference for offline processing is not proof of offline operation. Do not silently route to a network recognizer.
- **Local candidate:** `whisper.cpp` with a multilingual Whisper checkpoint is a practical candidate for Korean and Bengali on-device evaluation. It is not assumed to be low-latency streaming; chunking/VAD, partial-result policy, battery/thermal behavior, model size, exact checkpoint license, and Bengali/Korean accuracy must be measured. The runtime license does not by itself establish permission to redistribute every checkpoint or dataset.
- **Decision gate:** compare local recognition with available Android on-device services on manually reviewed Korean/Bengali conversational audio, including accents, noise, pauses, amounts, dates, workplace directions, and negation. Record WER/CER and completion-to-final-text latency; do not publish fabricated scores.

### Translation

- Use a dedicated local translation model (not a static phrase table and not a chat prompt masquerading as translation). Evaluate bidirectional Korean↔Bengali, especially negation, numbers, honorifics, dates, amounts, and ambiguity.
- Multilingual encoder-decoder checkpoints (for example NLLB/M2M-family candidates) are evaluation candidates, not a distribution decision. Audit the exact model, tokenizer, training-data terms, and quantized artifact licenses. Some research checkpoints have non-commercial restrictions; do not bundle or redistribute until an exact-license review is complete.
- No translation implementation currently returns canned output. Missing model must be a blocking, explicit result rather than a pass-through or invented translation.

### Context-aware reply agent

- A small quantized multilingual local LLM (roughly 1–3B parameter evaluation range) can be tested for Korean generation and Bengali explanation on 8 GB Android devices using an Android-compatible runtime such as `llama.cpp`/GGUF. This is a candidate shape, not a selected model.
- Measure end-to-end peak memory with speech/translation resources included, first-token latency, tokens/second, thermal throttling, and response quality. Test grounding, bounded context, correction, incomplete utterances, polite/simple variants, and a “clarify rather than guess” policy.
- The agent must receive only a bounded recent context window, and users need a clear-session action. No third-party API or remote inference fallback is part of the core architecture.

### Korean speech output

Android `TextToSpeech` is the smallest initial adapter. Check the installed Korean voice/language and voice network requirement at runtime. Offer installation guidance when absent; do not claim a local voice is present or offline until verified. Playback controls belong to the speech adapter; interruption stops queued speech immediately.

### Model distribution and license policy

No runtime model hosting location is configured. A later controlled model catalog must pin model revisions and cryptographic hashes, show download size/storage needs, verify the complete artifact before loading, support cancellation/removal, and retain model/license notices. GitHub Actions is build/test automation only, never a model inference service. No API key is stored in source, resources, or the APK.

## 4. Privacy and data flow today

This build has no `RECORD_AUDIO` or `INTERNET` permission, no audio capture, no network client, and no database. Manual translator text is held in the current UI state and produces no output. No conversation is saved. These statements describe the current code only; future storage/download features require a fresh data-flow and deletion review.

## 5. Roadmap and completion gates

1. **Inspect and plan — complete.** Confirm the empty baseline and document architecture/stack gates.
2. **Android foundation and UI — first increment complete.** Compose navigation, Bengali resources, truthful engine readiness states, domain seams, and tests. Voice/model workflows remain unimplemented.
3. **Foreground audio capture — not started.** Runtime permission, lifecycle-safe `AudioRecord`, VAD/endpoint controls, start/stop/pause/interruption tests; no persistent background microphone.
4. **Korean/Bengali recognition — not started.** Candidate adapters, partial/final separation, noise/pause tests, and device/language availability reporting.
5. **Bilingual translation and local reply agent — not started.** Select only after license and quality/latency/memory gates; add correction/clarification and bounded context.
6. **Korean TTS and continuous session orchestration — not started.** Real voice availability, playback controls/speed, interruption, queueing, completion-to-translation and first-audio latency instrumentation.
7. **Scenarios and learning — not started.** Reviewed scenario content, saved entries, local persistence, spaced repetition, and progress tests.
8. **Privacy, model manager, and recovery — not started.** Verified downloads, model corruption/low-storage/OOM recovery, local deletion, and airplane-mode tests.
9. **Quality/performance validation — not started.** Run tests/lint on CI and benchmark on clean Android 12+ devices with at least 8 GB RAM; publish measured latency and memory only.
10. **Release readiness — not started.** Produce and install APK/AAB, verify signing, complete offline and permissions checks, publish licenses/install guide.

**Release is not accepted** until actual Korean speech can be transcribed and translated into Bengali, Bengali speech can generate a suitable Korean reply, Korean audio can play, a multi-turn foreground session survives interruption/stop/restart, and the selected end-to-end pipeline is tested without network access. This first increment does not meet those criteria.
