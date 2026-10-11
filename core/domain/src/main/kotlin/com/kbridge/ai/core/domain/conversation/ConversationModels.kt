package com.kbridge.ai.core.domain.conversation

enum class ConversationLanguage(val tag: String) {
    KOREAN("ko-KR"),
    BANGLA("bn-BD"),
}

enum class TranslationDirection(
    val sourceLanguage: ConversationLanguage,
    val targetLanguage: ConversationLanguage,
) {
    KOREAN_TO_BANGLA(ConversationLanguage.KOREAN, ConversationLanguage.BANGLA),
    BANGLA_TO_KOREAN(ConversationLanguage.BANGLA, ConversationLanguage.KOREAN),
}

enum class ReplyTone {
    SIMPLE,
    NATURAL,
    POLITE,
}

enum class ConversationPhase {
    IDLE,
    REQUESTING_MICROPHONE_PERMISSION,
    LISTENING,
    PROCESSING_PARTIAL_SPEECH,
    FINALIZING_SENTENCE,
    TRANSLATING,
    GENERATING_REPLIES,
    SPEAKING_RESPONSE,
    WAITING_FOR_NEXT_SPEAKER,
    PAUSED,
    STOPPED,
    RECOVERABLE_ERROR,
}

enum class TurnStatus {
    TRANSCRIBED,
    TRANSLATING,
    TRANSLATED,
    GENERATING_REPLIES,
    READY,
    FAILED,
    SUPERSEDED,
}

data class ReplySuggestion(
    val text: String,
    val tone: ReplyTone,
    val bengaliExplanation: String? = null,
)

data class ConversationTurn(
    val id: String,
    val direction: TranslationDirection,
    val sourceText: String,
    val translatedText: String? = null,
    val replySuggestions: List<ReplySuggestion> = emptyList(),
    val createdAtEpochMillis: Long,
    val status: TurnStatus = TurnStatus.TRANSCRIBED,
)

data class ConversationSnapshot(
    val phase: ConversationPhase = ConversationPhase.IDLE,
    val sessionActive: Boolean = false,
    val microphoneCaptureActive: Boolean = false,
    val turns: List<ConversationTurn> = emptyList(),
    val interimTranscript: String? = null,
    val activeTurnId: String? = null,
    val queuedTurnIds: List<String> = emptyList(),
    val recentTranscriptIds: List<String> = emptyList(),
    val recoverableError: String? = null,
) {
    fun boundedContext(maxTurns: Int = DEFAULT_CONTEXT_TURN_LIMIT): ConversationContext {
        require(maxTurns >= 0) { "maxTurns must be non-negative" }
        return ConversationContext(
            turns = turns
                .filter { it.status != TurnStatus.SUPERSEDED && it.status != TurnStatus.FAILED }
                .filter { it.translatedText != null }
                .takeLast(maxTurns),
        )
    }

    companion object {
        const val DEFAULT_CONTEXT_TURN_LIMIT = 12
    }
}

data class ConversationContext(val turns: List<ConversationTurn>)
