package com.kbridge.ai

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kbridge.ai.core.domain.phrasebook.OfflinePhrasebook
import com.kbridge.ai.core.domain.phrasebook.PhraseScenario
import com.kbridge.ai.core.domain.phrasebook.TranslationHistoryEntry
import com.kbridge.ai.data.local.KBridgeDatabase
import com.kbridge.ai.data.repository.RoomPhraseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LocalPhraseStorageTest {
    private lateinit var database: KBridgeDatabase
    private lateinit var repository: RoomPhraseRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database =
            Room
                .inMemoryDatabaseBuilder(context, KBridgeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = RoomPhraseRepository(database.phraseDao(), database.translationHistoryDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun bundledPhrasesAreAvailableAndCanBeSaved() =
        runBlocking {
            repository.seedBundledPhrases()

            val phrasebook = repository.observePhrasebook().first()
            assertEquals(OfflinePhrasebook.entries.size, phrasebook.size)

            val phrase = phrasebook.first()
            repository.setPhraseSaved(phrase, saved = true)

            assertTrue(repository.observeSavedPhrases().first().any { it.id == phrase.id })
        }

    @Test
    fun localTranslationHistoryCanBeDeleted() =
        runBlocking {
            repository.recordTranslation(
                TranslationHistoryEntry(
                    id = UUID.randomUUID().toString(),
                    sourceText = "안녕하세요.",
                    translatedText = "হ্যালো।",
                    direction = "KOREAN_TO_BANGLA",
                    createdAtEpochMillis = 1L,
                ),
            )

            assertEquals(1, repository.observeRecentHistory(20).first().size)
            repository.clearHistory()
            assertTrue(repository.observeRecentHistory(20).first().isEmpty())
        }

    @Test
    fun savedUserPhraseIsStoredWithItsSituation() =
        runBlocking {
            repository.saveUserPhrase("안녕하세요.", "হ্যালো।", PhraseScenario.EVERYDAY)

            val saved = repository.observeSavedPhrases().first()
            assertEquals(1, saved.size)
            assertEquals(PhraseScenario.EVERYDAY, saved.single().scenario)
        }
}
