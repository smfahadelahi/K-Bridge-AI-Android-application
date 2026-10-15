package com.kbridge.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PhraseEntity::class, TranslationHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class KBridgeDatabase : RoomDatabase() {
    abstract fun phraseDao(): PhraseDao

    abstract fun translationHistoryDao(): TranslationHistoryDao
}
