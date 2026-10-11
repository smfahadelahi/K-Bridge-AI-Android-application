package com.kbridge.ai.core.domain.conversation

sealed interface ConversationEvent {
    data object StartRequested : ConversationEvent

    data object MicrophoneCaptureStarted : ConversationEvent

    data object MicrophonePermissionDenied : ConversationEvent

    data class PartialTranscriptReceived(val text: String) : ConversationEvent

    data class FinalTranscriptReceived(
        val transcriptId: String,
        val direction: TranslationDirection,
        val text: String,
        val timestampEpochMillis: Long,
    ) : ConversationEvent

    data class TranslationStarted(val turnId: String) : ConversationEvent

    data class TranslationCompleted(val turnId: String, val translatedText: String) : ConversationEvent

    data class ReplyGenerationStarted(val turnId: String) : ConversationEvent

    data class ReplySuggestionsReady(
        val turnId: String,
        val suggestions: List<ReplySuggestion>,
    ) : ConversationEvent

    data class ReplyGenerationSkipped(val turnId: String) : ConversationEvent

    data class SpeakRequested(val text: String) : ConversationEvent

    data object PlaybackCompleted : ConversationEvent

    data object PlaybackInterrupted : ConversationEvent

    data object PauseRequested : ConversationEvent

    data object ResumeRequested : ConversationEvent

    data object StopRequested : ConversationEvent

    data object ClearSessionRequested : ConversationEvent

    data class CancelTurnRequested(val turnId: String) : ConversationEvent

    data class RecoverableFailure(val message: String) : ConversationEvent

    data object RetryRequested : ConversationEvent
}
