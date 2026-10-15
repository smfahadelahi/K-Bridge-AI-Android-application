package com.kbridge.ai.core.domain.phrasebook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePhrasebookTest {
    @Test
    fun bundledPhraseIdsAreUniqueAndStable() {
        val ids = OfflinePhrasebook.entries.map(PhraseEntry::id)

        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.startsWith("bundled:") })
    }

    @Test
    fun bundledPhrasesContainBothScriptsAndARealSituation() {
        assertTrue(OfflinePhrasebook.entries.isNotEmpty())
        OfflinePhrasebook.entries.forEach { phrase ->
            assertTrue(phrase.korean.any { it in '\uac00'..'\ud7a3' })
            assertTrue(phrase.bangla.any { it in '\u0980'..'\u09ff' })
            assertFalse(phrase.korean.isBlank())
            assertFalse(phrase.bangla.isBlank())
            assertTrue(phrase.isBundled)
            assertFalse(phrase.isSaved)
        }
    }

    @Test
    fun phrasebookCoversEveryStarterScenario() {
        assertEquals(PhraseScenario.entries.toSet(), OfflinePhrasebook.entries.map { it.scenario }.toSet())
    }
}
