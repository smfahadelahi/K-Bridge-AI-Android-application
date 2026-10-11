package com.kbridge.ai.core.domain.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationReducerTest {
    private val reducer = ConversationReducer()

    @Test
    fun microphoneIsNotReportedActiveUntilCaptureActuallyStarts() {
        val requesting = reducer.reduce(ConversationSnapshot(), ConversationEvent.StartRequested)

        assertEquals(ConversationPhase.REQUESTING_MICROPHONE_PERMISSION, requesting.phase)
        assertFalse(requesting.microphoneCaptureActive)

        val listening = reducer.reduce(requesting, ConversationEvent.MicrophoneCaptureStarted)

        assertEquals(ConversationPhase.LISTENING, listening.phase)
        assertTrue(listening.microphoneCaptureActive)
    }

    @Test
    fun deniedPermissionProducesRecoverableErrorWithoutStartingCapture() {
        val requesting = reducer.reduce(ConversationSnapshot(), ConversationEvent.StartRequested)
        val denied = reducer.reduce(requesting, ConversationEvent.MicrophonePermissionDenied)

        assertEquals(ConversationPhase.RECOVERABLE_ERROR, denied.phase)
        assertFalse(denied.sessionActive)
        assertFalse(denied.microphoneCaptureActive)
        assertTrue(denied.recoverableError!!.contains("permission"))
    }

    @Test
    fun interimTextIsSeparateAndDuplicateFinalTranscriptIsIgnored() {
        val listening = listeningSnapshot()
        val partial = reducer.reduce(listening, ConversationEvent.PartialTranscriptReceived("안녕하세요"))
        assertEquals("안녕하세요", partial.interimTranscript)
        assertTrue(partial.turns.isEmpty())

        val finalized = reducer.reduce(partial, finalEvent("utterance-1", "안녕하세요"))
        val duplicate = reducer.reduce(finalized, finalEvent("utterance-1", "다른 내용"))

        assertEquals(1, duplicate.turns.size)
        assertEquals("안녕하세요", duplicate.turns.single().sourceText)
        assertNull(duplicate.interimTranscript)
        assertEquals(finalized, duplicate)
    }

    @Test
    fun incomingSentencesQueueAndOnlyCurrentTurnCanCompleteTranslation() {
        var state = listeningSnapshot()
        state = reducer.reduce(state, finalEvent("first", "첫 번째 문장"))
        state = reducer.reduce(state, finalEvent("second", "두 번째 문장"))

        assertEquals("first", state.activeTurnId)
        assertEquals(listOf("second"), state.queuedTurnIds)

        state = reducer.reduce(state, ConversationEvent.TranslationStarted("first"))
        val stale = reducer.reduce(state, ConversationEvent.TranslationCompleted("second", "늦은 결과"))
        assertEquals(state, stale)

        state = reducer.reduce(state, ConversationEvent.TranslationCompleted("first", "প্রথম বাক্য"))
        assertEquals("প্রথম বাক্য", state.turns.first().translatedText)
        assertEquals(ConversationPhase.GENERATING_REPLIES, state.phase)
    }

    @Test
    fun completedTurnAdvancesTheRapidFollowUpQueueInOrder() {
        var state = listeningSnapshot()
        state = reducer.reduce(state, finalEvent("first", "첫 번째 질문"))
        state = reducer.reduce(state, finalEvent("second", "두 번째 질문"))
        state = reducer.reduce(state, ConversationEvent.TranslationStarted("first"))
        state = reducer.reduce(state, ConversationEvent.TranslationCompleted("first", "প্রথম প্রশ্ন"))
        state = reducer.reduce(state, ConversationEvent.ReplyGenerationStarted("first"))
        state =
            reducer.reduce(
                state,
                ConversationEvent.ReplySuggestionsReady("first", emptyList()),
            )

        assertEquals("second", state.activeTurnId)
        assertTrue(state.queuedTurnIds.isEmpty())
        assertEquals(ConversationPhase.FINALIZING_SENTENCE, state.phase)
        assertEquals(listOf("first"), state.boundedContext().turns.map { it.id })
    }

    @Test
    fun pauseStopsCaptureAndKeepsTranscribedWorkForResume() {
        var state = listeningSnapshot()
        state = reducer.reduce(state, finalEvent("turn-1", "다음 주에 다시 확인해 주세요"))
        state = reducer.reduce(state, ConversationEvent.TranslationStarted("turn-1"))
        state = reducer.reduce(state, ConversationEvent.PauseRequested)

        assertEquals(ConversationPhase.PAUSED, state.phase)
        assertFalse(state.microphoneCaptureActive)
        assertEquals("turn-1", state.activeTurnId)
        assertEquals(TurnStatus.TRANSCRIBED, state.turns.single().status)

        state = reducer.reduce(state, ConversationEvent.ResumeRequested)
        state = reducer.reduce(state, ConversationEvent.MicrophoneCaptureStarted)
        assertEquals(ConversationPhase.FINALIZING_SENTENCE, state.phase)
        assertTrue(state.microphoneCaptureActive)
    }

    @Test
    fun boundedContextReturnsMostRecentNonSupersededTurns() {
        val turns =
            (1..4).map { index ->
                ConversationTurn(
                    id = "turn-$index",
                    direction = TranslationDirection.KOREAN_TO_BANGLA,
                    sourceText = "문장 $index",
                    translatedText = "বাক্য $index",
                    createdAtEpochMillis = index.toLong(),
                    status = if (index == 3) TurnStatus.SUPERSEDED else TurnStatus.READY,
                )
            }
        val snapshot = ConversationSnapshot(turns = turns)

        assertEquals(
            listOf("turn-2", "turn-4"),
            snapshot.boundedContext(maxTurns = 2).turns.map { it.id },
        )
    }

    @Test
    fun stoppingDisablesCaptureAndRejectsLateModelResults() {
        var state = listeningSnapshot()
        state = reducer.reduce(state, finalEvent("turn-1", "일정이 바뀌었습니다"))
        state = reducer.reduce(state, ConversationEvent.TranslationStarted("turn-1"))
        state = reducer.reduce(state, ConversationEvent.StopRequested)
        val late = reducer.reduce(state, ConversationEvent.TranslationCompleted("turn-1", "সময়সূচি বদলেছে"))

        assertEquals(ConversationPhase.STOPPED, late.phase)
        assertFalse(late.sessionActive)
        assertFalse(late.microphoneCaptureActive)
        assertEquals(TurnStatus.SUPERSEDED, late.turns.single().status)
        assertNull(late.activeTurnId)
        assertEquals(state, late)
    }

    @Test
    fun playbackCanBeInterruptedAndSessionCanListenAgain() {
        var state = listeningSnapshot()
        state = reducer.reduce(state, ConversationEvent.SpeakRequested("천천히 말씀해 주세요."))
        assertEquals(ConversationPhase.SPEAKING_RESPONSE, state.phase)
        assertFalse(state.microphoneCaptureActive)

        state = reducer.reduce(state, ConversationEvent.PlaybackInterrupted)
        assertEquals(ConversationPhase.WAITING_FOR_NEXT_SPEAKER, state.phase)
        assertFalse(state.microphoneCaptureActive)

        state = reducer.reduce(state, ConversationEvent.MicrophoneCaptureStarted)
        assertEquals(ConversationPhase.LISTENING, state.phase)
        assertTrue(state.microphoneCaptureActive)
    }

    @Test
    fun cancellingAnActiveTurnSupersedesItAndIgnoresItsLateResult() {
        var state = listeningSnapshot()
        state = reducer.reduce(state, finalEvent("turn-1", "내일 출근 시간이 바뀌었습니다"))
        state = reducer.reduce(state, ConversationEvent.TranslationStarted("turn-1"))
        state = reducer.reduce(state, ConversationEvent.CancelTurnRequested("turn-1"))
        val late = reducer.reduce(state, ConversationEvent.TranslationCompleted("turn-1", "আগামীকাল সময় বদলেছে"))

        assertEquals(ConversationPhase.WAITING_FOR_NEXT_SPEAKER, late.phase)
        assertEquals(TurnStatus.SUPERSEDED, late.turns.single().status)
        assertNull(late.activeTurnId)
        assertEquals(state, late)
    }

    @Test
    fun clearSessionDeletesLocalConversationState() {
        var state = listeningSnapshot()
        state = reducer.reduce(state, finalEvent("turn-1", "오늘 야근이 있나요?"))
        state = reducer.reduce(state, ConversationEvent.ClearSessionRequested)

        assertEquals(ConversationSnapshot(), state)
        assertNotEquals(ConversationPhase.STOPPED, state.phase)
    }

    private fun listeningSnapshot(): ConversationSnapshot =
        reducer
            .reduce(ConversationSnapshot(), ConversationEvent.StartRequested)
            .let { reducer.reduce(it, ConversationEvent.MicrophoneCaptureStarted) }

    private fun finalEvent(
        id: String,
        text: String,
    ) = ConversationEvent.FinalTranscriptReceived(
        transcriptId = id,
        direction = TranslationDirection.KOREAN_TO_BANGLA,
        text = text,
        timestampEpochMillis = 1_000L,
    )
}
