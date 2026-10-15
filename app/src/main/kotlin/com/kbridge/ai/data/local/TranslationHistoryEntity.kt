package com.kbridge.ai.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kbridge.ai.core.domain.phrasebook.TranslationHistoryEntry

@Entity(
    tableName = "translation_history",
    indices = [Index(value = ["createdAtEpochMillis"])],
)
data class TranslationHistoryEntity(
    @PrimaryKey val id: String,
    val sourceText: String,
    val translatedText: String,
    val direction: String,
    val createdAtEpochMillis: Long,
)

internal fun TranslationHistoryEntity.toDomain() =
    TranslationHistoryEntry(
        id = id,
        sourceText = sourceText,
        translatedText = translatedText,
        direction = direction,
        createdAtEpochMillis = createdAtEpochMillis,
    )
