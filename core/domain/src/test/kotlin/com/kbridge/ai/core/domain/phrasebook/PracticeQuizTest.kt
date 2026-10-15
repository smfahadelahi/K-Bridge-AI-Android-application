package com.kbridge.ai.core.domain.phrasebook

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeQuizTest {
    @Test
    fun emptySavedPhraseListHasNoQuestion() {
        assertNull(PracticeQuiz.nextQuestion(emptyList(), random = Random(1)))
    }

    @Test
    fun questionUsesASavedPhraseAndOneOfTheTwoDirections() {
        val saved = listOf(phrase("saved-1"), phrase("saved-2"))

        val question = PracticeQuiz.nextQuestion(saved, random = Random(2))

        val actualQuestion = requireNotNull(question)
        assertTrue(saved.any { it.id == actualQuestion.phraseId })
        assertTrue(QuizDirection.entries.contains(actualQuestion.direction))
    }

    @Test
    fun questionAvoidsRepeatingThePreviousPhraseWhenPossible() {
        val saved = listOf(phrase("saved-1"), phrase("saved-2"))

        val question = PracticeQuiz.nextQuestion(saved, previousPhraseId = "saved-1", random = Random(4))

        assertEquals("saved-2", question?.phraseId)
    }

    @Test
    fun questionAlternatesDirectionWhenOnlyOnePhraseIsSaved() {
        val saved = listOf(phrase("saved-1"))

        val question =
            PracticeQuiz.nextQuestion(
                savedPhrases = saved,
                previousPhraseId = "saved-1",
                previousDirection = QuizDirection.KOREAN_TO_BANGLA,
                random = Random(8),
            )

        assertEquals(QuizDirection.BANGLA_TO_KOREAN, question?.direction)
    }

    private fun phrase(id: String) =
        PhraseEntry(
            id = id,
            korean = "안녕하세요.",
            bangla = "হ্যালো।",
            scenario = PhraseScenario.EVERYDAY,
            isBundled = true,
            isSaved = true,
            createdAtEpochMillis = 0,
        )
}
