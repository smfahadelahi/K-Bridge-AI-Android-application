package com.kbridge.ai.core.domain.phrasebook

/** Situation labels remain stable identifiers so localized labels can live in Android resources. */
enum class PhraseScenario(val id: String) {
    EVERYDAY("everyday"),
    SHOPPING("shopping"),
    TRANSPORT("transport"),
    HEALTH("health"),
    WORKPLACE("workplace"),
    HOUSING("housing"),
}

data class PhraseEntry(
    val id: String,
    val korean: String,
    val bangla: String,
    val scenario: PhraseScenario,
    val isBundled: Boolean,
    val isSaved: Boolean,
    val createdAtEpochMillis: Long,
)

data class TranslationHistoryEntry(
    val id: String,
    val sourceText: String,
    val translatedText: String,
    val direction: String,
    val createdAtEpochMillis: Long,
)
