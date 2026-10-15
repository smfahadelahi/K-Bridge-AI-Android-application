package com.kbridge.ai.data.repository

import com.kbridge.ai.core.domain.phrasebook.OfflinePhrasebook
import com.kbridge.ai.core.domain.phrasebook.PhraseScenario
import com.kbridge.ai.core.domain.phrasebook.TranslationHistoryEntry
import com.kbridge.ai.data.local.PhraseDao
import com.kbridge.ai.data.local.PhraseEntity
import com.kbridge.ai.data.local.TranslationHistoryDao
import com.kbridge.ai.data.local.TranslationHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomPhraseRepositoryTest {
    private val phraseDao = InMemoryPhraseDao()
    private val historyDao = InMemoryTranslationHistoryDao()
    private val repository = RoomPhraseRepository(phraseDao, historyDao, clock = { FIXED_TIME })

    @Test
    fun bundledPhraseSeedingIsIdempotentAndSavedStateCanBeToggled() =
        runBlocking {
            repository.seedBundledPhrases()
            repository.seedBundledPhrases()

            val initialPhrasebook = repository.observePhrasebook().first()
            assertEquals(OfflinePhrasebook.entries.size, initialPhrasebook.size)

            val firstPhrase = initialPhrasebook.first()
            repository.setPhraseSaved(firstPhrase, saved = true)
            assertTrue(repository.observeSavedPhrases().first().any { it.id == firstPhrase.id })

            repository.setPhraseSaved(firstPhrase.copy(isSaved = true), saved = false)
            assertTrue(repository.observeSavedPhrases().first().isEmpty())
            assertEquals(OfflinePhrasebook.entries.size, repository.observePhrasebook().first().size)
        }

    @Test
    fun savedUserPhraseIsTrimmedAndRemovedWhenUnbookmarked() =
        runBlocking {
            repository.saveUserPhrase("  안녕하세요.  ", "  হ্যালো।  ", PhraseScenario.EVERYDAY)

            val savedPhrase = repository.observeSavedPhrases().first().single()
            assertEquals("안녕하세요.", savedPhrase.korean)
            assertEquals("হ্যালো।", savedPhrase.bangla)
            assertEquals(PhraseScenario.EVERYDAY, savedPhrase.scenario)
            assertTrue(repository.observePhrasebook().first().any { it.id == savedPhrase.id })

            repository.setPhraseSaved(savedPhrase, saved = false)
            assertTrue(repository.observeSavedPhrases().first().isEmpty())
            assertTrue(repository.observePhrasebook().first().isEmpty())
        }

    @Test
    fun historyIsBoundedAndCanBeDeleted() =
        runBlocking {
            repeat(HISTORY_LIMIT + 1) { index ->
                repository.recordTranslation(
                    TranslationHistoryEntry(
                        id = "history-$index",
                        sourceText = "source-$index",
                        translatedText = "translated-$index",
                        direction = "KOREAN_TO_BANGLA",
                        createdAtEpochMillis = index.toLong(),
                    ),
                )
            }

            val retainedHistory = repository.observeRecentHistory(HISTORY_LIMIT + 1).first()
            assertEquals(HISTORY_LIMIT, retainedHistory.size)
            assertEquals(HISTORY_LIMIT.toLong(), retainedHistory.first().createdAtEpochMillis)

            repository.clearHistory()
            assertTrue(repository.observeRecentHistory(HISTORY_LIMIT).first().isEmpty())
        }

    private class InMemoryPhraseDao : PhraseDao {
        private val phrases = linkedMapOf<String, PhraseEntity>()
        private val phrasebook = MutableStateFlow(emptyList<PhraseEntity>())
        private val savedPhrases = MutableStateFlow(emptyList<PhraseEntity>())

        override fun observePhrasebook(): Flow<List<PhraseEntity>> = phrasebook

        override fun observeSavedPhrases(): Flow<List<PhraseEntity>> = savedPhrases

        override suspend fun insertBundledPhrases(phrases: List<PhraseEntity>) {
            phrases.forEach { phrase -> this.phrases.putIfAbsent(phrase.id, phrase) }
            publish()
        }

        override suspend fun upsertPhrase(phrase: PhraseEntity) {
            phrases[phrase.id] = phrase
            publish()
        }

        override suspend fun setSaved(
            id: String,
            saved: Boolean,
            updatedAt: Long,
        ) {
            phrases[id]?.let { phrase ->
                phrases[id] = phrase.copy(isSaved = saved, updatedAtEpochMillis = updatedAt)
            }
            publish()
        }

        override suspend fun deleteUserPhrase(id: String) {
            if (phrases[id]?.isBundled == false) phrases.remove(id)
            publish()
        }

        private fun publish() {
            val orderedPhrases = phrases.values.sortedWith(compareBy(PhraseEntity::scenarioId, PhraseEntity::korean))
            phrasebook.value = orderedPhrases.filter { it.isBundled || it.isSaved }
            savedPhrases.value =
                phrases.values
                    .filter(PhraseEntity::isSaved)
                    .sortedWith(compareByDescending(PhraseEntity::updatedAtEpochMillis).thenBy(PhraseEntity::korean))
        }
    }

    private class InMemoryTranslationHistoryDao : TranslationHistoryDao {
        private val entries = linkedMapOf<String, TranslationHistoryEntity>()
        private val history = MutableStateFlow(emptyList<TranslationHistoryEntity>())

        override fun observeRecent(limit: Int): Flow<List<TranslationHistoryEntity>> =
            history.map { recentEntries -> recentEntries.take(limit) }

        override suspend fun insert(entry: TranslationHistoryEntity) {
            entries[entry.id] = entry
            publish()
        }

        override suspend fun deleteEntriesBeyondLimit() {
            val retainedIds =
                entries.values
                    .sortedByDescending(TranslationHistoryEntity::createdAtEpochMillis)
                    .take(HISTORY_LIMIT)
                    .map(TranslationHistoryEntity::id)
                    .toSet()
            entries.keys.retainAll(retainedIds)
            publish()
        }

        override suspend fun deleteAll() {
            entries.clear()
            publish()
        }

        private fun publish() {
            history.value = entries.values.sortedByDescending(TranslationHistoryEntity::createdAtEpochMillis)
        }
    }

    private companion object {
        const val HISTORY_LIMIT = 100
        const val FIXED_TIME = 42L
    }
}
