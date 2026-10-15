package com.kbridge.ai

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.kbridge.ai.data.local.KBridgeDatabase
import com.kbridge.ai.data.repository.PhraseRepository
import com.kbridge.ai.data.repository.RoomPhraseRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KBridgeApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var initializationJob: Job? = null

    private val _phrasebookInitialization = MutableStateFlow(PhrasebookInitializationStatus.LOADING)
    val phrasebookInitialization: StateFlow<PhrasebookInitializationStatus> =
        _phrasebookInitialization.asStateFlow()

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
        initializePhrasebook()
    }

    fun initializePhrasebook() {
        if (initializationJob?.isActive == true) return
        _phrasebookInitialization.value = PhrasebookInitializationStatus.LOADING
        initializationJob =
            applicationScope.launch {
                try {
                    phraseRepository.seedBundledPhrases()
                    _phrasebookInitialization.value = PhrasebookInitializationStatus.READY
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    _phrasebookInitialization.value = PhrasebookInitializationStatus.ERROR
                    Log.e(TAG, "Could not initialize the local phrasebook.", exception)
                }
            }
    }

    private companion object {
        const val DATABASE_NAME = "kbridge-local.db"
        const val TAG = "KBridgeApplication"
    }
}

enum class PhrasebookInitializationStatus {
    LOADING,
    READY,
    ERROR,
}
