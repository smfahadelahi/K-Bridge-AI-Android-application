package com.kbridge.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PhraseDao {
    @Query("SELECT * FROM phrases WHERE isBundled = 1 OR isSaved = 1 ORDER BY scenarioId, korean")
    fun observePhrasebook(): Flow<List<PhraseEntity>>

    @Query("SELECT * FROM phrases WHERE isSaved = 1 ORDER BY updatedAtEpochMillis DESC, korean")
    fun observeSavedPhrases(): Flow<List<PhraseEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBundledPhrases(phrases: List<PhraseEntity>)

    @Upsert
    suspend fun upsertPhrase(phrase: PhraseEntity)

    @Query("UPDATE phrases SET isSaved = :saved, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun setSaved(
        id: String,
        saved: Boolean,
        updatedAt: Long,
    )

    @Query("DELETE FROM phrases WHERE id = :id AND isBundled = 0")
    suspend fun deleteUserPhrase(id: String)
}
