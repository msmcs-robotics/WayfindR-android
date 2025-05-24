package com.example.wayfindr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

class SpeechManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsInitialized = false
    
    init {
        initializeTextToSpeech()
        initializeSpeechRecognizer()
    }
    
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context, this)
    }
    
    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
        } else {
            onError("Speech recognition not available on this device")
        }
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val langResult = tts.setLanguage(Locale.US)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || 
                    langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    onError("Text-to-speech language not supported")
                } else {
                    isTtsInitialized = true
                    setupTtsListener()
                }
            }
        } else {
            onError("Text-to-speech initialization failed")
        }
    }
    
    private fun setupTtsListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // TTS started speaking
            }
            
            override fun onDone(utteranceId: String?) {
                // TTS finished speaking
            }
            
            override fun onError(utteranceId: String?) {
                onError("Text-to-speech error occurred")
            }
        })
    }
    
    fun speakText(text: String) {
        if (!isTtsInitialized) {
            onError("Text-to-speech not ready")
            return
        }
        
        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "tts_utterance_${System.currentTimeMillis()}"
        )
    }
    
    fun startListening() {
        speechRecognizer?.let { recognizer ->
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            
            recognizer.startListening(intent)
        } ?: run {
            onError("Speech recognizer not available")
        }
    }
    
    fun stopListening() {
        speechRecognizer?.stopListening()
    }
    
    fun stopSpeaking() {
        textToSpeech?.stop()
    }
    
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningStateChanged(true)
            }
            
            override fun onBeginningOfSpeech() {
                // Speech input started
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Volume level changed - could be used for visual feedback
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Partial speech data received
            }
            
            override fun onEndOfSpeech() {
                onListeningStateChanged(false)
            }
            
            override fun onError(error: Int) {
                onListeningStateChanged(false)
                val errorMessage = ErrorHandler.handleSpeechError(error)
                onError(errorMessage)
            }
            
            override fun onResults(results: Bundle?) {
                onListeningStateChanged(false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    onSpeechResult(recognizedText)
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                // Partial results available - could be used for real-time display
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Additional events from the recognition service
            }
        }
    }
    
    fun cleanup() {
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
        speechRecognizer = null
        textToSpeech = null
        isTtsInitialized = false
    }
    
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking == true
    }
}