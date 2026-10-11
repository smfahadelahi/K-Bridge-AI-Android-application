package com.kbridge.ai.core.domain.engine

import com.kbridge.ai.core.domain.conversation.ConversationLanguage
import com.kbridge.ai.core.domain.conversation.TranslationDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EngineContractsTest {
    @Test
    fun offlineCapabilityIsFalseUntilAnImplementationExplicitlyVerifiesIt() {
        val unavailable = EngineAvailability(EngineAvailabilityState.NOT_CONFIGURED)

        assertFalse(unavailable.offlineVerified)
    }

    @Test
    fun translationDirectionsKeepTheSourceAndTargetLanguagesExplicit() {
        assertEquals(
            ConversationLanguage.KOREAN,
            TranslationDirection.KOREAN_TO_BANGLA.sourceLanguage,
        )
        assertEquals(
            ConversationLanguage.BANGLA,
            TranslationDirection.KOREAN_TO_BANGLA.targetLanguage,
        )
        assertEquals(
            ConversationLanguage.BANGLA,
            TranslationDirection.BANGLA_TO_KOREAN.sourceLanguage,
        )
        assertEquals(
            ConversationLanguage.KOREAN,
            TranslationDirection.BANGLA_TO_KOREAN.targetLanguage,
        )
    }
}
