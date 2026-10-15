package com.kbridge.ai.platform.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.kbridge.ai.core.domain.conversation.ConversationLanguage
import com.kbridge.ai.core.domain.conversation.TranslationDirection
import kotlinx.coroutines.tasks.await

/** On-device ML Kit adapter. Model downloads are only started by an explicit user action. */
class MlKitTranslationEngine {
    private val modelManager: RemoteModelManager = RemoteModelManager.getInstance()

    suspend fun isModelReady(direction: TranslationDirection): Boolean {
        val downloadedLanguages =
            modelManager
                .getDownloadedModels(TranslateRemoteModel::class.java)
                .await()
                .map { model -> model.language }
                .toSet()
        return direction.languages().all(downloadedLanguages::contains)
    }

    suspend fun downloadModel(direction: TranslationDirection) {
        val translator = createTranslator(direction)
        try {
            translator
                .downloadModelIfNeeded(
                    DownloadConditions
                        .Builder()
                        .requireWifi()
                        .build(),
                ).await()
        } finally {
            translator.close()
        }
    }

    suspend fun translate(
        text: String,
        direction: TranslationDirection,
    ): String {
        require(text.isNotBlank()) { "Translation input must not be blank" }
        val translator = createTranslator(direction)
        return try {
            translator.translate(text).await()
        } finally {
            translator.close()
        }
    }

    private fun createTranslator(direction: TranslationDirection): Translator {
        val options =
            TranslatorOptions
                .Builder()
                .setSourceLanguage(direction.sourceLanguage.mlKitCode())
                .setTargetLanguage(direction.targetLanguage.mlKitCode())
                .build()
        return Translation.getClient(options)
    }

    private fun TranslationDirection.languages(): Set<String> =
        setOf(sourceLanguage.mlKitCode(), targetLanguage.mlKitCode())

    private fun ConversationLanguage.mlKitCode(): String =
        when (this) {
            ConversationLanguage.KOREAN -> TranslateLanguage.KOREAN
            ConversationLanguage.BANGLA -> TranslateLanguage.BENGALI
        }
}
