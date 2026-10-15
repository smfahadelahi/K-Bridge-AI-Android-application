package com.kbridge.ai

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.kbridge.ai.data.local.KBridgeDatabase
import com.kbridge.ai.data.repository.PhraseRepository
import com.kbridge.ai.data.repository.RoomPhraseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KBridgeApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: KBridgeDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            KBridgeDatabase::class.java,
            DATABASE_NAME,
        ).build()
    }

    val phraseRepository: PhraseRepository by lazy {
        RoomPhraseRepository(
            phraseDao = database.phraseDao(),
            historyDao = database.translationHistoryDao(),
        )
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                phraseRepository.seedBundledPhrases()
            } catch (exception: Exception) {
                Log.e(TAG, "Could not initialize the local phrasebook.", exception)
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "kbridge-local.db"
        const val TAG = "KBridgeApplication"
    }
}
