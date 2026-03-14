package com.jonicodes.notetaker.data.source

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: Flow<Boolean> = _isListening.asStateFlow()

    private val fullTranscript = StringBuilder()

    private fun createRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    fun startListening(): Flow<SpeechEvent> = callbackFlow {
        fullTranscript.clear()

        val onDeviceAvailable = SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        speechRecognizer = if (onDeviceAvailable) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                trySend(SpeechEvent.Ready)
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                trySend(SpeechEvent.RmsChanged(rmsdB))
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                // Will be restarted if still listening
            }

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        if (_isListening.value) {
                            speechRecognizer?.startListening(createRecognizerIntent())
                        }
                    }
                    SpeechRecognizer.ERROR_CLIENT -> {
                        // Cancelled by user — do nothing
                    }
                    else -> {
                        trySend(SpeechEvent.Error(mapError(error)))
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty()

                if (text.isNotBlank()) {
                    if (fullTranscript.isNotEmpty()) {
                        fullTranscript.append(" ")
                    }
                    fullTranscript.append(text)
                    trySend(SpeechEvent.FinalResult(text, fullTranscript.toString()))
                }

                if (_isListening.value) {
                    speechRecognizer?.startListening(createRecognizerIntent())
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty()
                if (text.isNotBlank()) {
                    trySend(SpeechEvent.PartialResult(text))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer?.setRecognitionListener(listener)
        speechRecognizer?.startListening(createRecognizerIntent())

        awaitClose {
            stopInternal()
        }
    }

    fun stopListening(): String {
        stopInternal()
        return fullTranscript.toString()
    }

    private fun stopInternal() {
        _isListening.value = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
            // SpeechRecognizer may already be destroyed
        }
        speechRecognizer = null
    }

    private fun mapError(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service is busy"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission not granted"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests"
            else -> "Speech recognition error (code: $error)"
        }
    }
}

sealed class SpeechEvent {
    data object Ready : SpeechEvent()
    data class PartialResult(val text: String) : SpeechEvent()
    data class FinalResult(val segment: String, val fullTranscript: String) : SpeechEvent()
    data class RmsChanged(val rmsdB: Float) : SpeechEvent()
    data class Error(val message: String) : SpeechEvent()
}
