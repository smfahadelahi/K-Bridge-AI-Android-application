package com.kbridge.ai.core.domain.phrasebook

import kotlin.random.Random

enum class QuizDirection {
    KOREAN_TO_BANGLA,
    BANGLA_TO_KOREAN,
}

data class PracticeQuestion(
    val phraseId: String,
    val direction: QuizDirection,
)

object PracticeQuiz {
    fun nextQuestion(
        savedPhrases: List<PhraseEntry>,
        previousPhraseId: String? = null,
        previousDirection: QuizDirection? = null,
        random: Random = Random.Default,
    ): PracticeQuestion? {
        if (savedPhrases.isEmpty()) return null
        val candidates = savedPhrases.filterNot { it.id == previousPhraseId }.ifEmpty { savedPhrases }
        val directions = QuizDirection.entries.filterNot { it == previousDirection }.ifEmpty { QuizDirection.entries }
        val phrase = candidates[random.nextInt(candidates.size)]
        val direction = directions[random.nextInt(directions.size)]
        return PracticeQuestion(phraseId = phrase.id, direction = direction)
    }
}
