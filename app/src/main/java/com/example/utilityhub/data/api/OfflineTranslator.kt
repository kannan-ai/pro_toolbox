package com.example.utilityhub.data.api

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

class OfflineTranslator {
    private val modelManager = RemoteModelManager.getInstance()
    private var currentTranslator: Translator? = null

    suspend fun isModelDownloaded(langCode: String): Boolean {
        val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(langCode).build()
        return modelManager.isModelDownloaded(model).await()
    }

    suspend fun downloadModel(langCode: String): Boolean {
        // Broad list of languages supported by ML Kit for offline translation
        val supported = setOf(
            "af", "sq", "ar", "be", "bn", "bg", "ca", "zh", "hr", "cs", "da", "nl", "en", "eo", "et", 
            "fi", "fr", "gl", "ka", "de", "el", "gu", "ht", "he", "hi", "hu", "is", "id", "ga", "it", 
            "ja", "kn", "ko", "lv", "lt", "mk", "ms", "mt", "mr", "no", "fa", "pl", "pt", "pa", "ro", 
            "ru", "sk", "sl", "es", "sw", "sv", "ta", "te", "th", "tr", "uk", "ur", "vi", "cy", "ml"
        )
        if (!supported.contains(langCode)) {
            android.util.Log.w("OfflineTranslator", "Language $langCode is not supported for offline translation.")
            return false
        }

        val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(langCode).build()
        val conditions = DownloadConditions.Builder()
            .build() // Remove requireWifi() to allow mobile data downloads
        return try {
            modelManager.download(model, conditions).await()
            true
        } catch (e: Exception) {
            println("OfflineTranslator: Download failed for $langCode: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun translateOffline(text: String, fromLang: String, toLang: String): String {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(fromLang)
            .setTargetLanguage(toLang)
            .build()
        
        val translator = Translation.getClient(options)
        currentTranslator = translator
        
        return try {
            // Ensure models are downloaded before trying to translate
            // (Caller should check isModelDownloaded, but this is a safety net)
            translator.downloadModelIfNeeded().await()
            translator.translate(text).await()
        } catch (e: Exception) {
            "Error: ${e.message}"
        } finally {
            translator.close()
        }
    }
}
