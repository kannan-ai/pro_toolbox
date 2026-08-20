package com.example.utilityhub.features.support

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SwaraWakeWordAssistant(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var shouldBeListening = false

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            isListening = false
            if (shouldBeListening) {
                startListening() // Restart if it should be listening
            }
        }
        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0].lowercase()
                if (text.contains("hey swara") || text.contains("hello swara") || text.contains("hi swara")) {
                    onWakeWordDetected()
                }
            }
            if (shouldBeListening) {
                startListening() // Restart to keep listening
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0].lowercase()
                if (text.contains("hey swara") || text.contains("hello swara") || text.contains("hi swara")) {
                    onWakeWordDetected()
                }
            }
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun start() {
        shouldBeListening = true
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(recognitionListener)
        }
        startListening()
    }

    private fun startListening() {
        if (isListening) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try {
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        shouldBeListening = false
        speechRecognizer?.stopListening()
        isListening = false
    }

    fun destroy() {
        stop()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
