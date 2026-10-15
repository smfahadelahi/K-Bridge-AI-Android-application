package com.kbridge.ai.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kbridge.ai.core.domain.phrasebook.PhraseEntry
import com.kbridge.ai.core.domain.phrasebook.PhraseScenario

@Entity(
    tableName = "phrases",
    indices = [Index(value = ["isSaved", "updatedAtEpochMillis"])],
)
data class PhraseEntity(
    @PrimaryKey val id: String,
    val korean: String,
    val bangla: String,
    val scenarioId: String,
    val isBundled: Boolean,
    val isSaved: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

internal fun PhraseEntity.toDomain(): PhraseEntry =
    PhraseEntry(
        id = id,
        korean = korean,
        bangla = bangla,
        scenario = PhraseScenario.entries.firstOrNull { it.id == scenarioId } ?: PhraseScenario.EVERYDAY,
        isBundled = isBundled,
        isSaved = isSaved,
        createdAtEpochMillis = createdAtEpochMillis,
    )

internal fun PhraseEntry.toEntity(): PhraseEntity =
    PhraseEntity(
        id = id,
        korean = korean,
        bangla = bangla,
        scenarioId = scenario.id,
        isBundled = isBundled,
        isSaved = isSaved,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = createdAtEpochMillis,
    )
