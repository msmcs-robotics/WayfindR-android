package com.example.wayfindr

import android.content.Context
import android.widget.Toast
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorHandler {
    
    fun handleNetworkError(context: Context, exception: Exception): String {
        val errorMessage = when (exception) {
            is ConnectException -> "Cannot connect to server. Please check if the server is running and the network connection is available."
            is SocketTimeoutException -> "Request timed out. The server may be busy or unreachable."
            is UnknownHostException -> "Cannot resolve server address. Please check your network connection."
            else -> "Network error: ${exception.message ?: "Unknown error occurred"}"
        }
        
        // Show toast for immediate user feedback
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        
        return errorMessage
    }
    
    fun handleSpeechError(errorCode: Int): String {
        return when (errorCode) {
            android.speech.SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your microphone."
            android.speech.SpeechRecognizer.ERROR_CLIENT -> "Client side error occurred."
            android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
            android.speech.SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
            android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out."
            android.speech.SpeechRecognizer.ERROR_NO_MATCH -> "No speech was recognized. Please try again."
            android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please wait."
            android.speech.SpeechRecognizer.ERROR_SERVER -> "Server error during speech recognition."
            android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected."
            else -> "Unknown speech recognition error"
        }
    }
}