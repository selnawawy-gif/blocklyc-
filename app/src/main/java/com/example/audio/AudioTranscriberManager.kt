package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TranscriptItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val text: String,
    val isFinal: Boolean,
    val detectedAction: String? = null
)

data class AudioTranscriptionState(
    val isListening: Boolean = false,
    val isAvailable: Boolean = true,
    val currentRmsDb: Float = 0f,
    val partialText: String = "",
    val transcripts: List<TranscriptItem> = emptyList(),
    val statusMessage: String = "Ready to transcribe audio feed",
    val detectedBlockSuggestion: String? = null
)

class AudioTranscriberManager(
    private val context: Context,
    private val onVoiceActionDetected: ((String) -> Unit)? = null
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val _state = MutableStateFlow(AudioTranscriptionState())
    val state: StateFlow<AudioTranscriptionState> = _state.asStateFlow()

    init {
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        _state.update { it.copy(isAvailable = available) }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.update {
                it.copy(
                    isAvailable = false,
                    statusMessage = "Speech recognizer service not available on device"
                )
            }
            return
        }

        try {
            stopListening()

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
            _state.update {
                it.copy(
                    isListening = true,
                    statusMessage = "Listening to audio feed in real-time..."
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isListening = false,
                    statusMessage = "Failed to start audio feed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _state.update {
            it.copy(
                isListening = false,
                currentRmsDb = 0f,
                statusMessage = "Audio transcription stopped"
            )
        }
    }

    fun clearTranscripts() {
        _state.update {
            it.copy(
                transcripts = emptyList(),
                partialText = "",
                detectedBlockSuggestion = null
            )
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.update { it.copy(statusMessage = "Microphone live, speak or play audio...") }
            }

            override fun onBeginningOfSpeech() {
                _state.update { it.copy(statusMessage = "Transcribing incoming audio...") }
            }

            override fun onRmsChanged(rmsdB: Float) {
                _state.update { it.copy(currentRmsDb = rmsdB.coerceAtLeast(0f)) }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _state.update { it.copy(statusMessage = "Processing speech...") }
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Transcription code $error"
                }
                _state.update {
                    it.copy(
                        isListening = false,
                        currentRmsDb = 0f,
                        statusMessage = "Audio feed pause: $errorMsg"
                    )
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val finalPhrase = matches?.firstOrNull() ?: return
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                val detectedAction = detectArduinoIntent(finalPhrase)

                val item = TranscriptItem(
                    timestamp = time,
                    text = finalPhrase,
                    isFinal = true,
                    detectedAction = detectedAction
                )

                _state.update {
                    it.copy(
                        transcripts = it.transcripts + item,
                        partialText = "",
                        statusMessage = "Transcribed: \"$finalPhrase\"",
                        detectedBlockSuggestion = detectedAction
                    )
                }

                if (detectedAction != null) {
                    onVoiceActionDetected?.invoke(detectedAction)
                }

                // If continuous listening is desired, restart recognizer
                if (_state.value.isListening) {
                    startListening()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                _state.update { it.copy(partialText = partial) }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun detectArduinoIntent(text: String): String? {
        val lower = text.lowercase()
        return when {
            lower.contains("turn on") || lower.contains("high") || lower.contains("led on") -> "Add Block: Digital Write Pin 13 HIGH"
            lower.contains("turn off") || lower.contains("low") || lower.contains("led off") -> "Add Block: Digital Write Pin 13 LOW"
            lower.contains("delay") || lower.contains("wait") -> "Add Block: Delay 1000ms"
            lower.contains("servo") || lower.contains("angle") -> "Add Block: Servo Write 90°"
            lower.contains("analog") || lower.contains("potentiometer") -> "Add Block: Analog Read Pin A0"
            lower.contains("buzzer") || lower.contains("tone") || lower.contains("beep") -> "Add Block: Tone Buzzer 440Hz"
            lower.contains("serial") || lower.contains("print") -> "Add Block: Serial Printline"
            lower.contains("ultrasonic") || lower.contains("distance") -> "Add Block: HC-SR04 Sonar"
            else -> null
        }
    }
}
