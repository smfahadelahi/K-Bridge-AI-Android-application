package com.kbridge.ai.core.domain.conversation

/**
 * Pure reducer for the live-conversation lifecycle. Engine effects (microphone, model calls, and TTS)
 * are deliberately outside this class so transitions can be tested without Android or model files.
 */
class ConversationReducer(
    private val maxVisibleTurns: Int = 80,
    private val duplicateWindowSize: Int = 128,
) {
    init {
        require(maxVisibleTurns > 0) { "maxVisibleTurns must be positive" }
        require(duplicateWindowSize > 0) { "duplicateWindowSize must be positive" }
    }

    fun reduce(
        snapshot: ConversationSnapshot,
        event: ConversationEvent,
    ): ConversationSnapshot =
        when (event) {
            ConversationEvent.StartRequested -> start(snapshot)
            ConversationEvent.MicrophoneCaptureStarted -> microphoneStarted(snapshot)
            ConversationEvent.MicrophonePermissionDenied -> permissionDenied(snapshot)
            is ConversationEvent.PartialTranscriptReceived -> partialTranscript(snapshot, event.text)
            is ConversationEvent.FinalTranscriptReceived -> finalTranscript(snapshot, event)
            is ConversationEvent.TranslationStarted -> translationStarted(snapshot, event.turnId)
            is ConversationEvent.TranslationCompleted -> translationCompleted(snapshot, event)
            is ConversationEvent.ReplyGenerationStarted -> replyGenerationStarted(snapshot, event.turnId)
            is ConversationEvent.ReplySuggestionsReady -> repliesReady(snapshot, event)
            is ConversationEvent.ReplyGenerationSkipped -> replyGenerationSkipped(snapshot, event.turnId)
            is ConversationEvent.SpeakRequested -> speak(snapshot, event.text)
            ConversationEvent.PlaybackCompleted -> playbackFinished(snapshot)
            ConversationEvent.PlaybackInterrupted -> playbackFinished(snapshot)
            ConversationEvent.PauseRequested -> pause(snapshot)
            ConversationEvent.ResumeRequested -> resume(snapshot)
            ConversationEvent.StopRequested -> stop(snapshot)
            ConversationEvent.ClearSessionRequested -> ConversationSnapshot()
            is ConversationEvent.CancelTurnRequested -> cancelTurn(snapshot, event.turnId)
            is ConversationEvent.RecoverableFailure -> fail(snapshot, event.message)
            ConversationEvent.RetryRequested -> retry(snapshot)
        }

    private fun start(snapshot: ConversationSnapshot): ConversationSnapshot {
        if (snapshot.phase !in
            setOf(
                ConversationPhase.IDLE,
                ConversationPhase.STOPPED,
                ConversationPhase.RECOVERABLE_ERROR,
            )
        ) {
            return snapshot
        }
        return ConversationSnapshot(
            phase = ConversationPhase.REQUESTING_MICROPHONE_PERMISSION,
            sessionActive = true,
        )
    }

    private fun microphoneStarted(snapshot: ConversationSnapshot): ConversationSnapshot {
        if (!snapshot.sessionActive || snapshot.phase !in
            setOf(
                ConversationPhase.REQUESTING_MICROPHONE_PERMISSION,
                ConversationPhase.PAUSED,
                ConversationPhase.WAITING_FOR_NEXT_SPEAKER,
            )
        ) {
            return snapshot
        }
        return snapshot.copy(
            phase =
                if (snapshot.activeTurnId == null) {
                    ConversationPhase.LISTENING
                } else {
                    ConversationPhase.FINALIZING_SENTENCE
                },
            microphoneCaptureActive = true,
            recoverableError = null,
        )
    }

    private fun permissionDenied(snapshot: ConversationSnapshot): ConversationSnapshot {
        if (snapshot.phase != ConversationPhase.REQUESTING_MICROPHONE_PERMISSION) return snapshot
        return snapshot.copy(
            phase = ConversationPhase.RECOVERABLE_ERROR,
            sessionActive = false,
            microphoneCaptureActive = false,
            interimTranscript = null,
            recoverableError = "Microphone permission was not granted.",
        )
    }

    private fun partialTranscript(
        snapshot: ConversationSnapshot,
        text: String,
    ): ConversationSnapshot {
        if (!snapshot.sessionActive || !snapshot.microphoneCaptureActive) return snapshot
        val normalized = text.trim()
        if (normalized.isEmpty() || normalized == snapshot.interimTranscript) return snapshot
        return snapshot.copy(
            phase = ConversationPhase.PROCESSING_PARTIAL_SPEECH,
            interimTranscript = normalized,
            recoverableError = null,
        )
    }

    private fun finalTranscript(
        snapshot: ConversationSnapshot,
        event: ConversationEvent.FinalTranscriptReceived,
    ): ConversationSnapshot {
        val normalized = event.text.trim()
        if (!snapshot.sessionActive || !snapshot.microphoneCaptureActive || normalized.isEmpty()) return snapshot
        if (event.transcriptId.isBlank() || event.transcriptId in snapshot.recentTranscriptIds) return snapshot

        val turn =
            ConversationTurn(
                id = event.transcriptId,
                direction = event.direction,
                sourceText = normalized,
                createdAtEpochMillis = event.timestampEpochMillis,
            )
        val nextTurns = (snapshot.turns + turn).takeLast(maxVisibleTurns)
        val isBusy = snapshot.activeTurnId != null
        val nextQueued = if (isBusy) snapshot.queuedTurnIds + turn.id else snapshot.queuedTurnIds
        val nextActive = snapshot.activeTurnId ?: turn.id
        val nextPhase = if (isBusy) snapshot.phase else ConversationPhase.FINALIZING_SENTENCE
        return snapshot.copy(
            phase = nextPhase,
            turns = nextTurns,
            interimTranscript = null,
            activeTurnId = nextActive,
            queuedTurnIds = nextQueued,
            recentTranscriptIds = (snapshot.recentTranscriptIds + event.transcriptId).takeLast(duplicateWindowSize),
            recoverableError = null,
        )
    }

    private fun translationStarted(
        snapshot: ConversationSnapshot,
        turnId: String,
    ): ConversationSnapshot {
        if (turnId != snapshot.activeTurnId) return snapshot
        val turn = snapshot.turns.find { it.id == turnId } ?: return snapshot
        if (turn.status != TurnStatus.TRANSCRIBED) return snapshot
        return updateTurn(snapshot, turnId) { it.copy(status = TurnStatus.TRANSLATING) }.copy(
            phase = ConversationPhase.TRANSLATING,
        )
    }

    private fun translationCompleted(
        snapshot: ConversationSnapshot,
        event: ConversationEvent.TranslationCompleted,
    ): ConversationSnapshot {
        if (event.turnId != snapshot.activeTurnId || snapshot.phase != ConversationPhase.TRANSLATING) {
            return snapshot
        }
        val translated = event.translatedText.trim()
        if (translated.isEmpty()) return snapshot
        return updateTurn(snapshot, event.turnId) {
            it.copy(translatedText = translated, status = TurnStatus.TRANSLATED)
        }.copy(phase = ConversationPhase.GENERATING_REPLIES)
    }

    private fun replyGenerationStarted(
        snapshot: ConversationSnapshot,
        turnId: String,
    ): ConversationSnapshot {
        if (turnId != snapshot.activeTurnId || snapshot.phase != ConversationPhase.GENERATING_REPLIES) {
            return snapshot
        }
        val turn = snapshot.turns.find { it.id == turnId } ?: return snapshot
        if (turn.status != TurnStatus.TRANSLATED) return snapshot
        return updateTurn(snapshot, turnId) { it.copy(status = TurnStatus.GENERATING_REPLIES) }
    }

    private fun repliesReady(
        snapshot: ConversationSnapshot,
        event: ConversationEvent.ReplySuggestionsReady,
    ): ConversationSnapshot {
        if (event.turnId != snapshot.activeTurnId || snapshot.phase != ConversationPhase.GENERATING_REPLIES) {
            return snapshot
        }
        val turn = snapshot.turns.find { it.id == event.turnId } ?: return snapshot
        if (turn.status !in setOf(TurnStatus.TRANSLATED, TurnStatus.GENERATING_REPLIES)) return snapshot
        return finishActiveTurn(
            snapshot =
                updateTurn(snapshot, event.turnId) {
                    it.copy(replySuggestions = event.suggestions.toList(), status = TurnStatus.READY)
                },
        )
    }

    private fun replyGenerationSkipped(
        snapshot: ConversationSnapshot,
        turnId: String,
    ): ConversationSnapshot {
        if (turnId != snapshot.activeTurnId || snapshot.phase != ConversationPhase.GENERATING_REPLIES) {
            return snapshot
        }
        val turn = snapshot.turns.find { it.id == turnId } ?: return snapshot
        if (turn.status != TurnStatus.TRANSLATED) return snapshot
        return finishActiveTurn(
            snapshot = updateTurn(snapshot, turnId) { it.copy(status = TurnStatus.READY) },
        )
    }

    private fun finishActiveTurn(snapshot: ConversationSnapshot): ConversationSnapshot {
        val nextId = snapshot.queuedTurnIds.firstOrNull()
        val nextQueue = if (nextId == null) emptyList() else snapshot.queuedTurnIds.drop(1)
        return snapshot.copy(
            phase =
                if (nextId != null) {
                    ConversationPhase.FINALIZING_SENTENCE
                } else {
                    ConversationPhase.WAITING_FOR_NEXT_SPEAKER
                },
            activeTurnId = nextId,
            queuedTurnIds = nextQueue,
        )
    }

    private fun speak(
        snapshot: ConversationSnapshot,
        text: String,
    ): ConversationSnapshot {
        if (!snapshot.sessionActive || text.isBlank() || snapshot.phase !in
            setOf(
                ConversationPhase.WAITING_FOR_NEXT_SPEAKER,
                ConversationPhase.LISTENING,
            )
        ) {
            return snapshot
        }
        return snapshot.copy(
            phase = ConversationPhase.SPEAKING_RESPONSE,
            microphoneCaptureActive = false,
            interimTranscript = null,
        )
    }

    private fun playbackFinished(snapshot: ConversationSnapshot): ConversationSnapshot {
        if (snapshot.phase != ConversationPhase.SPEAKING_RESPONSE) return snapshot
        return snapshot.copy(
            phase = ConversationPhase.WAITING_FOR_NEXT_SPEAKER,
            microphoneCaptureActive = false,
        )
    }

    private fun pause(snapshot: ConversationSnapshot): ConversationSnapshot {
        if (!snapshot.sessionActive || snapshot.phase in
            setOf(
                ConversationPhase.IDLE,
                ConversationPhase.STOPPED,
                ConversationPhase.RECOVERABLE_ERROR,
            )
        ) {
            return snapshot
        }
        val pendingIds = snapshot.activeTurnId?.let { listOf(it) }.orEmpty() + snapshot.queuedTurnIds
        return snapshot.copy(
            phase = ConversationPhase.PAUSED,
            microphoneCaptureActive = false,
            turns =
                snapshot.turns.map { turn ->
                    if (turn.id in pendingIds) {
                        turn.copy(
                            translatedText = null,
                            replySuggestions = emptyList(),
                            status = TurnStatus.TRANSCRIBED,
                        )
                    } else {
                        turn
                    }
                },
            interimTranscript = null,
            activeTurnId = pendingIds.firstOrNull(),
            queuedTurnIds = pendingIds.drop(1),
        )
    }

    private fun resume(snapshot: ConversationSnapshot): ConversationSnapshot {
        if (snapshot.phase != ConversationPhase.PAUSED || !snapshot.sessionActive) return snapshot
        return snapshot.copy(phase = ConversationPhase.REQUESTING_MICROPHONE_PERMISSION)
    }

    private fun stop(snapshot: ConversationSnapshot): ConversationSnapshot {
        if (snapshot.phase == ConversationPhase.IDLE || snapshot.phase == ConversationPhase.STOPPED) return snapshot
        val pendingIds = snapshot.queuedTurnIds + listOfNotNull(snapshot.activeTurnId)
        return snapshot.copy(
            phase = ConversationPhase.STOPPED,
            sessionActive = false,
            microphoneCaptureActive = false,
            turns =
                snapshot.turns.map { turn ->
                    if (turn.id in pendingIds) turn.copy(status = TurnStatus.SUPERSEDED) else turn
                },
            interimTranscript = null,
            activeTurnId = null,
            queuedTurnIds = emptyList(),
            recoverableError = null,
        )
    }

    private fun cancelTurn(
        snapshot: ConversationSnapshot,
        turnId: String,
    ): ConversationSnapshot {
        if (turnId == snapshot.activeTurnId) {
            val next = updateTurn(snapshot, turnId) { it.copy(status = TurnStatus.SUPERSEDED) }
            return finishActiveTurn(next)
        }
        if (turnId !in snapshot.queuedTurnIds) return snapshot
        return snapshot.copy(
            turns =
                snapshot.turns.map { turn ->
                    if (turn.id == turnId) turn.copy(status = TurnStatus.SUPERSEDED) else turn
                },
            queuedTurnIds = snapshot.queuedTurnIds.filterNot { it == turnId },
        )
    }

    private fun fail(
        snapshot: ConversationSnapshot,
        message: String,
    ): ConversationSnapshot {
        if (!snapshot.sessionActive || message.isBlank()) return snapshot
        val pendingIds = snapshot.queuedTurnIds + listOfNotNull(snapshot.activeTurnId)
        return snapshot.copy(
            phase = ConversationPhase.RECOVERABLE_ERROR,
            microphoneCaptureActive = false,
            turns =
                snapshot.turns.map { turn ->
                    if (turn.id in pendingIds) turn.copy(status = TurnStatus.FAILED) else turn
                },
            interimTranscript = null,
            activeTurnId = null,
            queuedTurnIds = emptyList(),
            recoverableError = message.trim(),
        )
    }

    private fun retry(snapshot: ConversationSnapshot): ConversationSnapshot {
        if (snapshot.phase != ConversationPhase.RECOVERABLE_ERROR) return snapshot
        return snapshot.copy(
            phase = ConversationPhase.REQUESTING_MICROPHONE_PERMISSION,
            sessionActive = true,
            microphoneCaptureActive = false,
            recoverableError = null,
        )
    }

    private inline fun updateTurn(
        snapshot: ConversationSnapshot,
        turnId: String,
        transform: (ConversationTurn) -> ConversationTurn,
    ): ConversationSnapshot =
        snapshot.copy(
            turns = snapshot.turns.map { turn -> if (turn.id == turnId) transform(turn) else turn },
        )
}
