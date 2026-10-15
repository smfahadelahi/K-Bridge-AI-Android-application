package com.kbridge.ai.data.repository

import com.kbridge.ai.core.domain.phrasebook.PhraseEntry
import com.kbridge.ai.core.domain.phrasebook.PhraseScenario
import com.kbridge.ai.core.domain.phrasebook.TranslationHistoryEntry
import kotlinx.coroutines.flow.Flow

interface PhraseRepository {
    fun observePhrasebook(): Flow<List<PhraseEntry>>

    fun observeSavedPhrases(): Flow<List<PhraseEntry>>

    fun observeRecentHistory(limit: Int): Flow<List<TranslationHistoryEntry>>

    suspend fun seedBundledPhrases()

    suspend fun saveUserPhrase(
        korean: String,
        bangla: String,
        scenario: PhraseScenario,
    )

    suspend fun setPhraseSaved(
        phrase: PhraseEntry,
        saved: Boolean,
    )

    suspend fun recordTranslation(entry: TranslationHistoryEntry)

    suspend fun clearHistory()
}
