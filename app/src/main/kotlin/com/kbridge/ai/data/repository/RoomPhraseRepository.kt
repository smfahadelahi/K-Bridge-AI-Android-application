package com.kbridge.ai.data.repository

import com.kbridge.ai.core.domain.phrasebook.OfflinePhrasebook
import com.kbridge.ai.core.domain.phrasebook.PhraseEntry
import com.kbridge.ai.core.domain.phrasebook.PhraseScenario
import com.kbridge.ai.core.domain.phrasebook.TranslationHistoryEntry
import com.kbridge.ai.data.local.PhraseDao
import com.kbridge.ai.data.local.TranslationHistoryDao
import com.kbridge.ai.data.local.TranslationHistoryEntity
import com.kbridge.ai.data.local.toDomain
import com.kbridge.ai.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomPhraseRepository(
    private val phraseDao: PhraseDao,
    private val historyDao: TranslationHistoryDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : PhraseRepository {
    override fun observePhrasebook(): Flow<List<PhraseEntry>> =
        phraseDao.observePhrasebook().map { entities -> entities.map { it.toDomain() } }

    override fun observeSavedPhrases(): Flow<List<PhraseEntry>> =
        phraseDao.observeSavedPhrases().map { entities -> entities.map { it.toDomain() } }

    override fun observeRecentHistory(limit: Int): Flow<List<TranslationHistoryEntry>> {
        require(limit > 0) { "limit must be positive" }
        return historyDao.observeRecent(limit).map { entries -> entries.map { it.toDomain() } }
    }

    override suspend fun seedBundledPhrases() {
        phraseDao.insertBundledPhrases(OfflinePhrasebook.entries.map(PhraseEntry::toEntity))
    }

    override suspend fun saveUserPhrase(
        korean: String,
        bangla: String,
        scenario: PhraseScenario,
    ) {
        val now = clock()
        phraseDao.upsertPhrase(
            PhraseEntry(
                id = "user:${UUID.randomUUID()}",
                korean = korean.trim(),
                bangla = bangla.trim(),
                scenario = scenario,
                isBundled = false,
                isSaved = true,
                createdAtEpochMillis = now,
            ).toEntity(),
        )
    }

    override suspend fun setPhraseSaved(
        phrase: PhraseEntry,
        saved: Boolean,
    ) {
        if (!saved && !phrase.isBundled) {
            phraseDao.deleteUserPhrase(phrase.id)
        } else {
            phraseDao.setSaved(phrase.id, saved, clock())
        }
    }

    override suspend fun recordTranslation(entry: TranslationHistoryEntry) {
        historyDao.insert(
            TranslationHistoryEntity(
                id = entry.id,
                sourceText = entry.sourceText,
                translatedText = entry.translatedText,
                direction = entry.direction,
                createdAtEpochMillis = entry.createdAtEpochMillis,
            ),
        )
        historyDao.deleteEntriesBeyondLimit()
    }

    override suspend fun clearHistory() {
        historyDao.deleteAll()
    }
}
