package com.kbridge.ai.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kbridge.ai.KBridgeApplication
import com.kbridge.ai.core.domain.conversation.TranslationDirection
import com.kbridge.ai.core.domain.phrasebook.PhraseEntry
import com.kbridge.ai.core.domain.phrasebook.PhraseScenario
import com.kbridge.ai.core.domain.phrasebook.TranslationHistoryEntry
import com.kbridge.ai.data.repository.PhraseRepository
import com.kbridge.ai.platform.translation.MlKitTranslationEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class KBridgeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PhraseRepository = (application as KBridgeApplication).phraseRepository
    private val translationEngine = MlKitTranslationEngine()
    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _translationState = MutableStateFlow(TranslationUiState())
    private var translationJob: Job? = null
    private var translationRequestId = 0L
    val translationState: StateFlow<TranslationUiState> = _translationState.asStateFlow()

    val phrasebook: StateFlow<List<PhraseEntry>> =
        repository
            .observePhrasebook()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedPhrases: StateFlow<List<PhraseEntry>> =
        repository
            .observeSavedPhrases()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentHistory: StateFlow<List<TranslationHistoryEntry>> =
        repository
            .observeRecentHistory(MAX_HISTORY_ITEMS)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _rememberHistory = MutableStateFlow(preferences.getBoolean(KEY_REMEMBER_HISTORY, false))
    val rememberHistory: StateFlow<Boolean> = _rememberHistory.asStateFlow()

    fun updateDraft(value: String) {
        translationRequestId += 1
        translationJob?.cancel()
        _translationState.update {
            it.copy(
                draft = value,
                result = null,
                koreanResult = null,
                banglaResult = null,
                resultSaved = false,
                isTranslating = false,
                error = null,
            )
        }
    }

    fun clearTranslationResult() {
        translationRequestId += 1
        translationJob?.cancel()
        _translationState.update {
            it.copy(
                result = null,
                koreanResult = null,
                banglaResult = null,
                resultSaved = false,
                isTranslating = false,
                error = null,
            )
        }
    }

    fun selectPhraseScenario(scenario: PhraseScenario) {
        _translationState.update { it.copy(selectedScenario = scenario) }
    }

    fun refreshTranslationModel(direction: TranslationDirection) {
        if (_translationState.value.modelStatus == TranslationModelStatus.DOWNLOADING) return
        _translationState.update {
            it.copy(modelStatus = TranslationModelStatus.CHECKING, error = null)
        }
        viewModelScope.launch {
            try {
                val ready = translationEngine.isModelReady(direction)
                _translationState.update {
                    it.copy(
                        modelStatus =
                            if (ready) {
                                TranslationModelStatus.READY
                            } else {
                                TranslationModelStatus.NEEDS_DOWNLOAD
                            },
                        error = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _translationState.update {
                    it.copy(
                        modelStatus = TranslationModelStatus.ERROR,
                        error = TranslationError.MODEL_CHECK,
                    )
                }
            }
        }
    }

    fun downloadTranslationModel(direction: TranslationDirection) {
        if (_translationState.value.modelStatus == TranslationModelStatus.DOWNLOADING) return
        _translationState.update {
            it.copy(modelStatus = TranslationModelStatus.DOWNLOADING, error = null)
        }
        viewModelScope.launch {
            try {
                translationEngine.downloadModel(direction)
                val ready = translationEngine.isModelReady(direction)
                _translationState.update {
                    it.copy(
                        modelStatus =
                            if (ready) {
                                TranslationModelStatus.READY
                            } else {
                                TranslationModelStatus.ERROR
                            },
                        error = if (ready) null else TranslationError.DOWNLOAD,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _translationState.update {
                    it.copy(
                        modelStatus = TranslationModelStatus.NEEDS_DOWNLOAD,
                        error = TranslationError.DOWNLOAD,
                    )
                }
            }
        }
    }

    fun translate(direction: TranslationDirection) {
        val input = _translationState.value.draft.trim()
        if (input.isBlank() || _translationState.value.isTranslating) return
        translationJob?.cancel()
        val requestId = ++translationRequestId
        _translationState.update {
            it.copy(
                isTranslating = true,
                result = null,
                koreanResult = null,
                banglaResult = null,
                resultSaved = false,
                error = null,
            )
        }
        translationJob =
            viewModelScope.launch {
                try {
                    if (!translationEngine.isModelReady(direction)) {
                        updateTranslationState(requestId) {
                            it.copy(
                                isTranslating = false,
                                modelStatus = TranslationModelStatus.NEEDS_DOWNLOAD,
                            )
                        }
                        return@launch
                    }
                    val translated = translationEngine.translate(input, direction)
                    val korean =
                        if (direction == TranslationDirection.KOREAN_TO_BANGLA) {
                            input
                        } else {
                            translated
                        }
                    val bangla =
                        if (direction == TranslationDirection.KOREAN_TO_BANGLA) {
                            translated
                        } else {
                            input
                        }
                    updateTranslationState(requestId) {
                        it.copy(
                            isTranslating = false,
                            modelStatus = TranslationModelStatus.READY,
                            result = translated,
                            koreanResult = korean,
                            banglaResult = bangla,
                            error = null,
                        )
                    }
                    if (_rememberHistory.value && requestId == translationRequestId) {
                        repository.recordTranslation(
                            TranslationHistoryEntry(
                                id = UUID.randomUUID().toString(),
                                sourceText = input,
                                translatedText = translated,
                                direction = direction.name,
                                createdAtEpochMillis = System.currentTimeMillis(),
                            ),
                        )
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    updateTranslationState(requestId) {
                        it.copy(
                            isTranslating = false,
                            error = TranslationError.TRANSLATION,
                        )
                    }
                }
            }
    }

    private fun updateTranslationState(
        requestId: Long,
        transform: (TranslationUiState) -> TranslationUiState,
    ) {
        if (requestId == translationRequestId) {
            _translationState.update(transform)
        }
    }

    fun saveCurrentTranslation() {
        val state = _translationState.value
        val korean = state.koreanResult ?: return
        val bangla = state.banglaResult ?: return
        if (state.resultSaved) return
        viewModelScope.launch {
            try {
                repository.saveUserPhrase(korean, bangla, state.selectedScenario)
                _translationState.update { it.copy(resultSaved = true) }
            } catch (_: Exception) {
                _translationState.update { it.copy(error = TranslationError.SAVE_PHRASE) }
            }
        }
    }

    fun setPhraseSaved(
        phrase: PhraseEntry,
        saved: Boolean,
    ) {
        viewModelScope.launch {
            repository.setPhraseSaved(phrase, saved)
        }
    }

    fun setRememberHistory(enabled: Boolean) {
        _rememberHistory.value = enabled
        preferences.edit().putBoolean(KEY_REMEMBER_HISTORY, enabled).apply()
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    companion object {
        private const val PREFERENCES_NAME = "kbridge-privacy"
        private const val KEY_REMEMBER_HISTORY = "remember_translation_history"
        private const val MAX_HISTORY_ITEMS = 20
    }
}

data class TranslationUiState(
    val draft: String = "",
    val selectedScenario: PhraseScenario = PhraseScenario.EVERYDAY,
    val result: String? = null,
    val koreanResult: String? = null,
    val banglaResult: String? = null,
    val modelStatus: TranslationModelStatus = TranslationModelStatus.CHECKING,
    val isTranslating: Boolean = false,
    val resultSaved: Boolean = false,
    val error: TranslationError? = null,
)

enum class TranslationModelStatus {
    CHECKING,
    NEEDS_DOWNLOAD,
    DOWNLOADING,
    READY,
    ERROR,
}

enum class TranslationError {
    MODEL_CHECK,
    DOWNLOAD,
    TRANSLATION,
    SAVE_PHRASE,
}
