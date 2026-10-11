package com.kbridge.ai.core.domain.engine

import com.kbridge.ai.core.domain.conversation.ConversationContext
import com.kbridge.ai.core.domain.conversation.ConversationLanguage
import com.kbridge.ai.core.domain.conversation.ConversationTurn
import com.kbridge.ai.core.domain.conversation.ReplySuggestion
import com.kbridge.ai.core.domain.conversation.ReplyTone
import com.kbridge.ai.core.domain.conversation.TranslationDirection

enum class EngineAvailabilityState {
    READY,
    NOT_CONFIGURED,
    MODEL_MISSING,
    DOWNLOAD_REQUIRED,
    LANGUAGE_UNAVAILABLE,
    ERROR,
}

data class EngineAvailability(
    val state: EngineAvailabilityState,
    val detail: String? = null,
    /** True only after the complete execution path has been verified without network access. */
    val offlineVerified: Boolean = false,
)

data class SpeechRecognitionRequest(val language: ConversationLanguage)

sealed interface SpeechRecognitionUpdate {
    data class Partial(val text: String) : SpeechRecognitionUpdate
    data class Final(val transcriptId: String, val text: String) : SpeechRecognitionUpdate
    data class RecoverableError(val detail: String) : SpeechRecognitionUpdate
}

/** Streaming speech adapter contract. Implementations must report partial and final results distinctly. */
interface SpeechRecognitionEngine {
    fun availability(language: ConversationLanguage): EngineAvailability
    fun start(request: SpeechRecognitionRequest, listener: (SpeechRecognitionUpdate) -> Unit)
    fun stop()
}

data class TranslationRequest(
    val text: String,
    val direction: TranslationDirection,
)

data class TranslationOutput(
    val text: String,
    val notes: List<String> = emptyList(),
)

interface TranslationEngine {
    fun availability(direction: TranslationDirection): EngineAvailability
    suspend fun translate(request: TranslationRequest): TranslationOutput
}

data class ReplyGenerationRequest(
    val context: ConversationContext,
    val incomingTurn: ConversationTurn,
    val tones: List<ReplyTone> = listOf(ReplyTone.SIMPLE, ReplyTone.NATURAL, ReplyTone.POLITE),
)

/** Local-first response generation contract. Network-backed implementations are not part of this module. */
interface ConversationAgent {
    fun availability(): EngineAvailability
    suspend fun suggestReplies(request: ReplyGenerationRequest): List<ReplySuggestion>
}

interface SpeechSynthesisEngine {
    fun availability(language: ConversationLanguage): EngineAvailability
    fun speak(text: String, language: ConversationLanguage, rate: Float)
    fun pause()
    fun resume()
    fun stop()
    fun release()
}
