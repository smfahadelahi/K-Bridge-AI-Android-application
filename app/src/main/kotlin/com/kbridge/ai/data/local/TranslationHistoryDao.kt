package com.kbridge.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationHistoryDao {
    @Query("SELECT * FROM translation_history ORDER BY createdAtEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TranslationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TranslationHistoryEntity)

    @Query(
        "DELETE FROM translation_history " +
            "WHERE id NOT IN (SELECT id FROM translation_history " +
            "ORDER BY createdAtEpochMillis DESC LIMIT 100)",
    )
    suspend fun deleteEntriesBeyondLimit()

    @Query("DELETE FROM translation_history")
    suspend fun deleteAll()
}
